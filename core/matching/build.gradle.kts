import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin: normalizer, fuzzy matcher, ranking, calculator. Zero Android dependencies
// so the whole module unit-tests on the JVM in seconds.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api(project(":core:model"))
    testImplementation(libs.junit)
}
