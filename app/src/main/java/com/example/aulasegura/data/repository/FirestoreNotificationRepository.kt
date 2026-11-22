package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.NotificationRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * The signed-in user's tray, straight from `notifications` ordered by recency.
 *
 * No Room cache: a notification is a small document, the index
 * (`userId + createdAt desc`) already exists, and a tray that is stale by a day is
 * worse than a tray that needs network. If the query fails the screen says so
 * instead of inventing rows.
 */
class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : NotificationRepository {

    override fun observeMyNotifications(): Flow<List<AppNotification>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection(COLLECTION)
                .whereEqualTo(FIELD_USER_ID, uid)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(MAX_ROWS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Closing with the error would throw into every collector;
                        // on sign-out the watch is briefly rejected by the rules.
                        close()
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents.orEmpty().mapNotNull { it.toNotification() })
                }

            awaitClose { registration.remove() }
        }
    }

    override suspend fun markRead(notificationId: String) {
        if (auth.currentUser == null) return
        // The rules only accept this one field; a failure here is cosmetic.
        runCatching {
            firestore.collection(COLLECTION).document(notificationId)
                .update(FIELD_READ, true)
                .await()
        }
    }

    override suspend fun markAllRead() {
        if (auth.currentUser == null) return

        val unread = runCatching {
            firestore.collection(COLLECTION)
                .whereEqualTo(FIELD_USER_ID, auth.currentUser?.uid)
                .whereEqualTo(FIELD_READ, false)
                .get()
                .await()
        }.getOrNull()?.documents ?: return

        if (unread.isEmpty()) return

        val batch = firestore.batch()
        unread.forEach { document -> batch.update(document.reference, FIELD_READ, true) }
        runCatching { batch.commit().await() }
    }

    private fun DocumentSnapshot.toNotification(): AppNotification? {
        val title = getString(FIELD_TITLE).orEmpty()
        if (title.isBlank()) return null

        return AppNotification(
            id = id,
            title = title,
            body = getString(FIELD_BODY).orEmpty(),
            type = NotificationType.from(getString(FIELD_TYPE)),
            deepLink = getString(FIELD_DEEP_LINK),
            read = getBoolean(FIELD_READ) ?: false,
            createdAt = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val COLLECTION = "notifications"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TITLE = "title"
        const val FIELD_BODY = "body"
        const val FIELD_TYPE = "type"
        const val FIELD_DEEP_LINK = "deepLink"
        const val FIELD_READ = "read"
        const val FIELD_CREATED_AT = "createdAt"
        const val MAX_ROWS = 120L
    }
}
