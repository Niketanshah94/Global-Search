package com.quickfind.core.sources.settings

import android.os.Build
import androidx.annotation.StringRes
import com.quickfind.core.sources.R

/**
 * Curated system-settings shortcuts (spec 5.4). Android has no public API to search
 * settings, so we ship this list and hide entries whose intent does not resolve.
 *
 * Several actions below are not public SDK constants (e.g. TETHER_SETTINGS); OEMs differ,
 * which is exactly why every entry is verified with resolveActivity at startup.
 * Each action must also appear in the manifest `<queries>` block to be resolvable on API 30+.
 */
data class SettingShortcut(
    val action: String,
    @StringRes val title: Int,
    val keywords: List<String>,
    val minSdk: Int = Build.VERSION_CODES.O,
)

object SettingsCatalog {
    val entries: List<SettingShortcut> = listOf(
        SettingShortcut("android.settings.SETTINGS", R.string.setting_all, listOf("settings", "preferences", "system")),
        SettingShortcut("android.settings.WIFI_SETTINGS", R.string.setting_wifi, listOf("wifi", "wi-fi", "wireless", "internet", "network", "wlan")),
        SettingShortcut("android.settings.panel.action.INTERNET_CONNECTIVITY", R.string.setting_internet_panel, listOf("internet", "wifi", "mobile data", "connectivity"), Build.VERSION_CODES.Q),
        SettingShortcut("android.settings.BLUETOOTH_SETTINGS", R.string.setting_bluetooth, listOf("bluetooth", "bt", "pair", "headphones", "earbuds", "speaker")),
        SettingShortcut("android.settings.AIRPLANE_MODE_SETTINGS", R.string.setting_airplane, listOf("airplane", "flight", "aeroplane", "offline")),
        SettingShortcut("android.settings.DATA_ROAMING_SETTINGS", R.string.setting_mobile_network, listOf("mobile data", "cellular", "roaming", "sim", "4g", "5g", "lte")),
        SettingShortcut("android.settings.NETWORK_OPERATOR_SETTINGS", R.string.setting_network_operator, listOf("carrier", "operator", "network", "sim")),
        SettingShortcut("android.settings.DATA_USAGE_SETTINGS", R.string.setting_data_usage, listOf("data usage", "data saver", "mobile data", "limit"), Build.VERSION_CODES.P),
        SettingShortcut("android.settings.WIRELESS_SETTINGS", R.string.setting_connections, listOf("connections", "network", "wireless", "connected devices")),
        SettingShortcut("android.settings.TETHER_SETTINGS", R.string.setting_hotspot, listOf("hotspot", "tethering", "share internet", "portable")),
        SettingShortcut("android.settings.VPN_SETTINGS", R.string.setting_vpn, listOf("vpn", "private network")),
        SettingShortcut("android.settings.NFC_SETTINGS", R.string.setting_nfc, listOf("nfc", "tap", "contactless")),
        SettingShortcut("android.settings.NFC_PAYMENT_SETTINGS", R.string.setting_nfc_payment, listOf("tap to pay", "contactless", "payment", "wallet", "gpay")),
        SettingShortcut("android.settings.CAST_SETTINGS", R.string.setting_cast, listOf("cast", "screen mirroring", "chromecast", "smart view", "tv")),
        SettingShortcut("android.settings.DISPLAY_SETTINGS", R.string.setting_display, listOf("display", "screen", "brightness", "font size", "timeout")),
        SettingShortcut("android.settings.NIGHT_DISPLAY_SETTINGS", R.string.setting_night_light, listOf("night light", "blue light", "eye comfort", "reading mode")),
        SettingShortcut("android.settings.DARK_THEME_SETTINGS", R.string.setting_dark_theme, listOf("dark mode", "dark theme", "night mode"), Build.VERSION_CODES.Q),
        SettingShortcut("android.settings.AUTO_ROTATE_SETTINGS", R.string.setting_auto_rotate, listOf("rotate", "rotation", "orientation", "landscape")),
        SettingShortcut("android.settings.DREAM_SETTINGS", R.string.setting_screen_saver, listOf("screen saver", "daydream")),
        SettingShortcut("android.settings.SOUND_SETTINGS", R.string.setting_sound, listOf("sound", "volume", "ringtone", "vibration", "silent")),
        SettingShortcut("android.settings.panel.action.VOLUME", R.string.setting_volume_panel, listOf("volume", "media volume", "ring volume"), Build.VERSION_CODES.Q),
        SettingShortcut("android.settings.ZEN_MODE_PRIORITY_SETTINGS", R.string.setting_dnd, listOf("do not disturb", "dnd", "silent", "focus", "quiet")),
        SettingShortcut("android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS", R.string.setting_dnd_access, listOf("do not disturb access", "dnd access")),
        SettingShortcut("android.settings.ALL_APPS_NOTIFICATION_SETTINGS", R.string.setting_notifications, listOf("notifications", "alerts", "badges")),
        SettingShortcut("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", R.string.setting_notification_access, listOf("notification access", "notification listener")),
        SettingShortcut("android.intent.action.POWER_USAGE_SUMMARY", R.string.setting_battery, listOf("battery", "power", "usage", "charge")),
        SettingShortcut("android.settings.BATTERY_SAVER_SETTINGS", R.string.setting_battery_saver, listOf("battery saver", "power saving", "low power")),
        SettingShortcut("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS", R.string.setting_battery_optimization, listOf("battery optimization", "doze", "background", "restrict")),
        SettingShortcut("android.settings.APPLICATION_SETTINGS", R.string.setting_apps, listOf("apps", "applications", "installed apps")),
        SettingShortcut("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS", R.string.setting_all_apps, listOf("all apps", "app info", "force stop", "clear cache")),
        SettingShortcut("android.settings.MANAGE_DEFAULT_APPS_SETTINGS", R.string.setting_default_apps, listOf("default apps", "browser", "launcher", "home app", "sms app")),
        SettingShortcut("android.settings.VOICE_INPUT_SETTINGS", R.string.setting_assistant, listOf("assistant", "digital assistant", "voice input", "assist app")),
        SettingShortcut("android.settings.HOME_SETTINGS", R.string.setting_home_app, listOf("home app", "launcher", "home screen")),
        SettingShortcut("android.settings.action.MANAGE_OVERLAY_PERMISSION", R.string.setting_overlay, listOf("display over other apps", "overlay", "draw over")),
        SettingShortcut("android.settings.action.MANAGE_WRITE_SETTINGS", R.string.setting_modify_system, listOf("modify system settings", "write settings")),
        SettingShortcut("android.settings.MANAGE_UNKNOWN_APP_SOURCES", R.string.setting_unknown_sources, listOf("install unknown apps", "unknown sources", "apk", "sideload")),
        SettingShortcut("android.settings.USAGE_ACCESS_SETTINGS", R.string.setting_usage_access, listOf("usage access", "usage stats")),
        SettingShortcut("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", R.string.setting_all_files_access, listOf("all files access", "manage storage", "file permission"), Build.VERSION_CODES.R),
        SettingShortcut("android.settings.INTERNAL_STORAGE_SETTINGS", R.string.setting_storage, listOf("storage", "space", "memory", "free up", "disk")),
        SettingShortcut("android.settings.MEMORY_CARD_SETTINGS", R.string.setting_sd_card, listOf("sd card", "memory card", "external storage")),
        SettingShortcut("android.settings.LOCATION_SOURCE_SETTINGS", R.string.setting_location, listOf("location", "gps", "position")),
        SettingShortcut("android.settings.SECURITY_SETTINGS", R.string.setting_security, listOf("security", "screen lock", "pin", "password", "pattern", "lock screen")),
        SettingShortcut("android.settings.BIOMETRIC_ENROLL", R.string.setting_biometrics, listOf("fingerprint", "face unlock", "biometrics"), Build.VERSION_CODES.R),
        SettingShortcut("android.settings.FINGERPRINT_ENROLL", R.string.setting_fingerprint, listOf("fingerprint", "biometrics"), Build.VERSION_CODES.P),
        SettingShortcut("android.settings.PRIVACY_SETTINGS", R.string.setting_privacy, listOf("privacy", "permissions", "permission manager")),
        SettingShortcut("android.settings.ACCESSIBILITY_SETTINGS", R.string.setting_accessibility, listOf("accessibility", "talkback", "magnification", "a11y")),
        SettingShortcut("android.settings.CAPTIONING_SETTINGS", R.string.setting_captions, listOf("captions", "subtitles", "live caption")),
        SettingShortcut("android.settings.LOCALE_SETTINGS", R.string.setting_language, listOf("language", "locale", "region")),
        SettingShortcut("android.settings.INPUT_METHOD_SETTINGS", R.string.setting_keyboard, listOf("keyboard", "input method", "gboard", "typing")),
        SettingShortcut("android.settings.HARD_KEYBOARD_SETTINGS", R.string.setting_physical_keyboard, listOf("physical keyboard", "hardware keyboard")),
        SettingShortcut("android.settings.USER_DICTIONARY_SETTINGS", R.string.setting_dictionary, listOf("dictionary", "personal dictionary", "words")),
        SettingShortcut("android.settings.DATE_SETTINGS", R.string.setting_date_time, listOf("date", "time", "time zone", "clock", "24 hour")),
        SettingShortcut("android.settings.SYNC_SETTINGS", R.string.setting_accounts, listOf("accounts", "sync", "google account", "backup")),
        SettingShortcut("android.settings.DEVICE_INFO_SETTINGS", R.string.setting_about_phone, listOf("about phone", "device info", "imei", "model", "android version", "build number")),
        SettingShortcut("android.settings.APPLICATION_DEVELOPMENT_SETTINGS", R.string.setting_developer, listOf("developer options", "usb debugging", "adb")),
        SettingShortcut("android.settings.PRINT_SETTINGS", R.string.setting_printing, listOf("print", "printer", "printing")),
        SettingShortcut("android.settings.WEBVIEW_SETTINGS", R.string.setting_webview, listOf("webview", "web view implementation")),
        SettingShortcut("android.settings.SHOW_REGULATORY_INFO", R.string.setting_regulatory, listOf("regulatory", "certification", "labels")),
        SettingShortcut("android.settings.panel.action.NFC", R.string.setting_nfc_panel, listOf("nfc", "tap"), Build.VERSION_CODES.Q),
    )
}
