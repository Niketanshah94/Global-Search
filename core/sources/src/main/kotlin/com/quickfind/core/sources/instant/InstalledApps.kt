package com.quickfind.core.sources.instant

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks whether specific apps are installed. Each package checked here must be listed in a
 * `<queries><package>` entry in the app manifest (package visibility, Android 11+).
 */
@Singleton
class InstalledApps @Inject constructor(@ApplicationContext private val context: Context) {
    private var cachedWhatsApp: String? = null
    private var cachedAt = 0L

    /** "com.whatsapp" or "com.whatsapp.w4b" (Business), cached for 30 s. */
    val whatsAppPackage: String?
        get() {
            val now = System.currentTimeMillis()
            if (now - cachedAt > 30_000) {
                cachedWhatsApp = WHATSAPP_PACKAGES.firstOrNull(::isInstalled)
                cachedAt = now
            }
            return cachedWhatsApp
        }

    fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
