package com.quickfind.core.matching.ranking

import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType

data class RankedResult(val result: SearchResult, val finalScore: Int)

data class SectionResults(val section: ResultSection, val items: List<RankedResult>)

data class AssembledResults(
    val topHit: RankedResult?,
    val sections: List<SectionResults>,
) {
    val isEmpty: Boolean get() = topHit == null && sections.isEmpty()

    companion object {
        val EMPTY = AssembledResults(null, emptyList())
    }
}

/**
 * Ranks, de-duplicates and groups results (spec 4.7 "Display order").
 *
 * - The calculator always wins the top hit when it has an answer.
 * - Web search is only the top hit when nothing else matched.
 * - The top hit is removed from its section so it is not shown twice.
 */
object ResultAssembler {
    fun assemble(
        results: List<SearchResult>,
        context: RankingContext,
        hiddenIds: Set<String> = emptySet(),
    ): AssembledResults {
        if (results.isEmpty()) return AssembledResults.EMPTY

        val bestById = LinkedHashMap<String, RankedResult>()
        for (r in results) {
            if (r.id in hiddenIds) continue
            val ranked = RankedResult(r, context.finalScore(r))
            val existing = bestById[r.id]
            if (existing == null || ranked.finalScore > existing.finalScore) bestById[r.id] = ranked
        }
        if (bestById.isEmpty()) return AssembledResults.EMPTY

        val ordered = bestById.values.sortedWith(
            compareByDescending<RankedResult> { it.finalScore }.thenBy { it.result.title.lowercase() },
        )

        val topHit = ordered.firstOrNull { it.result.type == SourceType.CALCULATOR }
            ?: ordered.firstOrNull { it.result.type != SourceType.WEB }
            ?: ordered.first()

        val sections = ResultSection.entries.mapNotNull { section ->
            val items = ordered.filter { it !== topHit && it.result.type in section.types }
                // Web search is "always the last item": keep it below URL suggestions.
                .sortedBy { if (it.result.type == SourceType.WEB) 1 else 0 }
            if (items.isEmpty()) null else SectionResults(section, items)
        }
        return AssembledResults(topHit, sections)
    }
}
