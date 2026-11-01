package com.quickfind.core.matching

/**
 * Builds the FTS side of file search (spec 5.3): the token string stored per file, and the
 * prefix `MATCH` expression for a query. Both sides split on separators, camelCase and
 * letter/digit boundaries so "ResumeFinal_v2.pdf" is findable by "final", "v2" or "resumef".
 */
object FileNameTokens {
    /** "ResumeFinal_v2.pdf" -> "resume final v 2 pdf resumefinalv2pdf". */
    fun of(name: String): String {
        val words = TextNormalizer.normalize(name).words.map { it.second }
        if (words.isEmpty()) return ""
        val compact = words.joinToString("")
        return (words + compact).distinct().joinToString(" ")
    }

    /** "final v2" -> "final* v* 2*". Null when nothing searchable remains. */
    fun ftsMatch(query: NormalizedQuery): String? {
        val parts = query.tokens.flatMap { token -> TextNormalizer.normalize(token).words.map { it.second } }
            .map { word -> word.filter { it.isLetterOrDigit() || Character.getType(it).toByte().let { t -> t == Character.NON_SPACING_MARK || t == Character.COMBINING_SPACING_MARK } } }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        return parts.joinToString(" ") { "$it*" }
    }
}
