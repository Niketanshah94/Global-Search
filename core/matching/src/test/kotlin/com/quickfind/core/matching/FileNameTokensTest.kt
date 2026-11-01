package com.quickfind.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileNameTokensTest {
    @Test
    fun tokensForStorage() {
        assertEquals("resume final v 2 pdf resumefinalv2pdf", FileNameTokens.of("ResumeFinal_v2.pdf"))
        assertEquals("cafe menu jpg cafemenujpg", FileNameTokens.of("Café Menu.jpg"))
        assertEquals("", FileNameTokens.of("..."))
    }

    @Test
    fun matchExpression() {
        assertEquals("final* v* 2*", FileNameTokens.ftsMatch(NormalizedQuery.of("final v2")))
        assertEquals("resume*", FileNameTokens.ftsMatch(NormalizedQuery.of("Résumé")))
        assertNull(FileNameTokens.ftsMatch(NormalizedQuery.of("  ")))
    }
}
