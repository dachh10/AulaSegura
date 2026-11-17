package com.example.aulasegura.data.repository

import android.util.Log
import com.example.aulasegura.BuildConfig
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.PushTokenRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * `users/{uid}/fcmTokens/{token}` = `{ token, platform, updatedAt }`.
 *
 * The token is the document id: asking twice writes the same document, and the
 * old device keeps its own. The rules only let the owner write this subcollection,
 * so a stolen uid cannot register somebody else's phone.
 */
class FirestorePushTokenRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val messaging: () -> FirebaseMessaging = { FirebaseMessaging.getInstance() },
) : PushTokenRepository {

    override suspend fun syncToken(): String? {
        val uid = auth.currentUser?.uid ?: return null
        val token = runCatching { messaging().token.await() }.getOrNull() ?: return null
        store(uid, token)
        return token
    }

    override suspend fun registerToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        if (token.isBlank()) return
        store(uid, token)
    }

    override suspend fun deleteCurrentToken() {
        val uid = auth.currentUser?.uid ?: return
        // A token that cannot be read cannot be deleted; the sign-out must not
        // hang on a network call, so this stays best-effort.
        val token = runCatching { messaging().token.await() }.getOrNull() ?: return
        runCatching {
            firestore.collection(USERS).document(uid)
                .collection(TOKENS).document(token)
                .delete()
                .await()
        }
    }

    private suspend fun store(uid: String, token: String) {
        // Printed on every registration, not only on rotation: the token is pasted
        // by hand into the console's test message, so it has to be findable.
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM token: $token")
        runCatching {
            firestore.collection(USERS).document(uid)
                .collection(TOKENS).document(token)
                .set(
                    mapOf(
                        FIELD_TOKEN to token,
                        FIELD_PLATFORM to PLATFORM_ANDROID,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
        }
    }

    private companion object {
        const val TAG = "FCM"
        const val USERS = "users"
        const val TOKENS = "fcmTokens"
        const val FIELD_TOKEN = "token"
        const val FIELD_PLATFORM = "platform"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val PLATFORM_ANDROID = "android"
    }
}
