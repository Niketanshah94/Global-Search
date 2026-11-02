package com.quickfind.core.sources.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Bound by the system once the user grants "Notification access". */
@AndroidEntryPoint
class QuickFindNotificationListener : NotificationListenerService() {
    @Inject lateinit var store: NotificationStore

    override fun onListenerConnected() {
        // Pick up what is already showing (an OTP that arrived before access was granted).
        runCatching { activeNotifications }.getOrNull()?.forEach(store::onPosted)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = store.onPosted(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) = store.onRemoved(sbn.key)

    override fun onListenerDisconnected() = store.onDisconnected()
}
