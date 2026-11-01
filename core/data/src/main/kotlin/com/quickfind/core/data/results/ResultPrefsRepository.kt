package com.quickfind.core.data.results

import com.quickfind.core.data.db.HiddenResultEntity
import com.quickfind.core.data.db.PinnedResultEntity
import com.quickfind.core.data.db.ResultPrefsDao
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.model.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** "Hide from results" and "Pin" from the long-press sheet (spec 7). */
interface ResultPrefsRepository {
    val hiddenIds: StateFlow<Set<String>>
    val hidden: Flow<List<HiddenResultEntity>>
    val pinned: Flow<List<SearchResult>>

    suspend fun hide(result: SearchResult)
    suspend fun unhide(resultId: String)
    suspend fun pin(result: SearchResult)
    suspend fun unpin(resultId: String)
}

@Singleton
class RoomResultPrefsRepository @Inject constructor(
    private val dao: ResultPrefsDao,
    private val json: Json,
    @ApplicationScope scope: CoroutineScope,
) : ResultPrefsRepository {

    override val hidden: Flow<List<HiddenResultEntity>> = dao.hidden()

    override val hiddenIds: StateFlow<Set<String>> = dao.hidden()
        .map { list -> list.mapTo(HashSet()) { it.resultId } }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    override val pinned: Flow<List<SearchResult>> = dao.pinned().map { list ->
        list.mapNotNull { runCatching { json.decodeFromString(SearchResult.serializer(), it.payload) }.getOrNull() }
    }

    override suspend fun hide(result: SearchResult) {
        dao.hide(HiddenResultEntity(result.id, result.title, result.type.name, System.currentTimeMillis()))
        dao.unpin(result.id)
    }

    override suspend fun unhide(resultId: String) = dao.unhide(resultId)

    override suspend fun pin(result: SearchResult) {
        // Store without match highlighting: it is meaningless outside the original query.
        val clean = result.copy(matchedRanges = emptyList(), matchScore = 0)
        dao.pin(PinnedResultEntity(result.id, json.encodeToString(SearchResult.serializer(), clean), System.currentTimeMillis()))
    }

    override suspend fun unpin(resultId: String) = dao.unpin(resultId)
}
