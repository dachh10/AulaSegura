package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

/**
 * The teacher's notification tray.
 *
 * Read-only except for `read`: the rules allow the recipient to flip that one
 * field and nothing else, because a client that could create its own
 * notifications could fake an announcement from the school.
 */
interface NotificationRepository {

    /** Newest first, live: a notification that arrives while the screen is open shows up. */
    fun observeMyNotifications(): Flow<List<AppNotification>>

    /** Flips `read` on one document; the row stops counting as unread. */
    suspend fun markRead(notificationId: String)

    /** Marks every unread notification of this user as read. */
    suspend fun markAllRead()
}
