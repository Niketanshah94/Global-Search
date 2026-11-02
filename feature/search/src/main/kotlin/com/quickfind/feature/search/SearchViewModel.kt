package com.quickfind.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.matching.ranking.AssembledResults
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val engine: SearchEngine,
    private val resultPrefs: ResultPrefsRepository,
    private val history: HistoryRepository,
    private val settingsRepository: SettingsRepository,
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
    private val emptyContent = MutableStateFlow(EmptyStateContent(emptyList(), emptyList(), emptyList()))
    private val clipboard = MutableStateFlow(false)

    val settings: StateFlow<UserSettings> = settingsRepository.settings

    private val engineResults: StateFlow<EngineResult> = engine
        .results(combine(query, scope) { q, s -> SearchRequest(q, s?.types) })
        .stateIn(viewModelScope, SharingStarted.Eagerly, EngineResult("", AssembledResults.EMPTY, isComplete = true))

    private val emptyUi = combine(emptyContent, resultPrefs.pinned, clipboard) { content, pinned, clip ->
        EmptyUi(pinned = pinned, recentApps = content.recentApps, recentFiles = content.recentFiles, clipboardAvailable = clip)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        scope,
        engineResults,
        expanded,
        emptyUi,
    ) { q, s, results, open, empty ->
        val showingCurrent = results.query == q
        val assembled = results.results
        SearchUiState(
            query = q,
            scope = s,
            topHit = assembled.topHit?.result,
            sections = assembled.sections.map { sec ->
                val isOpen = sec.section in open
                val items = sec.items.map { it.result }
                val visible = if (isOpen) items else items.take(SECTION_SIZE)
                SectionUi(sec.section, visible, items.size - visible.size, isOpen)
            },
            isSearching = q.isNotBlank() && (!showingCurrent || !results.isComplete),
            empty = empty,
            pinnedIds = empty.pinned.mapTo(HashSet()) { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    fun onQueryChange(text: String) {
        queryText = text
        query.value = text
        expanded.value = emptySet()
    }

    fun setScope(newScope: SearchScope?) {
        scope.value = newScope
    }

    fun clear() {
        queryText = ""
        query.value = ""
        scope.value = null
        expanded.value = emptySet()
    }

    fun toggleSection(section: ResultSection) {
        expanded.value = if (section in expanded.value) expanded.value - section else expanded.value + section
    }

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

    suspend fun loadSettings(): UserSettings = settingsRepository.load()

    private companion object {
        const val SECTION_SIZE = 4
        const val MAX_CLIPBOARD_CHARS = 500
    }
}
