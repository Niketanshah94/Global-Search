package com.quickfind.core.model

import kotlinx.serialization.Serializable

/**
 * What happens when a result (or one of its secondary actions) is chosen.
 *
 * Pure data: the Android layer turns these into Intents, so this module stays
 * free of Android dependencies and results can be serialized (pins, snapshots).
 */
@Serializable
sealed interface ResultAction {
    @Serializable
    data class LaunchApp(val packageName: String, val activityName: String, val userSerial: Long) : ResultAction

    @Serializable
    data class AppDetails(val packageName: String, val userSerial: Long) : ResultAction

    @Serializable
    data class Uninstall(val packageName: String) : ResultAction

    @Serializable
    data class PlayStore(val packageName: String) : ResultAction

    /** Uses ACTION_DIAL unless the user enabled direct calling and granted CALL_PHONE. */
    @Serializable
    data class Call(val number: String) : ResultAction

    @Serializable
    data class Sms(val number: String) : ResultAction

    @Serializable
    data class WhatsApp(val number: String) : ResultAction

    @Serializable
    data class Email(val address: String) : ResultAction

    @Serializable
    data class OpenContact(val contactUri: String) : ResultAction

    @Serializable
    data class OpenFile(val uri: String, val mimeType: String?) : ResultAction

    @Serializable
    data class ShareFile(val uri: String, val mimeType: String?) : ResultAction

    /** Opens the in-app folder view (third-party file managers vary too much). */
    @Serializable
    data class OpenFolder(val relativePath: String, val title: String) : ResultAction

    @Serializable
    data class OpenSetting(val intentAction: String) : ResultAction

    @Serializable
    data class CopyText(val text: String) : ResultAction

    @Serializable
    data class OpenUrl(val url: String) : ResultAction
}
