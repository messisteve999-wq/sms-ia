package com.steve.smsia

import android.app.Notification
import android.app.RemoteInput
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * V1 observes eligible notifications only. It deliberately does not auto-send.
 * A future implementation may offer a reply action only when the source app exposes RemoteInput,
 * the user has explicitly allow-listed the sender, and the user enabled that feature.
 */
class MessageNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (text.isBlank()) return
        // Intentionally do not log or transmit message content. Notification permission is user-controlled.
        // Reply actions vary by app and may be absent; no automatic reply is sent in this starter build.
    }
}
