package com.quickfind.feature.search

import app.cash.turbine.test
import com.quickfind.core.data.db.HiddenResultEntity
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.matching.ranking.QueryPick
import com.quickfind.core.matching.ranking.Ranker
import com.quickfind.core.matching.ranking.RankingContext
import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import com.quickfind.core.sources.engine.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class Apps(private val titles: List<String>) : SearchSource {
        override val type = SourceType.APP
        override val kind = SourceKind.IN_MEMORY
        override val isEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun search(query: NormalizedQuery, limit: Int) = titles.mapNotNull { t ->
            val m = FuzzyMatcher.match(query, TextNormalizer.normalize(t)) ?: return@mapNotNull null
            SearchResult("app:$t", type, t, null, m.score, m.ranges, ResultAction.CopyText(t))
        }
    }

    private class History : HistoryRepository {
        val picks = mutableListOf<QueryPick>()
        override suspend fun rankingContext(normalizedQuery: String, mode: RankingMode, typeBias: Map<SourceType, Int>) =
            RankingContext(Ranker.historyBoosts(normalizedQuery, picks), emptyMap(), typeBias, mode)
        override suspend fun recordLaunch(normalizedQuery: String, resultId: String) { picks += QueryPick(normalizedQuery, resultId, System.nanoTime()) }
        override suspend fun topLaunchedIds(limit: Int) = picks.map { it.resultId }.distinct()
        override suspend fun forget(resultId: String) { picks.removeAll { it.resultId == resultId } }
        override suspend fun clear() = picks.clear()
    }

    private class Prefs : ResultPrefsRepository {
        override val hiddenIds = MutableStateFlow(emptySet<String>())
        override val hidden = emptyFlow<List<HiddenResultEntity>>()
        override val pinned = MutableStateFlow(emptyList<SearchResult>())
        override suspend fun hide(result: SearchResult) { hiddenIds.value += result.id }
        override suspend fun unhide(resultId: String) { hiddenIds.value -= resultId }
        override suspend fun pin(result: SearchResult) { pinned.value += result }
        override suspend fun unpin(resultId: String) { pinned.value = pinned.value.filter { it.id != resultId } }
        override val nicknames = MutableStateFlow(emptyMap<String, SearchResult>())
        override suspend fun setNickname(result: SearchResult, nickname: String) { nicknames.value += (nickname to result) }
        override suspend fun removeNickname(resultId: String) { nicknames.value = nicknames.value.filterValues { it.id != resultId } }
    }

    private class Settings : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings())
        override val loaded = settings
        override suspend fun load() = settings.value
        override suspend fun update(transform: (UserSettings) -> UserSettings) { settings.value = transform(settings.value) }
    }

    private val apps = Apps(listOf("WhatsApp", "Weather", "Wallet", "Wikipedia", "Wear OS", "Waze", "Camera"))
    private val history = History()
    private val prefs = Prefs()

    private fun vm() = SearchViewModel(
        SearchEngine(setOf(apps), history, prefs, Settings(), dispatcher, dispatcher),
        prefs,
        history,
        Settings(),
    )

    @Test
    fun typingWhShowsWhatsAppAsTopHit() = runTest(dispatcher) {
        val vm = vm()
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("wh")
            var state = awaitItem()
            while (state.topHit == null) state = awaitItem()
            assertEquals("WhatsApp", state.topHit!!.title)
            assertEquals("WhatsApp", vm.topHit()!!.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sectionsShowFourThenExpand() = runTest(dispatcher) {
        val vm = vm()
        vm.onQueryChange("w")
        val apps = vm.uiState.value.sections.first { it.section == ResultSection.APPS }
        assertEquals(4, apps.visible.size)
        assertTrue(apps.moreCount > 0)
        vm.toggleSection(ResultSection.APPS)
        val expanded = vm.uiState.value.sections.first { it.section == ResultSection.APPS }
        assertEquals(0, expanded.moreCount)
        assertTrue(expanded.expanded)
    }

    @Test
    fun hidingRemovesResult() = runTest(dispatcher) {
        val vm = vm()
        vm.onQueryChange("wh")
        val whatsapp = vm.uiState.value.topHit!!
        vm.hide(whatsapp)
        vm.onQueryChange("wha")
        assertTrue(vm.uiState.value.topHit?.id != whatsapp.id)
    }

    @Test
    fun launchIsLearned() = runTest(dispatcher) {
        val vm = vm()
        vm.onQueryChange("w")
        val wallet = vm.uiState.value.sections.flatMap { it.visible }.plus(listOfNotNull(vm.uiState.value.topHit)).first { it.title == "Wallet" }
        vm.onLaunched(wallet)
        vm.onQueryChange("")
        vm.onQueryChange("w")
        assertEquals("Wallet", vm.uiState.value.topHit!!.title)
    }

    @Test
    fun clearResetsQueryAndScope() = runTest(dispatcher) {
        val vm = vm()
        vm.setScope(SearchScope.FILES)
        vm.onQueryChange("abc")
        vm.clear()
        assertEquals("", vm.uiState.value.query)
        assertEquals(null, vm.uiState.value.scope)
    }

    @Test
    fun scopePrefixBecomesChip() = runTest(dispatcher) {
        val vm = vm()
        vm.onQueryChange("a ")
        assertEquals(SearchScope.APPS, vm.uiState.value.scope)
        assertEquals("", vm.queryText)
        vm.onQueryChange("wh")
        assertEquals("WhatsApp", vm.uiState.value.topHit!!.title)
    }

    @Test
    fun nicknameIsExposed() = runTest(dispatcher) {
        val vm = vm()
        vm.onQueryChange("wh")
        val whatsapp = vm.uiState.value.topHit!!
        vm.setNickname(whatsapp, "chat")
        assertEquals("chat", vm.uiState.value.nicknames[whatsapp.id])
    }
}
