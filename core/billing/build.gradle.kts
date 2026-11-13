plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Play Console > Monetize > Monetization setup > "Licensing": the app's base64 RSA public key.
// Set it as the PLAY_LICENSE_KEY CI secret (or -PfindpalPlayLicenseKey=...) so purchases are
// checked against Google's signature on the phone. Empty: purchases are trusted as Play reports.
val playLicenseKey: String = providers.gradleProperty("findpalPlayLicenseKey")
    .orElse(providers.environmentVariable("PLAY_LICENSE_KEY"))
    .getOrElse("")

android {
    namespace = "com.quickfind.core.billing"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "PLAY_LICENSE_KEY", "\"${playLicenseKey.trim()}\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core:data"))
    api(libs.play.billing.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
