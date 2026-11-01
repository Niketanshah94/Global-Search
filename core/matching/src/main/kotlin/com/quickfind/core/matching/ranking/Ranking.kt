package com.quickfind.core.matching.ranking

import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType
import kotlin.math.exp

/** Tunable ranking constants (spec 4.7). */
object RankingWeights {
    const val HISTORY_MAX = 100
    /** Each older distinct pick for the same query gets this fraction of the previous one. */
    const val HISTORY_DECAY = 0.5
    /** A pick recorded for a prefix/extension of the query counts at this fraction. */
    const val HISTORY_PREFIX_FACTOR = 0.75
    const val FRECENCY_MAX = 60
    const val FRECENCY_HALF_LIFE_DAYS = 14.0
    const val DISABLED_PENALTY = -200

    val DEFAULT_TYPE_BIAS: Map<SourceType, Int> = mapOf(
        SourceType.APP to 20,
        SourceType.CONTACT to 10,
        SourceType.SETTING to 0,
        SourceType.FILE to -10,
        SourceType.FOLDER to -10,
    )
}

/** One row of `query_history`. */
data class QueryPick(val query: String, val resultId: String, val timestampMillis: Long)

/** One row of `launch_events`. */
data class LaunchEvent(val resultId: String, val timestampMillis: Long)

enum class RankingMode {
    /** Match score plus history, frecency and type bias. */
    SMART,

    /** Match score and type bias only; history is still recorded but ignored. */
    MATCH_ONLY,
}

/**
 * Pre-computed boosts for one query. Built off the main thread from in-memory copies of
 * the history tables, then applied to every candidate in O(1).
 */
class RankingContext(
    private val historyBoosts: Map<String, Int>,
    private val frecencyBoosts: Map<String, Int>,
    private val typeBias: Map<SourceType, Int>,
    private val mode: RankingMode,
) {
    fun finalScore(result: SearchResult): Int {
        var score = result.matchScore + (typeBias[result.type] ?: 0)
        if (mode == RankingMode.SMART) {
            score += historyBoosts[result.id] ?: 0
            score += frecencyBoosts[result.id] ?: 0
        }
        if (result.isDisabled) score += RankingWeights.DISABLED_PENALTY
        return score
    }

    fun historyBoost(id: String): Int = historyBoosts[id] ?: 0
    fun frecencyBoost(id: String): Int = frecencyBoosts[id] ?: 0

    companion object {
        val NEUTRAL = RankingContext(emptyMap(), emptyMap(), RankingWeights.DEFAULT_TYPE_BIAS, RankingMode.SMART)
    }
}

object Ranker {
    private const val DAY_MS = 24.0 * 60 * 60 * 1000

    /**
     * historyBoost: the result most recently picked for this query gets +100; each older,
     * different pick gets half of the previous one. Picks stored for a prefix of the query
     * (or a query this one is a prefix of) count at 75%.
     */
    fun historyBoosts(normalizedQuery: String, picks: List<QueryPick>): Map<String, Int> {
        if (normalizedQuery.isEmpty() || picks.isEmpty()) return emptyMap()
        val boosts = HashMap<String, Int>()
        var rank = 0
        // Newest first.
        for (pick in picks.sortedByDescending { it.timestampMillis }) {
            val exact = pick.query == normalizedQuery
            val related = !exact && pick.query.isNotEmpty() &&
                (normalizedQuery.startsWith(pick.query) || pick.query.startsWith(normalizedQuery))
            if (!exact && !related) continue
            if (pick.resultId in boosts) continue
            var boost = RankingWeights.HISTORY_MAX * Math.pow(RankingWeights.HISTORY_DECAY, rank.toDouble())
            if (!exact) boost *= RankingWeights.HISTORY_PREFIX_FACTOR
            val value = boost.toInt()
            if (value <= 0) break
            boosts[pick.resultId] = value
            rank++
        }
        return boosts
    }

    /** frecency: sum of exp(-ageDays/14) over launches, scaled so the top result gets 60. */
    fun frecencyBoosts(launches: List<LaunchEvent>, nowMillis: Long): Map<String, Int> {
        if (launches.isEmpty()) return emptyMap()
        val sums = HashMap<String, Double>()
        for (e in launches) {
            val ageDays = ((nowMillis - e.timestampMillis).coerceAtLeast(0)) / DAY_MS
            sums[e.resultId] = (sums[e.resultId] ?: 0.0) + exp(-ageDays / RankingWeights.FRECENCY_HALF_LIFE_DAYS)
        }
        val max = sums.values.maxOrNull() ?: return emptyMap()
        if (max <= 0.0) return emptyMap()
        return sums.mapValues { (_, v) -> (RankingWeights.FRECENCY_MAX * v / max).toInt() }
    }

    /** Result ids ordered by frecency, for the empty-query "recent & frequent" row. */
    fun topByFrecency(launches: List<LaunchEvent>, nowMillis: Long, limit: Int): List<String> =
        frecencyBoosts(launches, nowMillis).entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
}
