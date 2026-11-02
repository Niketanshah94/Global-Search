package com.quickfind.feature.settings

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Deep links into system screens, with fallbacks for OEMs that rename or hide them. */
object SystemLinks {
    /** Class name of the Glance widget receiver in :app (feature modules cannot import it). */
    private const val WIDGET_RECEIVER = "com.quickfind.app.widget.SearchWidgetReceiver"

    fun canPinWidget(context: Context): Boolean =
        context.getSystemService(AppWidgetManager::class.java)?.isRequestPinAppWidgetSupported == true

    /** Asks the launcher to add the search widget (onboarding step 1). */
    fun requestPinWidget(context: Context): Boolean {
        val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context.packageName, WIDGET_RECEIVER), null, null)
    }

    fun isDefaultAssistant(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val roles = context.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT) && roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)
    }

    /**
     * The "Digital assistant app" picker. Pixel/AOSP: VOICE_INPUT_SETTINGS lands on it.
     * Samsung One UI and Xiaomi HyperOS put it under Default apps, hence the fallbacks.
     */
    fun openAssistantSettings(activity: Activity) {
        open(
            activity,
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    /** Tier B: MANAGE_EXTERNAL_STORAGE is granted on a system screen, not with a dialog. */
    fun openAllFilesAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT < 30) return
        open(
            activity,
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${activity.packageName}")),
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        )
    }

    fun hasNotificationAccess(context: Context): Boolean =
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Notification access is granted on a system screen; API 30+ can open our own entry directly. */
    fun openNotificationAccess(activity: Activity) {
        val component = ComponentName(activity.packageName, NOTIFICATION_LISTENER)
        val detail = if (Build.VERSION.SDK_INT >= 30) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            null
        }
        open(activity, *listOfNotNull(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)).toTypedArray())
    }

    private const val NOTIFICATION_LISTENER = "com.quickfind.core.sources.notifications.QuickFindNotificationListener"

    fun openAppDetails(activity: Activity) {
        open(activity, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))
    }

    private fun open(activity: Activity, vararg intents: Intent) {
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next screen.
            } catch (_: SecurityException) {
                // Some OEM settings activities are not exported to third parties.
            }
        }
    }
}
