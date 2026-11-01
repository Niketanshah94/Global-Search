package com.quickfind.feature.settings

/** Third-party libraries shipped in the APK (spec 14 "open-source licenses"). Keep in sync with libs.versions.toml. */
object OpenSourceLicenses {
    val entries: List<Pair<String, String>> = listOf(
        "AndroidX (Activity, Core, Compose, Glance, Lifecycle, Room, DataStore, WorkManager, Hilt extensions)" to "Apache License 2.0",
        "Dagger / Hilt" to "Apache License 2.0",
        "Kotlin standard library, kotlinx.coroutines, kotlinx.serialization" to "Apache License 2.0",
        "Coil" to "Apache License 2.0",
        "Material Design icons" to "Apache License 2.0",
    )
}
