package com.quickfind.core.matching

import java.text.Normalizer

/**
 * Text prepared for matching (spec 4.5).
 *
 * [normalized] is lowercased with Latin/Greek/Cyrillic diacritics stripped. [indexMap] maps
 * each normalized char back to its index in [original], so matches can be highlighted in the
 * text the user actually sees. Separators are kept in [normalized] so that adjacency stays
 * meaningful; query tokens never contain them.
 */
class NormalizedText internal constructor(
    val original: String,
    val normalized: String,
    internal val indexMap: IntArray,
    internal val wordStart: BooleanArray,
    internal val camelBoundary: BooleanArray,
    internal val separator: BooleanArray,
) {
    /** Number of non-separator characters, used for the "unmatched" penalties. */
    internal val letterCount: Int = separator.count { !it }

    /** Normalized words with their start offsets (word starts and camelCase boundaries). */
    internal val words: List<Pair<Int, String>> by lazy {
        val result = ArrayList<Pair<Int, String>>()
        var i = 0
        while (i < normalized.length) {
            if (separator[i]) {
                i++
                continue
            }
            val start = i
            i++
            while (i < normalized.length && !separator[i] && !wordStart[i] && !camelBoundary[i]) i++
            result += start to normalized.substring(start, i)
        }
        result
    }

    override fun toString(): String = "NormalizedText($original -> $normalized)"
}

object TextNormalizer {
    private const val SEPARATORS = " \t\n_-./\\,;:()[]{}+&'\"!?@#*|~<>=`^$%"

    /** Ligatures and letters that NFD does not decompose. */
    private val SPECIAL_FOLDS = mapOf(
        'ß' to "ss", 'æ' to "ae", 'Æ' to "ae", 'œ' to "oe", 'Œ' to "oe",
        'ø' to "o", 'Ø' to "o", 'ł' to "l", 'Ł' to "l", 'đ' to "d", 'Đ' to "d", 'ı' to "i",
    )

    fun isSeparator(c: Char): Boolean = c.isWhitespace() || SEPARATORS.indexOf(c) >= 0

    fun normalize(text: String): NormalizedText {
        val out = StringBuilder(text.length)
        val map = IntArrayBuilder(text.length)
        var lastBaseScript: Character.UnicodeScript? = null

        for (i in text.indices) {
            val c = text[i]
            val fold = SPECIAL_FOLDS[c]
            if (fold != null) {
                fold.forEach { out.append(it); map.add(i) }
                lastBaseScript = Character.UnicodeScript.LATIN
                continue
            }
            if (c.isSurrogate()) {
                // Emoji and other astral characters: keep as-is, they can still be typed.
                out.append(c)
                map.add(i)
                continue
            }
            val decomposed = if (c.code < 0x80) c.toString() else Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
            for (d in decomposed) {
                if (isCombiningMark(d)) {
                    // Strip accents only from scripts where they are decoration ("Café" -> "cafe").
                    // Devanagari matras and viramas are combining marks too and must stay.
                    if (lastBaseScript in STRIPPABLE_SCRIPTS) continue
                } else {
                    lastBaseScript = runCatching { Character.UnicodeScript.of(d.code) }.getOrNull()
                }
                out.append(d.lowercaseChar())
                map.add(i)
            }
        }

        val normalized = out.toString()
        val indexMap = map.toArray()
        val n = normalized.length
        val separator = BooleanArray(n) { isSeparator(normalized[it]) }
        val wordStart = BooleanArray(n)
        val camel = BooleanArray(n)
        for (j in 0 until n) {
            if (separator[j]) continue
            val prevSep = j == 0 || separator[j - 1]
            val cur = normalized[j]
            val prev = if (j > 0) normalized[j - 1] else ' '
            // A digit run after letters starts a new word ("final2" -> "final", "2").
            val digitAfterLetter = j > 0 && cur.isDigit() && prev.isLetter()
            wordStart[j] = prevSep || digitAfterLetter
            if (!wordStart[j] && j > 0 && indexMap[j] != indexMap[j - 1]) {
                val o = text[indexMap[j]]
                val po = text[indexMap[j - 1]]
                camel[j] = o.isUpperCase() && (po.isLowerCase() || po.isDigit())
            }
        }
        return NormalizedText(text, normalized, indexMap, wordStart, camel, separator)
    }

    /** Lowercase + diacritic fold only; for comparing whole strings. */
    fun fold(text: String): String = normalize(text).normalized

    private val STRIPPABLE_SCRIPTS = setOf(
        Character.UnicodeScript.LATIN,
        Character.UnicodeScript.GREEK,
        Character.UnicodeScript.CYRILLIC,
    )

    private fun isCombiningMark(c: Char): Boolean = when (Character.getType(c).toByte()) {
        Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
        else -> false
    }

    private class IntArrayBuilder(capacity: Int) {
        private var data = IntArray(maxOf(capacity, 4))
        private var size = 0
        fun add(v: Int) {
            if (size == data.size) data = data.copyOf(size * 2)
            data[size++] = v
        }
        fun toArray(): IntArray = data.copyOf(size)
    }
}
