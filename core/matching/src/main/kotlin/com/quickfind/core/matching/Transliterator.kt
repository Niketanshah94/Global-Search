package com.quickfind.core.matching

/**
 * Romanizes Indic scripts so Latin typing finds native-script names: "rahul" → "राहुल".
 *
 * The Unicode Indic blocks share the ISCII layout, so each script is mapped onto its Devanagari
 * equivalent and one table covers Hindi, Marathi, Gujarati, Bengali, Punjabi, Odia, Tamil,
 * Telugu, Kannada and Malayalam. The output is tuned for matching, not for display:
 * long vowels collapse (आ → a, ई → i) and the final inherent "a" is dropped, which is how
 * people actually type names.
 */
object Transliterator {
    private const val DEVANAGARI = 0x0900

    /** Indic blocks laid out like Devanagari (start of block). */
    private val BLOCKS = intArrayOf(0x0900, 0x0980, 0x0A00, 0x0A80, 0x0B00, 0x0B80, 0x0C00, 0x0C80, 0x0D00)

    private const val VIRAMA = 0x4D
    private const val NUKTA = 0x3C

    // Offsets within the block.
    private val CONSONANTS = mapOf(
        0x15 to "k", 0x16 to "kh", 0x17 to "g", 0x18 to "gh", 0x19 to "n",
        0x1A to "ch", 0x1B to "chh", 0x1C to "j", 0x1D to "jh", 0x1E to "n",
        0x1F to "t", 0x20 to "th", 0x21 to "d", 0x22 to "dh", 0x23 to "n",
        0x24 to "t", 0x25 to "th", 0x26 to "d", 0x27 to "dh", 0x28 to "n", 0x29 to "n",
        0x2A to "p", 0x2B to "ph", 0x2C to "b", 0x2D to "bh", 0x2E to "m",
        0x2F to "y", 0x30 to "r", 0x31 to "r", 0x32 to "l", 0x33 to "l", 0x34 to "l", 0x35 to "v",
        0x36 to "sh", 0x37 to "sh", 0x38 to "s", 0x39 to "h",
        0x58 to "q", 0x59 to "kh", 0x5A to "g", 0x5B to "z", 0x5C to "d", 0x5D to "dh", 0x5E to "f", 0x5F to "y",
    )

    private val VOWELS = mapOf(
        0x05 to "a", 0x06 to "a", 0x07 to "i", 0x08 to "i", 0x09 to "u", 0x0A to "u", 0x0B to "ri",
        0x0C to "li", 0x0D to "e", 0x0E to "e", 0x0F to "e", 0x10 to "ai", 0x11 to "o", 0x12 to "o", 0x13 to "o", 0x14 to "au",
    )

    private val MATRAS = mapOf(
        0x3E to "a", 0x3F to "i", 0x40 to "i", 0x41 to "u", 0x42 to "u", 0x43 to "ri", 0x44 to "ri",
        0x45 to "e", 0x46 to "e", 0x47 to "e", 0x48 to "ai", 0x49 to "o", 0x4A to "o", 0x4B to "o", 0x4C to "au",
    )

    // 0x70 is Gurmukhi tippi (nasal, as in ਸਿੰਘ "singh"); 0x71 addak doubles the next consonant.
    private val SIGNS = mapOf(0x01 to "n", 0x02 to "n", 0x03 to "h", 0x50 to "om", 0x70 to "n")

    fun containsIndic(text: String): Boolean = text.any { offsetOf(it) != null }

    /** Offset of [c] inside its Indic block, or null for non-Indic characters. */
    private fun offsetOf(c: Char): Int? {
        val code = c.code
        for (start in BLOCKS) if (code in start until start + 0x80) return code - start
        return null
    }

    fun toLatin(text: String): String {
        val out = StringBuilder(text.length * 2)
        var pendingSchwa = false // a consonant was written and still carries its inherent "a"
        fun flushSchwa(nextIsLetter: Boolean) {
            // Keep the inherent vowel between consonants ("kamala"), drop it at the end of a word.
            if (pendingSchwa && nextIsLetter) out.append('a')
            pendingSchwa = false
        }
        for (c in text) {
            val off = offsetOf(c)
            if (off == null) {
                flushSchwa(nextIsLetter = false)
                out.append(c)
                continue
            }
            when {
                off == VIRAMA -> pendingSchwa = false
                off == NUKTA -> Unit
                CONSONANTS.containsKey(off) -> {
                    flushSchwa(nextIsLetter = true)
                    out.append(CONSONANTS.getValue(off))
                    pendingSchwa = true
                }
                MATRAS.containsKey(off) -> {
                    pendingSchwa = false
                    out.append(MATRAS.getValue(off))
                }
                VOWELS.containsKey(off) -> {
                    flushSchwa(nextIsLetter = true)
                    out.append(VOWELS.getValue(off))
                }
                SIGNS.containsKey(off) -> {
                    flushSchwa(nextIsLetter = true)
                    out.append(SIGNS.getValue(off))
                }
                off in 0x66..0x6F -> {
                    flushSchwa(nextIsLetter = false)
                    out.append('0' + (off - 0x66))
                }
                else -> flushSchwa(nextIsLetter = false)
            }
        }
        flushSchwa(nextIsLetter = false)
        return out.toString()
    }
}

/**
 * A title prepared for matching in both its own script and, for Indic text, in Latin letters.
 * Matching the romanized form loses highlight positions, so it scores slightly lower.
 */
class SearchableText(val text: String) {
    val primary: NormalizedText = TextNormalizer.normalize(text)
    val romanized: NormalizedText? =
        if (Transliterator.containsIndic(text)) TextNormalizer.normalize(Transliterator.toLatin(text)) else null
}

fun FuzzyMatcher.match(query: NormalizedQuery, target: SearchableText): MatchResult? {
    val direct = match(query, target.primary)
    val roman = target.romanized?.let { match(query, it) }?.let { MatchResult(it.score - 10, emptyList()) }
    return when {
        direct == null -> roman
        roman == null -> direct
        roman.score > direct.score -> roman
        else -> direct
    }
}
