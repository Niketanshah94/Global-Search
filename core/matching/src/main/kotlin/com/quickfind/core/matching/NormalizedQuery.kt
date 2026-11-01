package com.quickfind.core.matching

/**
 * A user query prepared once per keystroke and shared by every source.
 *
 * @property raw what the user typed, untouched (instant sources such as the calculator use it).
 * @property normalized folded and trimmed, single-spaced.
 * @property tokens [normalized] split on separators; every token must match (AND).
 * @property digits digits only, for phone-number substring matching.
 */
class NormalizedQuery private constructor(
    val raw: String,
    val normalized: String,
    val tokens: List<String>,
    val digits: String,
) {
    val isEmpty: Boolean get() = tokens.isEmpty()

    /** True when the query is mostly digits, e.g. "98765" or "+91 98765". */
    val looksNumeric: Boolean
        get() = digits.length >= MatchWeights.PHONE_MIN_DIGITS &&
            raw.count { it.isLetter() } == 0

    override fun toString(): String = "NormalizedQuery($raw -> $tokens)"

    companion object {
        val EMPTY = of("")

        fun of(raw: String): NormalizedQuery {
            val folded = TextNormalizer.fold(raw)
            val tokens = ArrayList<String>()
            val current = StringBuilder()
            for (c in folded) {
                if (TextNormalizer.isSeparator(c)) {
                    if (current.isNotEmpty()) tokens += current.toString()
                    current.clear()
                } else {
                    current.append(c)
                }
            }
            if (current.isNotEmpty()) tokens += current.toString()
            return NormalizedQuery(
                raw = raw,
                normalized = tokens.joinToString(" "),
                tokens = tokens,
                digits = PhoneNumbers.digitsOnly(raw),
            )
        }
    }
}
