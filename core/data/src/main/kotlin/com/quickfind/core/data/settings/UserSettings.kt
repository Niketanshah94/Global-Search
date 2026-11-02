package com.quickfind.core.data.settings

import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.matching.ranking.RankingWeights
import com.quickfind.core.model.SourceType
import com.quickfind.core.model.WebSearchEngine

enum class SearchBarPosition { TOP, BOTTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the user can change (spec section 14). Defaults work with zero permissions. */
data class UserSettings(
    // Sources
    val appsEnabled: Boolean = true,
    val contactsEnabled: Boolean = false,
    val filesEnabled: Boolean = false,
    val settingsEnabled: Boolean = true,
    val calculatorEnabled: Boolean = true,
    val calendarEnabled: Boolean = false,
    /** Needs "Notification access" granted in system settings. */
    val notificationsEnabled: Boolean = false,
    /** On-device embedding model: "payment" finds PhonePe, "taxi" finds Uber. */
    val semanticEnabled: Boolean = true,
    val webEnabled: Boolean = true,
    val showWorkApps: Boolean = true,

    // Web
    val searchEngine: WebSearchEngine = WebSearchEngine.GOOGLE,
    /** "f ", "c ", "yt " … shortcuts at the start of a query. */
    val prefixesEnabled: Boolean = true,
    /** Download public exchange rates when a currency query is typed (the only network use). */
    val currencyEnabled: Boolean = true,
    val customSearchTemplate: String = "",

    // Look and behaviour
    val searchBarPosition: SearchBarPosition = SearchBarPosition.TOP,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Off by default so the Aurora palette shows; users can switch to wallpaper colours.
    val dynamicColor: Boolean = false,
    val clearQueryOnReopen: Boolean = true,
    val persistentNotification: Boolean = false,
    val directCalling: Boolean = false,

    // Ranking
    val rankingMode: RankingMode = RankingMode.SMART,
    val typeBias: Map<SourceType, Int> = RankingWeights.DEFAULT_TYPE_BIAS,

    // Privacy and indexing
    val indexingPaused: Boolean = false,
    val indexHiddenFolders: Boolean = false,
    val excludedFolders: Set<String> = emptySet(),
    val documentTreeUris: Set<String> = emptySet(),

    val onboardingDone: Boolean = false,
) {
    fun isSourceEnabled(type: SourceType): Boolean = when (type) {
        SourceType.APP -> appsEnabled
        SourceType.CONTACT -> contactsEnabled
        SourceType.FILE, SourceType.FOLDER -> filesEnabled
        SourceType.SETTING -> settingsEnabled
        SourceType.CALCULATOR, SourceType.TOOL -> calculatorEnabled
        SourceType.WEB, SourceType.URL -> webEnabled
        SourceType.PHONE_NUMBER -> true
        SourceType.CALENDAR -> calendarEnabled
        SourceType.NOTIFICATION -> notificationsEnabled
    }
}
