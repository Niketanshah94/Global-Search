package com.quickfind.core.matching

/** Phone-number normalization (spec 4.5): digits only, substring match. */
object PhoneNumbers {
    fun digitsOnly(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) if (c in '0'..'9') sb.append(c)
        return sb.toString()
    }

    /**
     * Returns a match score if [queryDigits] appears inside [phoneDigits], else null.
     * "98765" matches "+91 98765 43210" (stored as "919876543210").
     */
    fun matchScore(queryDigits: String, phoneDigits: String): Int? {
        if (queryDigits.length < MatchWeights.PHONE_MIN_DIGITS) return null
        val at = phoneDigits.indexOf(queryDigits)
        if (at < 0) return null
        var score = MatchWeights.PHONE_MATCH_BASE + queryDigits.length * MatchWeights.PHONE_MATCH_PER_DIGIT
        if (queryDigits == phoneDigits) score += MatchWeights.EXACT_EQUAL
        return score
    }
}
