package com.example.aulasegura.domain.repository

/**
 * This device's FCM registration token, stored under `users/{uid}/fcmTokens`.
 *
 * A subcollection and not an array (CONTEXT §6.2): two devices writing the same
 * array would overwrite each other's token and one of them would stop receiving
 * push. The document id is the token itself, so re-registering is idempotent.
 *
 * In the free plan nothing sends these automatically — the token is what a
 * person pastes into the Firebase console to send a test message, and what a
 * future paid setup would target.
 */
interface PushTokenRepository {

    /**
     * Fetches the current token and stores it. Called once the session is
     * resolved, because writing the subcollection needs the uid.
     */
    suspend fun syncToken(): String?

    /** Used by `onNewToken`: the token rotated and the old one is dead. */
    suspend fun registerToken(token: String)

    /**
     * Removes this device's token before signing out, so a shared phone does not
     * keep receiving the previous teacher's notifications.
     */
    suspend fun deleteCurrentToken()
}
