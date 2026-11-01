package com.quickfind.core.matching

/** Result of matching a query against one piece of text. */
data class MatchResult(
    val score: Int,
    /** Ranges in the *original* text, merged and sorted, for highlighting. */
    val ranges: List<IntRange>,
)

/**
 * Sublime Text-style subsequence matcher, written from scratch (spec 4.6).
 *
 * Each query token must appear in the target as an in-order subsequence. The best alignment
 * per token is found with a small dynamic program so that "gm" lands on the word starts of
 * "Google Maps" rather than the first 'g' and 'm' a greedy scan would pick. If a token of
 * 4+ characters has no subsequence match, one Damerau-Levenshtein edit against a word prefix
 * is allowed, at a penalty.
 *
 * Thread-safe and allocation-light: a cheap greedy pre-check rejects most targets before
 * the DP runs.
 */
object FuzzyMatcher {
    private const val NEG = Int.MIN_VALUE / 4

    fun match(query: NormalizedQuery, target: NormalizedText): MatchResult? {
        if (query.isEmpty || target.normalized.isEmpty()) return null

        var total = 0
        val matched = BooleanArray(target.normalized.length)
        var firstMatch = Int.MAX_VALUE
        var anyTypo = false

        for (token in query.tokens) {
            val tokenMatch = matchToken(token, target)
                ?: typoMatch(token, target)?.also { anyTypo = true }
                ?: return null
            total += tokenMatch.score
            for (p in tokenMatch.positions) {
                matched[p] = true
                if (p < firstMatch) firstMatch = p
            }
        }

        // Penalties are computed once for the whole target, not per token.
        var leadingLetters = 0
        for (i in 0 until firstMatch.coerceAtMost(target.normalized.length)) {
            if (!target.separator[i]) leadingLetters++
        }
        total += maxOf(leadingLetters * MatchWeights.LEADING_UNMATCHED, MatchWeights.LEADING_UNMATCHED_CAP)

        val matchedCount = matched.count { it }
        val otherUnmatched = (target.letterCount - matchedCount - leadingLetters).coerceAtLeast(0)
        total += maxOf(otherUnmatched * MatchWeights.OTHER_UNMATCHED, MatchWeights.OTHER_UNMATCHED_CAP)

        if (!anyTypo) {
            val compactQuery = query.normalized
            val compactTarget = target.normalized.trim()
            if (compactTarget.startsWith(compactQuery)) total += MatchWeights.EXACT_PREFIX
            if (compactTarget == compactQuery) total += MatchWeights.EXACT_EQUAL
        }

        return MatchResult(total, toOriginalRanges(matched, target))
    }

    /** Convenience for one-off matching (tests, small lists). Prefer pre-normalized targets. */
    fun match(query: String, target: String): MatchResult? =
        match(NormalizedQuery.of(query), TextNormalizer.normalize(target))

    private class TokenMatch(val score: Int, val positions: IntArray)

    private fun matchToken(token: String, target: NormalizedText): TokenMatch? {
        val t = target.normalized
        val n = token.length
        val m = t.length
        if (n > m) return null
        if (!isSubsequence(token, t)) return null

        // best[i][j]: best score with token[i] matched exactly at target position j.
        val best = Array(n) { IntArray(m) { NEG } }
        val parent = Array(n) { IntArray(m) { -1 } }

        for (j in 0 until m) {
            if (t[j] == token[0] && !target.separator[j]) best[0][j] = bonus(target, j)
        }
        for (i in 1 until n) {
            // Running max of best[i-1][0..j-2] (non-adjacent predecessor), with its index.
            var runMax = NEG
            var runIdx = -1
            for (j in 1 until m) {
                if (j >= 2 && best[i - 1][j - 2] > runMax) {
                    runMax = best[i - 1][j - 2]
                    runIdx = j - 2
                }
                if (t[j] != token[i] || target.separator[j]) continue
                val b = bonus(target, j)
                val adjacent = best[i - 1][j - 1]
                val viaAdjacent = if (adjacent > NEG) adjacent + MatchWeights.CONSECUTIVE + b else NEG
                val viaGap = if (runMax > NEG) runMax + b else NEG
                if (viaAdjacent >= viaGap && viaAdjacent > NEG) {
                    best[i][j] = viaAdjacent
                    parent[i][j] = j - 1
                } else if (viaGap > NEG) {
                    best[i][j] = viaGap
                    parent[i][j] = runIdx
                }
            }
        }

        var endJ = -1
        var endScore = NEG
        for (j in 0 until m) {
            // Strictly greater keeps the earliest end on ties.
            if (best[n - 1][j] > endScore) {
                endScore = best[n - 1][j]
                endJ = j
            }
        }
        if (endJ < 0) return null

        val positions = IntArray(n)
        var j = endJ
        for (i in n - 1 downTo 0) {
            positions[i] = j
            j = parent[i][j]
        }
        return TokenMatch(endScore, positions)
    }

    private fun bonus(target: NormalizedText, j: Int): Int = when {
        target.wordStart[j] -> MatchWeights.WORD_START
        target.camelBoundary[j] -> MatchWeights.CAMEL_BOUNDARY
        else -> 0
    }

    private fun isSubsequence(token: String, text: String): Boolean {
        var i = 0
        for (c in text) {
            if (c == token[i]) {
                i++
                if (i == token.length) return true
            }
        }
        return false
    }

    private fun typoMatch(token: String, target: NormalizedText): TokenMatch? {
        if (token.length < MatchWeights.TYPO_MIN_QUERY_LENGTH) return null
        for ((start, word) in target.words) {
            // Compare against word prefixes of length len-1, len, len+1 (one insert/delete/substitute/swap).
            for (len in (token.length - 1)..(token.length + 1)) {
                if (len <= 0 || len > word.length) continue
                if (EditDistance.withinOne(token, word.substring(0, len))) {
                    val positions = IntArray(len) { start + it }
                    val score = MatchWeights.WORD_START + (len - 1) * MatchWeights.CONSECUTIVE + MatchWeights.TYPO_PENALTY
                    return TokenMatch(score, positions)
                }
            }
        }
        return null
    }

    private fun toOriginalRanges(matched: BooleanArray, target: NormalizedText): List<IntRange> {
        val originalIdx = java.util.TreeSet<Int>()
        for (j in matched.indices) if (matched[j]) originalIdx += target.indexMap[j]
        if (originalIdx.isEmpty()) return emptyList()
        val ranges = ArrayList<IntRange>()
        var start = originalIdx.first()
        var prev = start
        for (i in originalIdx) {
            if (i == start) continue
            if (i != prev + 1) {
                ranges += start..prev
                start = i
            }
            prev = i
        }
        ranges += start..prev
        return ranges
    }
}
