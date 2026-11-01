package com.quickfind.core.sources

import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import kotlinx.coroutines.flow.StateFlow

/** One data source behind the search bar (spec 4.2). */
interface SearchSource {
    val type: SourceType
    val kind: SourceKind
    val isEnabled: StateFlow<Boolean>

    /** Must respect its time budget and check coroutine cancellation inside loops. */
    suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult>

    /** Called once at process start: load snapshots, register observers. Must not block. */
    fun start() {}

    /** Called whenever the search screen becomes visible (permissions may have changed). */
    fun onVisible() {}

    /** Rebuilds a result from its id, for "recent apps" on the empty screen. */
    fun resultForId(id: String): SearchResult? = null

    /** Items for the empty-query screen ("recent files"). */
    suspend fun recent(limit: Int): List<SearchResult> = emptyList()
}
