package com.quickfind.core.matching.instant

import com.quickfind.core.model.WebSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryDetectorsTest {
    @Test
    fun phoneNumbers() {
        for (q in listOf("+91 98765 43210", "9876543210", "(555) 123-4567")) assertTrue(q, QueryDetectors.looksLikePhoneNumber(q))
        for (q in listOf("123", "2+3", "abc123456", "12345678901234567")) assertFalse(q, QueryDetectors.looksLikePhoneNumber(q))
    }

    @Test
    fun urls() {
        for (q in listOf("example.com", "www.wikipedia.org", "https://x.org/path?q=1", "news.ycombinator.com")) {
            assertTrue(q, QueryDetectors.looksLikeUrl(q))
        }
        for (q in listOf("3.14", "resume.pdf", "hello world.com", "localhost", "photo.jpg")) {
            assertFalse(q, QueryDetectors.looksLikeUrl(q))
        }
        assertEquals("https://example.com", QueryDetectors.toUrl("example.com"))
    }

    @Test
    fun webSearchUrls() {
        assertEquals("https://www.google.com/search?q=hello+world", QueryDetectors.webSearchUrl(WebSearchEngine.GOOGLE, "hello world", null))
        assertEquals("https://s.example/?q=a%26b", QueryDetectors.webSearchUrl(WebSearchEngine.CUSTOM, "a&b", "https://s.example/?q=%s"))
        assertEquals("https://www.google.com/search?q=x", QueryDetectors.webSearchUrl(WebSearchEngine.CUSTOM, "x", "no placeholder"))
    }
}
