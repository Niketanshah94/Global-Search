package com.quickfind.core.matching

/**
 * Every tunable number used by the fuzzy matcher, in one place (spec 4.6).
 * Change these, then run `./gradlew :core:matching:test` to see what moves.
 */
object MatchWeights {
    const val CONSECUTIVE = 15
    const val WORD_START = 30
    const val CAMEL_BOUNDARY = 25
    const val EXACT_PREFIX = 50
    const val EXACT_EQUAL = 100
    const val LEADING_UNMATCHED = -3
    const val LEADING_UNMATCHED_CAP = -30
    const val OTHER_UNMATCHED = -1

    /**
     * Cap on the "other unmatched" penalty. Not in the spec table; without it a long
     * file name loses to a short one purely by length, which the type bias already covers.
     */
    const val OTHER_UNMATCHED_CAP = -40

    /** Typo tolerance: one Damerau-Levenshtein edit against a word prefix. */
    const val TYPO_MIN_QUERY_LENGTH = 4
    const val TYPO_PENALTY = -40

    /** Minimum digits before phone-number substring matching kicks in. */
    const val PHONE_MIN_DIGITS = 3
    const val PHONE_MATCH_BASE = 60
    const val PHONE_MATCH_PER_DIGIT = 5
}
