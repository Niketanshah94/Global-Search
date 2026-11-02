package com.quickfind.core.sources

import com.quickfind.core.data.results.ResultPrefsRepository
import com.quickfind.core.matching.NormalizedQuery
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceKind
import com.quickfind.core.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Results the user gave a nickname to: "ma" -> Mom. An exact nickname beats everything but tools. */
@Singleton
class NicknameSource @Inject constructor(
    private val prefs: ResultPrefsRepository,
) : SearchSource {
    override val type = SourceType.APP
    override val coversTypes: Set<SourceType> = SourceType.entries.toSet()
    override val kind = SourceKind.IN_MEMORY
    override val isEnabled: StateFlow<Boolean> = MutableStateFlow(true)

    override suspend fun search(query: NormalizedQuery, limit: Int): List<SearchResult> {
        val q = query.normalized
        if (q.isEmpty()) return emptyList()
        return prefs.nicknames.value.mapNotNull { (nick, result) ->
            val score = when {
                nick == q -> EXACT
                nick.startsWith(q) -> PREFIX
                else -> return@mapNotNull null
            }
            result.copy(matchScore = score, subtitle = result.subtitle?.let { "“$nick” · $it" } ?: "“$nick”")
        }.take(limit)
    }

    private companion object {
        const val EXACT = 600
        const val PREFIX = 250
    }
}
