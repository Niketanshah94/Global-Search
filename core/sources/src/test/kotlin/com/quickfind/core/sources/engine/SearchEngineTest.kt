package com.quickfind.core.sources.engine

import app.cash.turbine.test
import com.quickfind.core.data.db.HiddenResultEntity
import com.quickfind.core.data.history.HistoryRepository
import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.matching.FuzzyMatcher
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.matching.TextNormalizer
import com.quickfind.core.matching.ranking.Ranker
import com.quickfind.core.matching.ranking.RankingContext
import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.matching.ranking.QueryPick
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.SearchSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchEngineTest {

    private class FakeSource(
        override val type: SourceType,
        override val kind: SourceKind,
        private val titles: List<String>,
        private val failing: Boolean = false,
    ) : SearchSource {
        override val isEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
            if (failing) error("boom")
            return titles.mapNotNull { t ->
                val m = FuzzyMatcher.match(query, TextNormalizer.normalize(t)) ?: return@mapNotNull null
                SearchResult("${type.name}:$t", type, t, null, m.score, m.ranges, ResultAction.CopyText(t))
            }
        }
    }

    private class FakeHistory : HistoryRepository {
        val picks = mutableListOf<QueryPick>()
        override suspend fun rankingContext(normalizedQuery: String, mode: RankingMode, typeBias: Map<SourceType, Int>) =
            RankingContext(Ranker.historyBoosts(normalizedQuery, picks), emptyMap(), typeBias, mode)
        override suspend fun recordLaunch(normalizedQuery: String, resultId: String) {
            picks += QueryPick(normalizedQuery, resultId, System.nanoTime())
        }
        override suspend fun topLaunchedIds(limit: Int) = picks.map { it.resultId }.distinct().take(limit)
        override suspend fun forget(resultId: String) { picks.removeAll { it.resultId == resultId } }
        override suspend fun clear() = picks.clear()
    }

    private class FakePrefs(hidden: Set<String> = emptySet()) : ResultPrefsRepository {
        override val hiddenIds = MutableStateFlow(hidden)
        override val hidden = emptyFlow<List<HiddenResultEntity>>()
        override val pinned = flowOf(emptyList<SearchResult>())
        override suspend fun hide(result: SearchResult) { hiddenIds.value += result.id }
        override suspend fun unhide(resultId: String) { hiddenIds.value -= resultId }
        override suspend fun pin(result: SearchResult) = Unit
        override suspend fun unpin(resultId: String) = Unit
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings())
        override val loaded = settings
        override suspend fun load() = settings.value
        override suspend fun update(transform: (UserSettings) -> UserSettings) { settings.value = transform(settings.value) }
    }

    private val apps = FakeSource(SourceType.APP, SourceKind.IN_MEMORY, listOf("WhatsApp", "Weather", "Camera", "Google Maps"))
    private val files = FakeSource(SourceType.FILE, SourceKind.INDEXED, listOf("whale.jpg", "camera_manual.pdf"))

    private fun engine(
        sources: Set<SearchSource>,
        history: FakeHistory = FakeHistory(),
        prefs: FakePrefs = FakePrefs(),
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ) = SearchEngine(sources, history, prefs, FakeSettings(), dispatcher, dispatcher)

    @Test
    fun whTopHitIsWhatsApp() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        engine(setOf(apps), dispatcher = d).results(flowOf(SearchRequest("wh"))).test {
            val r = awaitItem()
            assertEquals("WhatsApp", r.results.topHit!!.result.title)
            assertTrue(r.isComplete)
            awaitComplete()
        }
    }

    @Test
    fun inMemoryFirstThenIndexedMergesIn() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        engine(setOf(apps, files), dispatcher = d).results(flowOf(SearchRequest("cam"))).test {
            val first = awaitItem()
            assertFalse(first.isComplete)
            assertTrue(first.results.sections.none { s -> s.items.any { it.result.type == SourceType.FILE } })
            val second = awaitItem()
            assertTrue(second.isComplete)
            assertTrue(second.results.sections.any { s -> s.items.any { it.result.title == "camera_manual.pdf" } })
            awaitComplete()
        }
    }

    @Test
    fun failingSourceDoesNotBreakSearch() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        val broken = FakeSource(SourceType.CONTACT, SourceKind.IN_MEMORY, emptyList(), failing = true)
        engine(setOf(apps, broken), dispatcher = d).results(flowOf(SearchRequest("cam"))).test {
            assertEquals("Camera", awaitItem().results.topHit!!.result.title)
            awaitComplete()
        }
    }

    @Test
    fun learnsFromPicks() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        val history = FakeHistory()
        val e = engine(setOf(apps), history = history, dispatcher = d)
        val weather = SearchResult("APP:Weather", SourceType.APP, "Weather", null, 0, emptyList(), ResultAction.CopyText("w"))
        e.recordLaunch("w", weather)
        e.results(flowOf(SearchRequest("w"))).test {
            assertEquals("Weather", awaitItem().results.topHit!!.result.title)
            awaitComplete()
        }
    }

    @Test
    fun hiddenResultsAreFiltered() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        engine(setOf(apps), prefs = FakePrefs(setOf("APP:WhatsApp")), dispatcher = d).results(flowOf(SearchRequest("wh"))).test {
            assertEquals("Weather", awaitItem().results.topHit!!.result.title)
            awaitComplete()
        }
    }

    @Test
    fun scopeRestrictsSources() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        engine(setOf(apps, files), dispatcher = d).results(flowOf(SearchRequest("cam", scope = setOf(SourceType.FILE)))).test {
            val first = awaitItem()
            assertTrue(first.results.isEmpty)
            assertEquals("camera_manual.pdf", awaitItem().results.topHit!!.result.title)
            awaitComplete()
        }
    }

    @Test
    fun emptyQueryEmitsEmpty() = runTest {
        val d = StandardTestDispatcher(testScheduler)
        engine(setOf(apps), dispatcher = d).results(flowOf(SearchRequest("  "))).test {
            assertTrue(awaitItem().results.isEmpty)
            awaitComplete()
        }
    }
}
