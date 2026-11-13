package com.quickfind.core.model

/** What a Pro upsell is about, so the paywall can say why it opened. */
enum class ProFeature {
    GENERAL,
    APPS,
    FILES,
    MESSAGING,
    OTP,
    QUICK_ANSWERS,
    MORE_RESULTS,
    PINS,
}

/**
 * The one place that decides what FindPal Free can do.
 *
 * Free: search contacts (and a typed phone number) and call them, or open the contact card.
 * Everything else (apps, files, settings, calendar, notifications, OTP, quick answers,
 * meaning search, WhatsApp/SMS/Telegram/Signal, email, maps, pins, nicknames) is Pro.
 */
object ProGate {
    val FREE_TYPES: Set<SourceType> = setOf(SourceType.CONTACT, SourceType.PHONE_NUMBER)

    fun isFree(type: SourceType): Boolean = type in FREE_TYPES

    fun isFree(action: ResultAction): Boolean =
        action is ResultAction.Call || action is ResultAction.OpenContact

    /** Which feature a locked action belongs to, for the paywall headline. */
    fun featureFor(action: ResultAction): ProFeature = when (action) {
        is ResultAction.Sms, is ResultAction.WhatsApp, is ResultAction.Telegram,
        is ResultAction.Signal, is ResultAction.Email -> ProFeature.MESSAGING
        is ResultAction.LaunchApp, is ResultAction.AppDetails, is ResultAction.Uninstall,
        is ResultAction.PlayStore -> ProFeature.APPS
        is ResultAction.OpenFile, is ResultAction.ShareFile, is ResultAction.OpenFolder -> ProFeature.FILES
        is ResultAction.SetTimer, is ResultAction.SetAlarm, is ResultAction.CopyText -> ProFeature.QUICK_ANSWERS
        else -> ProFeature.GENERAL
    }

    /** Which feature the locked results of [types] point at (apps first: the most common). */
    fun featureFor(types: Collection<SourceType>): ProFeature = when {
        SourceType.APP in types -> ProFeature.APPS
        SourceType.FILE in types || SourceType.FOLDER in types -> ProFeature.FILES
        SourceType.CALCULATOR in types || SourceType.TOOL in types -> ProFeature.QUICK_ANSWERS
        else -> ProFeature.MORE_RESULTS
    }
}
