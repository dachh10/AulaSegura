package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.ProfileDao
import com.example.aulasegura.data.local.mapper.toCachedEntity
import com.example.aulasegura.data.local.mapper.toCachedUser
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
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
    private val profileDao: ProfileDao,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Cached so `schoolId`, `subjects` and `fullName` are available without a
     * second `users/{uid}` read per screen. Written only by [resolveProfile] —
     * from the network or, offline, from the Room copy — and cleared on sign-out.
     */
    @Volatile
    override var currentUser: AppUser? = null
        private set

    override suspend fun signIn(email: String, password: String): AuthResult =
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            resolveProfile().also { result ->
                // Only a real sign-in is logged; restoreSession() on startup is
                // the same user coming back, not a login.
                if (result is AuthResult.Success) {
                    analytics.log(AnalyticsEvent.LoginSuccess(result.user.role))
                }
            }
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
        currentUser = null
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
            // No network: the last profile read on this device is still the truth
            // about who is signed in. Without this branch a teacher who opens the
            // app inside a school with no signal lands on the login screen.
            return cachedProfile(firebaseUser.uid)
        } catch (e: FirebaseFirestoreException) {
            return cachedProfile(firebaseUser.uid)
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

        val user = AppUser(
            uid = firebaseUser.uid,
            email = snapshot.getString(FIELD_EMAIL) ?: firebaseUser.email.orEmpty(),
            role = role,
            fullName = snapshot.getString(FIELD_FULL_NAME).orEmpty(),
            schoolId = snapshot.getString(FIELD_SCHOOL_ID).orEmpty(),
            active = active,
            photoUrl = snapshot.getString(FIELD_PHOTO_URL),
            subjects = (snapshot.get(FIELD_SUBJECTS) as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
        )

        // Cached only after the profile is fully valid: a rejected account must
        // not leave a usable user behind. The cache is replaced, never appended,
        // so a shared device keeps exactly one profile.
        runCatching {
            profileDao.clear()
            profileDao.upsert(user.toCachedEntity(System.currentTimeMillis()))
        }

        currentUser = user
        return AuthResult.Success(user)
    }

    /**
     * The offline path of [resolveProfile].
     *
     * Firestore's persistent cache is deliberately off, so the network failure
     * has to fall back to the Room copy of the profile. A deactivated account can
     * keep working offline until the next successful read — that is the price of
     * a usable offline app, and the safety net is [observeAccountActive], which
     * signs the session out as soon as a real value arrives.
     */
    private suspend fun cachedProfile(uid: String): AuthResult {
        val cached = runCatching { profileDao.findById(uid) }.getOrNull()
            ?.toCachedUser()
            ?: return AuthResult.Failure(AuthFailure.NETWORK)

        currentUser = cached
        return AuthResult.Success(cached)
    }

    private companion object {
        const val USERS = "users"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_ACTIVE = "active"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_SUBJECTS = "subjects"
    }
}
