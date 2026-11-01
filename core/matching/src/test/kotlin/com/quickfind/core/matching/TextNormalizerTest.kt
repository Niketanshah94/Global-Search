package com.quickfind.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    private val foldTable = listOf(
        "Café" to "cafe",
        "Ångström" to "angstrom",
        "Straße" to "strasse",
        "ÉCOLE" to "ecole",
        "Crème Brûlée" to "creme brulee",
        "Ελληνικά" to "ελληνικα",
        "हिन्दी" to "हिन्दी",
    )

    @Test
    fun folds() {
        for ((input, expected) in foldTable) assertEquals(input, expected, TextNormalizer.fold(input))
    }

    @Test
    fun queryTokens() {
        assertEquals(listOf("resume", "final", "v2"), NormalizedQuery.of("Resume_final-v2").tokens)
        assertEquals(listOf("hello", "world"), NormalizedQuery.of("  hello   world ").tokens)
        assertTrue(NormalizedQuery.of("  ").isEmpty)
    }

    @Test
    fun indexMapSurvivesDecomposition() {
        // "é" decomposes into two chars; the 'e' must still map to original index 3.
        val n = TextNormalizer.normalize("Café!")
        assertEquals("cafe!", n.normalized)
        assertEquals(3, n.indexMap[3])
        assertEquals(4, n.indexMap[4])
    }

    @Test
    fun wordsSplitOnSeparatorsCamelCaseAndDigits() {
        val words = TextNormalizer.normalize("ResumeFinal_v2.pdf").words.map { it.second }
        assertEquals(listOf("resume", "final", "v", "2", "pdf"), words)
    }
}
