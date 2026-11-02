package com.quickfind.core.matching.instant

import com.quickfind.core.model.SourceType
import com.quickfind.core.model.WebSearchEngine

/**
 * Leading shortcuts typed before a query (spec: "aliases"):
 * "f report" files, "c rahul" contacts, "a cam" apps, "s wifi" settings,
 * "yt lofi" YouTube, "g weather" Google, "ddg privacy" DuckDuckGo, "play maps" Play Store.
 */
object QueryPrefixes {
    sealed interface Prefix {
        data class Scope(val types: Set<SourceType>) : Prefix
        data class Web(val engine: WebSearchEngine) : Prefix
    }

    data class Parsed(val prefix: Prefix, val rest: String)

    val SCOPES: Map<String, Set<SourceType>> = mapOf(
        "f" to setOf(SourceType.FILE, SourceType.FOLDER),
        "c" to setOf(SourceType.CONTACT, SourceType.PHONE_NUMBER),
        "a" to setOf(SourceType.APP),
        "s" to setOf(SourceType.SETTING),
    )

    val ENGINES: Map<String, WebSearchEngine> = mapOf(
        "g" to WebSearchEngine.GOOGLE,
        "yt" to WebSearchEngine.YOUTUBE,
        "ddg" to WebSearchEngine.DUCKDUCKGO,
        "bing" to WebSearchEngine.BING,
        "pplx" to WebSearchEngine.PERPLEXITY,
        "play" to WebSearchEngine.PLAY_STORE,
    )

    /** Returns the prefix and the remaining text, or null if the query has no prefix. */
    fun parse(raw: String): Parsed? {
        val trimmed = raw.trimStart()
        val space = trimmed.indexOf(' ')
        if (space <= 0) return null
        val word = trimmed.substring(0, space).lowercase()
        val rest = trimmed.substring(space + 1)
        SCOPES[word]?.let { return Parsed(Prefix.Scope(it), rest) }
        ENGINES[word]?.let { return Parsed(Prefix.Web(it), rest) }
        return null
    }
}
