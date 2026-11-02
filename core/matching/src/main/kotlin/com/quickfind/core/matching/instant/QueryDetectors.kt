package com.quickfind.core.matching.instant

import com.quickfind.core.matching.PhoneNumbers
import com.quickfind.core.model.WebSearchEngine
import java.net.URLEncoder

/** Pattern checks for instant sources (spec 5.5). */
object QueryDetectors {
    private val PHONE = Regex("""^\+?\(?[0-9][0-9\s\-().]{4,}$""")
    private val DOMAIN = Regex(
        """^(https?://)?(www\.)?([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,24}(:\d{1,5})?(/\S*)?$""",
        RegexOption.IGNORE_CASE,
    )

    private val EMAIL = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,24}$""")
    private val PIN_CODE = Regex("""\b\d{5,6}\b""")
    private val STREET_WORDS = Regex(
        """\b(road|rd|street|st|avenue|ave|lane|ln|nagar|marg|sector|colony|society|floor|flat|building|bldg|near|opp|chowk|block)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun looksLikeEmail(query: String): Boolean = EMAIL.matches(query.trim())

    /**
     * A pasted postal address: several words, a comma, and either a PIN/ZIP code or a street word.
     * Kept strict so ordinary searches never offer "Open in Maps".
     */
    fun looksLikeAddress(query: String): Boolean {
        val q = query.trim()
        if (q.length < 12 || !q.contains(',')) return false
        if (q.split(Regex("\\s+")).size < 3) return false
        return PIN_CODE.containsMatchIn(q) || STREET_WORDS.containsMatchIn(q)
    }

    /** "+91 98765 43210", "9876543210", "(555) 123-4567". Needs 6 to 15 digits. */
    fun looksLikePhoneNumber(query: String): Boolean {
        val q = query.trim()
        if (!PHONE.matches(q)) return false
        val digits = PhoneNumbers.digitsOnly(q).length
        return digits in 6..15
    }

    /** "example.com", "https://x.org/path". Rejects things like "3.14" and file names with spaces. */
    fun looksLikeUrl(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty() || q.any { it.isWhitespace() }) return false
        if (!DOMAIN.matches(q)) return false
        // Common file extensions typed as a search should not become a website.
        val tld = q.substringAfterLast('.').substringBefore('/').substringBefore(':').lowercase()
        return tld !in FILE_EXTENSIONS
    }

    fun toUrl(query: String): String {
        val q = query.trim()
        return if (q.startsWith("http://", true) || q.startsWith("https://", true)) q else "https://$q"
    }

    /** Builds a web search URL; [customTemplate] is used for [WebSearchEngine.CUSTOM] and must contain %s. */
    fun webSearchUrl(engine: WebSearchEngine, query: String, customTemplate: String?): String {
        val template = if (engine == WebSearchEngine.CUSTOM && customTemplate?.contains("%s") == true) {
            customTemplate
        } else if (engine == WebSearchEngine.CUSTOM) {
            WebSearchEngine.GOOGLE.template
        } else {
            engine.template
        }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return template.replace("%s", encoded)
    }

    private val FILE_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "gif",
        "webp", "heic", "mp3", "mp4", "mkv", "apk", "zip", "csv", "json", "xml", "md",
    )
}
