plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
}

android {
    // Brand: FindPal. The Kotlin namespace stays com.quickfind.* (internal only); the Play
    // Store ID is applicationId below.
    namespace = "com.quickfind.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.findpal.app"
        minSdk = 26
        targetSdk = 36
        // CI passes the run number so every Play upload gets a higher version code.
        versionCode = providers.environmentVariable("FINDPAL_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Play upload key, from CI secrets only (see docs/store/PLAY_CONSOLE.md). Never commit keys.
    // Without it, release builds are unsigned (fine for CI checks, not uploadable).
    val uploadKeystore = providers.environmentVariable("FINDPAL_UPLOAD_KEYSTORE").orNull
    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = providers.environmentVariable("FINDPAL_UPLOAD_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("FINDPAL_UPLOAD_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("FINDPAL_UPLOAD_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // The embedding model is memory-mapped by MediaPipe and must stay uncompressed.
    androidResources {
        noCompress += "tflite"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:sources"))
    implementation(project(":core:indexing"))
    implementation(project(":core:ui"))
    implementation(project(":core:billing"))
    implementation(project(":feature:search"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    baselineProfile(project(":benchmark"))
}
