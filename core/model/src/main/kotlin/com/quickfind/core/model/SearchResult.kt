package com.quickfind.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    /** Stable across sessions, e.g. "app:com.whatsapp/.Main#0", "contact:123", "file:ms:456". */
    val id: String,
    val type: SourceType,
    val title: String,
    val subtitle: String?,
    /** Score from the fuzzy matcher; ranking adds boosts on top (see core:matching Ranker). */
    val matchScore: Int,
    /** Ranges in [title] (original characters) to highlight. */
    val matchedRanges: List<@Serializable(with = IntRangeSerializer::class) IntRange>,
    val primaryAction: ResultAction,
    val secondaryActions: List<ResultAction> = emptyList(),
    val icon: ResultIcon = ResultIcon.Glyph(type),
    /** Shown with a work badge (apps from a managed profile). */
    val isWorkProfile: Boolean = false,
    /** Suspended apps or apps in a paused work profile get a ranking penalty. */
    val isDisabled: Boolean = false,
)

@Serializable
sealed interface ResultIcon {
    @Serializable
    data class App(val packageName: String, val activityName: String, val userSerial: Long) : ResultIcon

    @Serializable
    data class ContactPhoto(val photoUri: String?, val initials: String) : ResultIcon

    @Serializable
    data class FileThumbnail(val uri: String, val mimeType: String?) : ResultIcon

    @Serializable
    data class Glyph(val type: SourceType) : ResultIcon

    /** A short symbol drawn in a gradient tile, e.g. "⇄" for conversions or "₹" for currency. */
    @Serializable
    data class Symbol(val text: String) : ResultIcon
}
