package com.quickfind.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.billing.EntitlementRepository
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.matching.instant.QueryPrefixes
import com.quickfind.core.matching.ranking.AssembledResults
import com.quickfind.core.model.ProGate
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.sources.engine.EmptyStateContent
import com.quickfind.core.sources.engine.EngineResult
import com.quickfind.core.sources.engine.SearchEngine
import com.quickfind.core.sources.engine.SearchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val engine: SearchEngine,
    private val resultPrefs: ResultPrefsRepository,
    private val history: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val entitlements: EntitlementRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /**
     * The text field's contents, as Compose snapshot state. The field must read this, not
     * [uiState]: a StateFlow reaches the UI a frame later, and the keyboard then sees an old
     * value and puts the cursor in the wrong place (and every keystroke waits a frame).
     */
    var queryText by mutableStateOf("")
        private set
    private val scope = MutableStateFlow<SearchScope?>(null)
    private val expanded = MutableStateFlow<Set<ResultSection>>(emptySet())
    private val filter = MutableStateFlow<ResultSection?>(null)
    private val view = combine(expanded, filter) { e, f -> e to f }
    private val emptyContent = MutableStateFlow(EmptyStateContent(emptyList(), emptyList(), emptyList()))
    private val clipboard = MutableStateFlow(false)

    val settings: StateFlow<UserSettings> = settingsRepository.settings

    private val engineResults: StateFlow<EngineResult> = engine
        // Pro status is an input too: after subscribing, the query on screen re-runs unlocked.
        .results(combine(query, scope, entitlements.entitlement.map { it.isPro }.distinctUntilChanged()) { q, s, _ -> SearchRequest(q, s?.types) })
        .stateIn(viewModelScope, SharingStarted.Eagerly, EngineResult("", AssembledResults.EMPTY, isComplete = true))

    /** Empty-screen content plus result id -> nickname, so the sheet updates as soon as one is set. */
    private val emptyUi = combine(
        emptyContent,
        resultPrefs.pinned,
        clipboard,
        resultPrefs.nicknames,
        entitlements.entitlement,
    ) { content, pinned, clip, nicks, entitlement ->
        val pro = entitlement.isPro
        EmptyUi(
            pinned = if (pro) pinned else pinned.filter { ProGate.isFree(it.type) },
            recentApps = content.recentApps,
            recentFiles = content.recentFiles,
            clipboardAvailable = clip,
            otp = content.otp?.let { OtpUi(it.code, it.source) },
            foundToday = content.foundToday,
            otpLocked = content.otpLocked,
        ) to
            (nicks.entries.associate { (nick, r) -> r.id to nick } to pro)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        scope,
        engineResults,
        view,
        emptyUi,
    ) { q, s, results, openAndFilter, emptyAndNicks ->
        val (open, chosen) = openAndFilter
        val (empty, nicksAndPro) = emptyAndNicks
        val (nicknames, pro) = nicksAndPro
        val showingCurrent = results.query == q
        val assembled = results.results
        val top = assembled.topHit?.result
        val counts = ResultSection.entries.mapNotNull { section ->
            val n = (assembled.sections.firstOrNull { it.section == section }?.items?.size ?: 0) +
                (if (top != null && ResultSection.of(top.type) == section) 1 else 0)
            if (n > 0) section to n else null
        }
        // A chip that no longer has results falls back to "All".
        val active = chosen?.takeIf { c -> counts.any { it.first == c } }
        SearchUiState(
            query = q,
            scope = s,
            topHit = if (active == null || (top != null && ResultSection.of(top.type) == active)) top else null,
            sections = assembled.sections
                .filter { active == null || it.section == active }
                .map { sec ->
                    // With a chip selected, show every result of that kind.
                    val isOpen = sec.section in open || active != null
                    val items = sec.items.map { it.result }
                    val visible = if (isOpen) items else items.take(SECTION_SIZE)
                    SectionUi(sec.section, visible, items.size - visible.size, isOpen)
                },
            isSearching = q.isNotBlank() && (!showingCurrent || !results.isComplete),
            empty = empty,
            pinnedIds = empty.pinned.mapTo(HashSet()) { it.id },
            nicknames = nicknames,
            filter = active,
            counts = counts,
            isPro = pro,
            // Only for the query on screen, so an old count never sits under new results.
            locked = results.locked.takeIf { !pro && showingCurrent && it.count > 0 }
                ?.let { LockedUi(it.count, it.sections, it.types) },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun onQueryChange(text: String) {
        // "f " / "c " / "a " / "s " at the start turns into a scope chip, like a filter.
        if (scope.value == null && settingsRepository.settings.value.prefixesEnabled) {
            val parsed = QueryPrefixes.parse(text)
            val prefix = parsed?.prefix
            if (parsed != null && prefix is QueryPrefixes.Prefix.Scope) {
                SearchScope.entries.firstOrNull { it.types == prefix.types }?.let { matched ->
                    scope.value = matched
                    queryText = parsed.rest
                    query.value = parsed.rest
                    expanded.value = emptySet()
                    return
                }
            }
        }
        queryText = text
        query.value = text
        expanded.value = emptySet()
        filter.value = null
    }

    fun setScope(newScope: SearchScope?) {
        scope.value = newScope
    }

    fun clear() {
        queryText = ""
        query.value = ""
        filter.value = null
        scope.value = null
        expanded.value = emptySet()
    }

    fun setFilter(section: ResultSection?) {
        filter.value = section
    }

    fun toggleSection(section: ResultSection) {
        expanded.value = if (section in expanded.value) expanded.value - section else expanded.value + section
    }

    val isPro: Boolean get() = entitlements.isPro

    /** Called when the screen becomes visible: refreshes recent apps/files. */
    fun refreshEmptyState() {
        viewModelScope.launch { emptyContent.value = engine.emptyState() }
    }

    fun onClipboardAvailable(available: Boolean) {
        clipboard.value = available
    }

    /** Pasted clipboard text becomes the query. */
    fun onPaste(text: String) {
        clipboard.value = false
        onQueryChange(text.trim().take(MAX_CLIPBOARD_CHARS))
    }

    fun dismissClipboard() {
        clipboard.value = false
    }

    /** The result the keyboard's Enter key opens. */
    fun topHit(): SearchResult? = uiState.value.topHit

    fun onLaunched(result: SearchResult) {
        val q = query.value
        viewModelScope.launch { engine.recordLaunch(q, result) }
    }

    fun hide(result: SearchResult) {
        viewModelScope.launch {
            resultPrefs.hide(result)
            history.forget(result.id)
            refreshEmptyState()
        }
    }

    fun togglePin(result: SearchResult) {
        viewModelScope.launch {
            if (result.id in uiState.value.pinnedIds) resultPrefs.unpin(result.id) else resultPrefs.pin(result)
        }
    }

    fun setNickname(result: SearchResult, nickname: String) {
        viewModelScope.launch {
            // Remove the old one first so a result never answers to two nicknames.
            resultPrefs.removeNickname(result.id)
            if (nickname.isNotBlank()) resultPrefs.setNickname(result, nickname)
        }
    }

    suspend fun loadSettings(): UserSettings = settingsRepository.load()

    private companion object {
        const val SECTION_SIZE = 4
        const val MAX_CLIPBOARD_CHARS = 500
    }
}
