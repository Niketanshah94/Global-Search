package com.quickfind.core.matching

/** Finds one-time codes in notification text: "Your OTP is 482913", "G-123456 is your Google code". */
object OtpDetector {
    private val KEYWORDS = Regex(
        """\b(otp|one[- ]time|verification|verify|passcode|security code|login code|auth(entication)? code|code|pin)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val CODE = Regex("""(?<![\d₹$.,/-])(?:[A-Z]-)?(\d{4,8})(?![\d.,/-])""")
    private val NOT_A_CODE = Regex("""(?i)(rs\.?|inr|₹|\$|a/c|acct|account|card|xx)\s*$""")

    /** Java's \b does not treat Devanagari as word characters, so these are plain substring checks. */
    private val INDIC_KEYWORDS = listOf("ओटीपी", "सत्यापन", "कोड", "ઓટીપી")

    fun find(text: String): String? {
        if (!KEYWORDS.containsMatchIn(text) && INDIC_KEYWORDS.none { it in text }) return null
        for (m in CODE.findAll(text)) {
            val before = text.substring(0, m.range.first)
            // Skip amounts and masked account numbers: "Rs 2500", "A/c XX1234".
            if (NOT_A_CODE.containsMatchIn(before.takeLast(8))) continue
            val code = m.groupValues[1]
            // Years like "2026" are not codes.
            if (code.length == 4 && code.toInt() in 1990..2099) continue
            return code
        }
        return null
    }
}
