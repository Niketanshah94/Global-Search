package com.quickfind.feature.search

import androidx.compose.runtime.Immutable
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType

/** Restricts search to one source, from the "Search files"/"Search contacts" shortcuts. */
enum class SearchScope(val types: Set<SourceType>) {
    APPS(setOf(SourceType.APP)),
    CONTACTS(setOf(SourceType.CONTACT, SourceType.PHONE_NUMBER)),
    FILES(setOf(SourceType.FILE, SourceType.FOLDER)),
    SETTINGS(setOf(SourceType.SETTING)),
}

@Immutable
data class SectionUi(
    val section: ResultSection,
    val visible: List<SearchResult>,
    /** How many more items "Show more" reveals (0 when expanded or nothing more). */
    val moreCount: Int,
    val expanded: Boolean,
)

@Immutable
data class EmptyUi(
    val pinned: List<SearchResult> = emptyList(),
    val recentApps: List<SearchResult> = emptyList(),
    val recentFiles: List<SearchResult> = emptyList(),
    /** The clipboard holds text or a link. Content is only read when the chip is tapped. */
    val clipboardAvailable: Boolean = false,
    /** A one-time code from the last 10 minutes of notifications. */
    val otp: OtpUi? = null,
    val foundToday: Int = 0,
    /** Free tier: an OTP is waiting; the pill offers Pro instead of the code. */
    val otpLocked: Boolean = false,
)

/** Free tier: what Pro would also have shown for this query. */
@Immutable
data class LockedUi(val count: Int, val sections: List<ResultSection>, val types: Set<SourceType>)

@Immutable
data class OtpUi(val code: String, val source: String)

@Immutable
data class SearchUiState(
    val query: String = "",
    val scope: SearchScope? = null,
    val topHit: SearchResult? = null,
    val sections: List<SectionUi> = emptyList(),
    /** Indexed sources are still running. */
    val isSearching: Boolean = false,
    val empty: EmptyUi = EmptyUi(),
    val pinnedIds: Set<String> = emptySet(),
    /** Result id -> the nickname the user gave it. */
    val nicknames: Map<String, String> = emptyMap(),
    /** Filter chip selection; null means "All". */
    val filter: ResultSection? = null,
    /** Results per section (top hit included), for the filter chips. */
    val counts: List<Pair<ResultSection, Int>> = emptyList(),
    val isPro: Boolean = true,
    val locked: LockedUi? = null,
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = hasQuery && !isSearching && topHit == null && sections.isEmpty() && locked == null
}
