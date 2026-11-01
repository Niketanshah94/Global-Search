package com.quickfind.core.matching.ranking

import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankerTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000L * day

    private fun result(id: String, type: SourceType, score: Int, disabled: Boolean = false) = SearchResult(
        id = id, type = type, title = id, subtitle = null, matchScore = score, matchedRanges = emptyList(),
        primaryAction = ResultAction.OpenUrl("https://example.com"), isDisabled = disabled,
    )

    @Test
    fun mostRecentPickGetsFullBoostOlderDecay() {
        val picks = listOf(
            QueryPick("wh", "app:whatsapp", now - day),
            QueryPick("wh", "app:weather", now - 2 * day),
            QueryPick("wh", "app:whatsapp", now - 3 * day),
        )
        val boosts = Ranker.historyBoosts("wh", picks)
        assertEquals(100, boosts["app:whatsapp"])
        assertEquals(50, boosts["app:weather"])
    }

    @Test
    fun prefixPicksCountPartially() {
        val boosts = Ranker.historyBoosts("wha", listOf(QueryPick("wh", "app:whatsapp", now)))
        assertEquals(75, boosts["app:whatsapp"])
        assertTrue(Ranker.historyBoosts("cam", listOf(QueryPick("wh", "app:whatsapp", now))).isEmpty())
    }

    @Test
    fun frecencyScalesToSixtyAndDecays() {
        val launches = listOf(
            LaunchEvent("a", now), LaunchEvent("a", now - day),
            LaunchEvent("b", now - 30 * day),
        )
        val f = Ranker.frecencyBoosts(launches, now)
        assertEquals(60, f["a"])
        assertTrue(f["b"]!! < 10)
    }

    @Test
    fun historyCanLiftAResultAboveABetterMatch() {
        val whatsapp = result("app:whatsapp", SourceType.APP, 100)
        val weather = result("app:weather", SourceType.APP, 120)
        val ctx = RankingContext(mapOf("app:whatsapp" to 100), emptyMap(), RankingWeights.DEFAULT_TYPE_BIAS, RankingMode.SMART)
        val assembled = ResultAssembler.assemble(listOf(weather, whatsapp), ctx)
        assertEquals("app:whatsapp", assembled.topHit!!.result.id)

        val matchOnly = RankingContext(mapOf("app:whatsapp" to 100), emptyMap(), RankingWeights.DEFAULT_TYPE_BIAS, RankingMode.MATCH_ONLY)
        assertEquals("app:weather", ResultAssembler.assemble(listOf(weather, whatsapp), matchOnly).topHit!!.result.id)
    }

    @Test
    fun disabledAppsSink() {
        val ctx = RankingContext.NEUTRAL
        val a = result("app:a", SourceType.APP, 100, disabled = true)
        val b = result("app:b", SourceType.APP, 10)
        assertEquals("app:b", ResultAssembler.assemble(listOf(a, b), ctx).topHit!!.result.id)
    }

    @Test
    fun assemblerGroupsDedupesAndHides() {
        val ctx = RankingContext.NEUTRAL
        val results = listOf(
            result("app:cam", SourceType.APP, 100),
            result("app:cam", SourceType.APP, 90),
            result("file:1", SourceType.FILE, 50),
            result("setting:wifi", SourceType.SETTING, 40),
            result("web", SourceType.WEB, 0),
            result("hidden", SourceType.APP, 500),
        )
        val out = ResultAssembler.assemble(results, ctx, hiddenIds = setOf("hidden"))
        assertEquals("app:cam", out.topHit!!.result.id)
        assertEquals(listOf(ResultSection.FILES, ResultSection.SETTINGS, ResultSection.WEB), out.sections.map { it.section })
        assertTrue(out.sections.none { s -> s.items.any { it.result.id == "app:cam" } })
    }

    @Test
    fun calculatorAlwaysTopHitAndWebNeverUnlessAlone() {
        val ctx = RankingContext.NEUTRAL
        val calc = result("calc", SourceType.CALCULATOR, 0)
        val app = result("app:x", SourceType.APP, 300)
        val web = result("web", SourceType.WEB, 1000)
        assertEquals("calc", ResultAssembler.assemble(listOf(app, calc, web), ctx).topHit!!.result.id)
        assertEquals("app:x", ResultAssembler.assemble(listOf(app, web), ctx).topHit!!.result.id)
        assertEquals("web", ResultAssembler.assemble(listOf(web), ctx).topHit!!.result.id)
    }
}
