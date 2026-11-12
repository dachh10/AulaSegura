package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of [AuthRepository].
 *
 * The role is never trusted from the client side alone: it is read from
 * `users/{uid}` and, on the server, the security rules validate it again on
 * every operation.
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override suspend fun signIn(email: String, password: String): AuthResult =
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            resolveProfile()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Failure(AuthFailure.USER_DISABLED)
        } catch (e: FirebaseNetworkException) {
            AuthResult.Failure(AuthFailure.NETWORK)
        } catch (e: Exception) {
            signOut()
            AuthResult.Failure(AuthFailure.UNKNOWN)
        }

    override suspend fun restoreSession(): AuthResult {
        if (auth.currentUser == null) return AuthResult.Failure(AuthFailure.NO_SESSION)
        return resolveProfile()
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String): PasswordResetResult =
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            PasswordResetResult.Sent
        } catch (e: FirebaseAuthInvalidUserException) {
            // Anti-enumeration: an address without an account must look exactly
            // like a successful send, or the screen becomes an account oracle.
            PasswordResetResult.Sent
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Malformed address. The SDK reports both a bad format and a rejected
            // credential with this type; a reset email can only hit the former.
            PasswordResetResult.InvalidEmail
        } catch (e: FirebaseNetworkException) {
            PasswordResetResult.NetworkError
        } catch (e: Exception) {
            PasswordResetResult.Unknown
        }

    override fun observeAccountActive(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                // A failed read means "unknown", not "deactivated". Staying silent
                // is the only safe behaviour: never sign out on a network hiccup.
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }
                trySend(snapshot.getBoolean(FIELD_ACTIVE) ?: true)
            }

        awaitClose { registration.remove() }
    }

    private suspend fun resolveProfile(): AuthResult {
        val firebaseUser = auth.currentUser
            ?: return AuthResult.Failure(AuthFailure.NO_SESSION)

        val snapshot = try {
            firestore.collection(USERS).document(firebaseUser.uid).get().await()
        } catch (e: FirebaseNetworkException) {
            return AuthResult.Failure(AuthFailure.NETWORK)
        } catch (e: FirebaseFirestoreException) {
            return AuthResult.Failure(AuthFailure.NETWORK)
        }

        if (!snapshot.exists()) {
            signOut()
            return AuthResult.Failure(AuthFailure.PROFILE_MISSING)
        }

        val active = snapshot.getBoolean(FIELD_ACTIVE) ?: true
        if (!active) {
            signOut()
            return AuthResult.Failure(AuthFailure.PROFILE_INACTIVE)
        }

        val role = UserRole.fromFirestore(snapshot.getString(FIELD_ROLE))
        if (role == UserRole.UNKNOWN) {
            signOut()
            return AuthResult.Failure(AuthFailure.ROLE_UNKNOWN)
        }

        return AuthResult.Success(
            AppUser(
                uid = firebaseUser.uid,
                email = snapshot.getString(FIELD_EMAIL) ?: firebaseUser.email.orEmpty(),
                role = role,
                fullName = snapshot.getString(FIELD_FULL_NAME).orEmpty(),
                schoolId = snapshot.getString(FIELD_SCHOOL_ID).orEmpty(),
                active = active,
                photoUrl = snapshot.getString(FIELD_PHOTO_URL),
            ),
        )
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_ACTIVE = "active"
        const val FIELD_PHOTO_URL = "photoUrl"
    }
}
