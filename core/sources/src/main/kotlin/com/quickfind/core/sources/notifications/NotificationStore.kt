package com.quickfind.core.sources.notifications

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.quickfind.core.data.db.NotificationDao
import com.quickfind.core.data.db.NotificationEntity
import com.quickfind.core.data.di.ApplicationScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.OtpDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class Otp(val code: String, val source: String, val postedAt: Long)

/**
 * Keeps notification history for search (spec 10.7): title, text, app and time for 7 days,
 * on the phone only. Also remembers the latest one-time code for the "Copy OTP" chip.
 */
@Singleton
class NotificationStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NotificationDao,
    settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @Volatile private var enabled = false
    @Volatile var latestOtp: Otp? = null
        private set
    /** Tap targets of notifications still on screen; they cannot be stored across restarts. */
    private val live = ConcurrentHashMap<String, PendingIntent>()
    private var inserts = 0

    init {
        scope.launch {
            // Stored values only: a listener started at boot must not act on default settings.
            settings.loaded.collect { s ->
                enabled = s.notificationsEnabled
                if (!s.notificationsEnabled) {
                    live.clear()
                    latestOtp = null
                }
            }
        }
    }

    fun isAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun onPosted(sbn: StatusBarNotification) {
        if (!enabled || sbn.packageName == context.packageName) return
        val n = sbn.notification ?: return
        if (sbn.isOngoing || (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty().trim()
        if (title.isEmpty() && text.isEmpty()) return
        n.contentIntent?.let { live[sbn.key] = it }
        val label = appLabel(sbn.packageName)
        OtpDetector.find("$title $text")?.let { latestOtp = Otp(it, label, sbn.postTime) }
        val entity = NotificationEntity(
            key = "${sbn.key}|${(title + text).hashCode()}",
            sbnKey = sbn.key,
            packageName = sbn.packageName,
            appLabel = label,
            title = title,
            text = text.take(MAX_TEXT),
            postedAt = sbn.postTime,
        )
        scope.launch {
            dao.insert(entity)
            if (++inserts % 50 == 1) dao.prune(System.currentTimeMillis() - RETENTION_MS)
        }
    }

    fun onRemoved(key: String) {
        live.remove(key)
    }

    fun onDisconnected() = live.clear()

    /** Fires the notification's own tap target if it is still showing. */
    fun openLive(activityContext: Context, key: String): Boolean {
        val pending = live[key] ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                // Android 14+: let the target start an activity on our behalf (we are in front).
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pending.send(activityContext, 0, null, null, null, null, options.toBundle())
            } else {
                pending.send()
            }
            true
        } catch (_: PendingIntent.CanceledException) {
            live.remove(key)
            false
        }
    }

    suspend fun clear() {
        live.clear()
        latestOtp = null
        dao.clear()
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    companion object {
        const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        private const val MAX_TEXT = 1_000
    }
}
