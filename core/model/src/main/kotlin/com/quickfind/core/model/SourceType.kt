package com.quickfind.core.model

import kotlinx.serialization.Serializable

/** Every kind of thing a result can be. Order here has no meaning; see [ResultSection]. */
@Serializable
enum class SourceType {
    APP,
    CONTACT,
    FILE,
    FOLDER,
    SETTING,
    CALCULATOR,
    /** Instant tools: unit and currency conversion, timers, alarms, email and map shortcuts. */
    TOOL,
    PHONE_NUMBER,
    URL,
    WEB,
}

/** How a source produces results (spec 4.3). Drives dispatcher choice and debounce. */
enum class SourceKind {
    /** Loaded once into RAM and scanned per keystroke. Budget < 10 ms. */
    IN_MEMORY,

    /** Stored in Room FTS tables. Budget < 50 ms, ~80 ms debounce. */
    INDEXED,

    /** Computed from the query alone. Budget < 2 ms. */
    INSTANT,
}

/** Grouped sections on the results screen, in display order (spec 4.7). */
enum class ResultSection(val types: Set<SourceType>) {
    APPS(setOf(SourceType.APP)),
    CONTACTS(setOf(SourceType.CONTACT, SourceType.PHONE_NUMBER)),
    FILES(setOf(SourceType.FILE, SourceType.FOLDER)),
    SETTINGS(setOf(SourceType.SETTING)),
    WEB(setOf(SourceType.URL, SourceType.WEB, SourceType.CALCULATOR, SourceType.TOOL)),
    ;

    companion object {
        fun of(type: SourceType): ResultSection = entries.first { type in it.types }
    }
}
