package com.example.aulasegura.push

import android.util.Log
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.BuildConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the two things FCM sends: a new token and a message.
 *
 * The token is stored under `users/{uid}/fcmTokens` so it can be pasted into the
 * console *Send test message* dialog — in Spark there is no server to remember it.
 * It is also logged in debug builds, which is the documented way to copy it with
 * `adb logcat`.
 *
 * Data messages are turned into a local notification by [NotificationPoster].
 * Notification messages (with a `notification` payload) are shown by the SDK
 * itself when the app is in the background, and arrive here as data when the app
 * is in the foreground; posting them again would show two.
 */
class AulaSeguraMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM token: $token")

        val container = (applicationContext as AulaSeguraApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.pushTokenRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data[KEY_TITLE]
            ?: message.notification?.title
            ?: DEFAULT_TITLE
        val body = data[KEY_BODY]
            ?: message.notification?.body
            .orEmpty()

        NotificationPoster.post(
            context = this,
            title = title,
            body = body,
            deepLink = data[KEY_DEEP_LINK],
            notificationId = data[KEY_NOTIFICATION_ID],
        )
    }

    private companion object {
        const val TAG = "FCM"
        const val DEFAULT_TITLE = "AulaSegura"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_DEEP_LINK = "deepLink"
        const val KEY_NOTIFICATION_ID = "notificationId"
    }
}
