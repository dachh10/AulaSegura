package com.example.aulasegura.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.aulasegura.MainActivity
import com.example.aulasegura.R

/**
 * Builds the notification and the intent that opens the right screen.
 *
 * One channel for the whole app for now: splitting into "asistencia", "mensajes"
 * and "avisos" is a product decision, and a channel cannot be deleted once
 * created. The id is stable because changing it would silently reset the user's
 * preferences.
 */
object NotificationPoster {

    const val CHANNEL_ID = "aulasegura_general"
    const val CHANNEL_NAME = "Avisos de AulaSegura"

    /** Extras the activity reads to navigate after a tap. */
    const val EXTRA_DEEP_LINK = "aulasegura_deep_link"
    const val EXTRA_NOTIFICATION_ID = "aulasegura_notification_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Pases de lista, incidentes, tareas, avisos y mensajes."
            setShowBadge(true)
        }

        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun post(
        context: Context,
        title: String,
        body: String,
        deepLink: String?,
        notificationId: String?,
    ) {
        ensureChannel(context)

        // Android 13+ can deny notifications; posting without the grant throws on
        // some OEMs, so the check is explicit instead of a catch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DEEP_LINK, deepLink)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId?.hashCode() ?: title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(notificationId?.hashCode() ?: title.hashCode(), notification)
        }
    }
}
