package com.quickfind.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val TARGET_PACKAGE = "com.quickfind.app"

/** First launch shows onboarding on top of search; skip it so journeys measure search. */
fun MacrobenchmarkScope.skipOnboardingIfShown() {
    device.wait(Until.findObject(By.text("Skip")), 1_000)?.click()
    device.waitForIdle()
}

/** Types "whats" one character at a time, the spec 16 typing-latency journey. */
fun MacrobenchmarkScope.typeWhats() {
    val field = device.wait(Until.findObject(By.res("search_field")), 5_000) ?: return
    val text = StringBuilder()
    for (c in "whats") {
        text.append(c)
        field.text = text.toString()
        device.waitForIdle()
    }
}
