package com.quickfind.core.data.history

import com.quickfind.core.data.db.HistoryDao
import com.quickfind.core.data.db.LaunchEventEntity
import com.quickfind.core.data.db.QueryHistoryEntity
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.matching.ranking.LaunchEvent
import com.quickfind.core.matching.ranking.QueryPick
import com.quickfind.core.matching.ranking.Ranker
import com.quickfind.core.matching.ranking.RankingContext
import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.model.SourceType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface HistoryRepository {
    /** Boosts for one query. Cheap after the first call: history is held in memory. */
    suspend fun rankingContext(normalizedQuery: String, mode: RankingMode, typeBias: Map<SourceType, Int>): RankingContext

    /** Records that [resultId] was launched after typing [normalizedQuery] (may be empty). */
    suspend fun recordLaunch(normalizedQuery: String, resultId: String)

    /** Most "frecent" result ids, for the empty-query screen. */
    suspend fun topLaunchedIds(limit: Int): List<String>

    /** How many results were opened since [sinceMillis] (the "things found today" stat). */
    suspend fun launchesSince(sinceMillis: Long): Int

    /** Removes one result from history (used when it is hidden). */
    suspend fun forget(resultId: String)

    suspend fun clear()
}

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HistoryRepository {

    private val mutex = Mutex()
    private var loaded = false
    private val picks = ArrayList<QueryPick>()
    private val launches = ArrayList<LaunchEvent>()
    private var frecency: Map<String, Int> = emptyMap()
    private var frecencyComputedAt = 0L

    private suspend fun ensureLoaded() {
        if (loaded) return
        withContext(io) {
            val now = System.currentTimeMillis()
            dao.prunePicks(now - RETENTION_MS)
            dao.pruneLaunches(now - RETENTION_MS)
            picks += dao.recentPicks(MAX_PICKS).map { QueryPick(it.query, it.resultId, it.timestamp) }
            launches += dao.launchesSince(now - RETENTION_MS).map { LaunchEvent(it.resultId, it.timestamp) }
        }
        loaded = true
    }

    private fun frecencyNow(now: Long): Map<String, Int> {
        // Decay changes slowly; recompute at most every 10 minutes or after a new launch.
        if (now - frecencyComputedAt > 10 * 60 * 1000L) {
            frecency = Ranker.frecencyBoosts(launches, now)
            frecencyComputedAt = now
        }
        return frecency
    }

    override suspend fun rankingContext(
        normalizedQuery: String,
        mode: RankingMode,
        typeBias: Map<SourceType, Int>,
    ): RankingContext = mutex.withLock {
        ensureLoaded()
        if (mode == RankingMode.MATCH_ONLY) return@withLock RankingContext(emptyMap(), emptyMap(), typeBias, mode)
        val now = System.currentTimeMillis()
        RankingContext(Ranker.historyBoosts(normalizedQuery, picks), frecencyNow(now), typeBias, mode)
    }

    override suspend fun recordLaunch(normalizedQuery: String, resultId: String) {
        val now = System.currentTimeMillis()
        mutex.withLock {
            ensureLoaded()
            if (normalizedQuery.isNotEmpty()) {
                picks.add(0, QueryPick(normalizedQuery, resultId, now))
                if (picks.size > MAX_PICKS) picks.removeAt(picks.lastIndex)
            }
            launches.add(LaunchEvent(resultId, now))
            frecencyComputedAt = 0
        }
        withContext(io) {
            if (normalizedQuery.isNotEmpty()) dao.insertPick(QueryHistoryEntity(query = normalizedQuery, resultId = resultId, timestamp = now))
            dao.insertLaunch(LaunchEventEntity(resultId = resultId, timestamp = now))
        }
    }

    override suspend fun topLaunchedIds(limit: Int): List<String> = mutex.withLock {
        ensureLoaded()
        Ranker.topByFrecency(launches, System.currentTimeMillis(), limit)
    }

    override suspend fun launchesSince(sinceMillis: Long): Int = mutex.withLock {
        ensureLoaded()
        launches.count { it.timestampMillis >= sinceMillis }
    }

    override suspend fun forget(resultId: String) {
        mutex.withLock {
            picks.removeAll { it.resultId == resultId }
            launches.removeAll { it.resultId == resultId }
            frecencyComputedAt = 0
        }
        withContext(io) {
            dao.forgetPicks(resultId)
            dao.forgetLaunches(resultId)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            picks.clear()
            launches.clear()
            frecency = emptyMap()
        }
        withContext(io) {
            dao.clearPicks()
            dao.clearLaunches()
        }
    }

    private companion object {
        const val MAX_PICKS = 2000
        /** exp(-90/14) ≈ 0.002: older launches no longer matter. */
        const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
}
