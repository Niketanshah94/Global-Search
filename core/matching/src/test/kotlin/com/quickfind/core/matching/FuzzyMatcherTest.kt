package com.quickfind.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    private fun score(query: String, target: String): Int? = FuzzyMatcher.match(query, target)?.score

    /** Spec 4.6 required cases: (query, target, shouldMatch). */
    private val matchTable = listOf(
        Triple("gm", "Google Maps", true),
        Triple("ytm", "YouTube Music", true),
        Triple("xyz", "Camera", false),
        Triple("cafe", "Café Coffee Day", true),
        Triple("whatsap", "WhatsApp", true),
        Triple("wh", "WhatsApp", true),
        Triple("resfin", "ResumeFinal_v2.pdf", true),
        Triple("final v2", "ResumeFinal_v2.pdf", true),
        Triple("maps google", "Google Maps", true),
        Triple("maps apple", "Google Maps", false),
    )

    @Test
    fun requiredMatchTable() {
        for ((query, target, expected) in matchTable) {
            val result = FuzzyMatcher.match(query, target)
            assertEquals("'$query' vs '$target'", expected, result != null)
        }
    }

    /** (query, better, worse): the first target must outrank the second. */
    private val orderingTable = listOf(
        Triple("cam", "Camera", "Scam Detector"),
        Triple("gm", "Google Maps", "Program Manager"),
        Triple("wh", "WhatsApp", "Weather Home"),
        Triple("ytm", "YouTube Music", "My Tiny Mixer"),
        Triple("set", "Settings", "Reset Tool"),
        Triple("calc", "Calculator", "Calendar Clock"),
        Triple("camera", "Camera", "Camera Roll Backup"),
    )

    @Test
    fun requiredOrderingTable() {
        for ((query, better, worse) in orderingTable) {
            val b = score(query, better)
            val w = score(query, worse)
            assertNotNull("'$query' should match '$better'", b)
            if (w != null) assertTrue("'$query': '$better'($b) should beat '$worse'($w)", b!! > w)
        }
    }

    @Test
    fun wordStartsBeatGreedyLetters() {
        // Greedy would pick the 'g' and 'm' inside "Google"; the DP must pick word starts.
        val r = FuzzyMatcher.match("gm", "Google Maps")!!
        assertEquals(listOf(0..0, 7..7), r.ranges)
    }

    @Test
    fun highlightRangesPointAtOriginalText() {
        val r = FuzzyMatcher.match("cafe", "Café Coffee Day")!!
        assertEquals(listOf(0..3), r.ranges)
    }

    @Test
    fun camelCaseBoundariesCount() {
        val camel = score("ft", "FileTransfer")!!
        val mid = score("ft", "Fileotransfer")!!
        assertTrue("camel boundary should score higher ($camel vs $mid)", camel > mid)
    }

    @Test
    fun exactTitleBeatsPrefix() {
        assertTrue(score("maps", "Maps")!! > score("maps", "Maps Go")!!)
    }

    @Test
    fun typoToleranceOnlyFromFourCharacters() {
        assertNotNull("one swapped letter", FuzzyMatcher.match("whta", "What's New"))
        assertNotNull("one substitution", FuzzyMatcher.match("camira", "Camera"))
        assertNull("too short for typo tolerance", FuzzyMatcher.match("cma", "Mac"))
    }

    @Test
    fun typoIsPenalised() {
        assertTrue(score("camera", "Camera")!! > score("camira", "Camera")!!)
    }

    @Test
    fun devanagariIsKeptAndMatched() {
        val n = TextNormalizer.normalize("नमस्ते")
        assertEquals("नमस्ते", n.normalized)
        assertNotNull(FuzzyMatcher.match("नम", "नमस्ते दुनिया"))
    }

    @Test
    fun emptyQueryNeverMatches() {
        assertNull(FuzzyMatcher.match("", "Camera"))
        assertNull(FuzzyMatcher.match("   ", "Camera"))
    }
}
