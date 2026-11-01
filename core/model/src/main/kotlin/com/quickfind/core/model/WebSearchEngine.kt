package com.quickfind.core.model

/** Selectable web search engines (spec 5.5). `%s` is replaced by the URL-encoded query. */
enum class WebSearchEngine(val displayName: String, val template: String) {
    GOOGLE("Google", "https://www.google.com/search?q=%s"),
    BING("Bing", "https://www.bing.com/search?q=%s"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=%s"),
    PERPLEXITY("Perplexity", "https://www.perplexity.ai/search?q=%s"),
    YOUTUBE("YouTube", "https://www.youtube.com/results?search_query=%s"),
    PLAY_STORE("Play Store", "https://play.google.com/store/search?q=%s&c=apps"),
    CUSTOM("Custom", ""),
}
