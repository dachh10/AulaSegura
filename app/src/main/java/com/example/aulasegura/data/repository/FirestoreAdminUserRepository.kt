package com.example.aulasegura.data.repository

import android.content.Context
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountCreationError
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateError
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.CreateAccountResult
import com.example.aulasegura.domain.repository.orphanedAfterCleanup
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * The principal's user list, straight from `users` with one school filter.
 *
 * Decision: the query filters by `schoolId` only and Docentes/Padres are split in
 * memory by the pure UI helper. One read serves both tabs, so switching tabs costs
 * nothing and the single equality filter is exactly what the security rule needs.
 *
 * The two profile writes use `DocumentReference.update(map)` with a map built from
 * the editable [AdminUserFields] plus a server `updatedAt`; identity fields cannot
 * travel in those maps, which is what `changedKeys().hasOnly(...)` checks. Account
 * creation is different: it is a `set()` of the full create-rule shape, written
 * while still authenticated as the principal through the primary app.
 */
class FirestoreAdminUserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val context: Context,
) : AdminUserRepository {

    override suspend fun getUsers(): List<AdminUser> {
        val schoolId = auth.currentUser?.schoolId ?: return emptyList()

        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(COLLECTION)
                .whereEqualTo(AdminUserFields.SCHOOL_ID, schoolId)
                .get(Source.SERVER)
                .await()
        }
        return snapshot.documents.mapNotNull { it.toAdminUser() }
    }

    override suspend fun getSelf(): AdminUser? {
        val uid = auth.currentUserId ?: return null

        val snapshot = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION).document(uid).get(Source.SERVER).await()
            }
        } catch (error: Exception) {
            null
        }
        if (snapshot != null && snapshot.exists()) return snapshot.toAdminUser()

        // Offline or a transient read failure: the cached session still knows the
        // name, email, school, role and last resolved photo. The profile stays
        // honest instead of flashing empty fields; a later refresh fills the rest.
        return auth.currentUser?.toAdminUser()
    }

    override suspend fun updateProfile(
        uid: String,
        changes: Map<String, Any>,
    ): AdminUserUpdateResult {
        // Defence in depth: even if a caller hands us an identity field, it is
        // dropped here. The rules would reject the write anyway; dropping it keeps
        // the client from ever putting immutable data on the wire.
        val payload = changes.filterKeys { it in WRITABLE_KEYS }.toMutableMap()
        payload[AdminUserFields.UPDATED_AT] = FieldValue.serverTimestamp()
        return applyUpdate(uid, payload)
    }

    override suspend fun updatePhoto(
        uid: String,
        photoUrl: String,
        photoPublicId: String,
    ): AdminUserUpdateResult {
        // Exactly the three keys the "own profile" rule allows. No schoolId, no
        // email, no role: a photo change must never look like an identity change.
        val payload = mapOf<String, Any>(
            AdminUserFields.PHOTO_URL to photoUrl,
            AdminUserFields.PHOTO_PUBLIC_ID to photoPublicId,
            AdminUserFields.UPDATED_AT to FieldValue.serverTimestamp(),
        )
        return applyUpdate(uid, payload)
    }

    /**
     * Creates the Auth user through a secondary FirebaseApp, then the matching
     * Firestore document through the primary one (still authenticated as the
     * principal, so the rule's `isDirectivo()` clause holds).
     *
     * The secondary app is reused, never removed: removing it would also drop its
     * session bookkeeping and force a new registration on every account. Only its
     * session is cleared, in `finally`, so the next creation starts as nobody.
     */
    override suspend fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: AccountRole,
        phone: String,
        subjects: List<String>,
        groupIds: List<String>,
    ): CreateAccountResult {
        val principal = auth.currentUser
            ?: return CreateAccountResult.Failure(AccountCreationError.NO_SESSION)
        if (principal.role != UserRole.ADMIN) {
            return CreateAccountResult.Failure(AccountCreationError.DENIED)
        }

        val secondaryAuth = try {
            secondaryAuth()
        } catch (error: Exception) {
            return CreateAccountResult.Failure(AccountCreationError.UNKNOWN)
        }

        var createdUid: String? = null
        return try {
            val created = withTimeout(WRITE_TIMEOUT_MILLIS) {
                secondaryAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            }
            val uid = created.user?.uid
                ?: return CreateAccountResult.Failure(AccountCreationError.UNKNOWN)
            createdUid = uid

            val document = AdminUserFields.newAccountDocument(
                uid = uid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role,
                phone = phone,
                subjects = subjects,
                groupIds = groupIds,
                schoolId = principal.schoolId,
            ).toMutableMap()
            document[AdminUserFields.CREATED_AT] = FieldValue.serverTimestamp()
            document[AdminUserFields.UPDATED_AT] = FieldValue.serverTimestamp()

            // set(), not merge: the rule checks this exact create shape.
            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION).document(uid).set(document).await()
            }
            CreateAccountResult.Success(uid)
        } catch (error: TimeoutCancellationException) {
            failWithCleanup(secondaryAuth, createdUid, AccountCreationError.TIMEOUT)
        } catch (error: FirebaseAuthException) {
            failWithCleanup(secondaryAuth, createdUid, mapAuthError(error))
        } catch (error: FirebaseFirestoreException) {
            failWithCleanup(secondaryAuth, createdUid, mapFirestoreError(error))
        } catch (error: Exception) {
            failWithCleanup(secondaryAuth, createdUid, AccountCreationError.UNKNOWN)
        } finally {
            // Only the secondary session is cleared; the app instance is reused.
            secondaryAuth.signOut()
        }
    }

    private fun secondaryAuth(): FirebaseAuth {
        val primary = FirebaseApp.getInstance()
        val secondary = runCatching { FirebaseApp.getInstance(SECONDARY_APP_NAME) }.getOrNull()
            ?: FirebaseApp.initializeApp(
                primary.applicationContext,
                primary.options,
                SECONDARY_APP_NAME,
            )
        return FirebaseAuth.getInstance(secondary)
    }

    /**
     * Best effort: a failed Firestore write must not leave a login with no profile.
     * `orphaned = true` reports the opposite case honestly when even this fails.
     */
    private suspend fun failWithCleanup(
        secondaryAuth: FirebaseAuth,
        createdUid: String?,
        reason: AccountCreationError,
    ): CreateAccountResult {
        if (createdUid == null) return CreateAccountResult.Failure(reason)
        // Firebase's delete().await() returns null on SUCCESS, so the result value
        // cannot be used as the outcome. Capture whether there was a user first:
        // no user to delete means cleanup cannot be confirmed and must be reported.
        val hadUser = secondaryAuth.currentUser != null
        val cleanup = runCatching { secondaryAuth.currentUser?.delete()?.await() }
        val orphaned = orphanedAfterCleanup(hadUser, cleanupSucceeded = cleanup.isSuccess)
        return CreateAccountResult.Failure(reason, orphaned = orphaned)
    }

    private fun mapAuthError(error: FirebaseAuthException): AccountCreationError =
        when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> AccountCreationError.EMAIL_IN_USE
            "ERROR_WEAK_PASSWORD" -> AccountCreationError.WEAK_PASSWORD
            "ERROR_INVALID_EMAIL" -> AccountCreationError.INVALID_EMAIL
            "ERROR_NETWORK_REQUEST_FAILED" -> AccountCreationError.NETWORK
            else -> AccountCreationError.UNKNOWN
        }

    private fun mapFirestoreError(error: FirebaseFirestoreException): AccountCreationError =
        when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            -> AccountCreationError.DENIED

            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> AccountCreationError.NETWORK

            else -> AccountCreationError.UNKNOWN
        }

    /** Single write path, so both profile writes classify Firestore failures alike. */
    private suspend fun applyUpdate(
        uid: String,
        payload: Map<String, Any>,
    ): AdminUserUpdateResult {
        if (auth.currentUser == null) {
            return AdminUserUpdateResult.Failure(AdminUserUpdateError.NO_SESSION)
        }

        return try {
            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION).document(uid).update(payload).await()
            }
            AdminUserUpdateResult.Success
        } catch (error: TimeoutCancellationException) {
            AdminUserUpdateResult.Failure(AdminUserUpdateError.TIMEOUT)
        } catch (error: FirebaseFirestoreException) {
            val reason = when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
                -> AdminUserUpdateError.DENIED

                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> AdminUserUpdateError.NETWORK

                else -> AdminUserUpdateError.UNKNOWN
            }
            AdminUserUpdateResult.Failure(reason)
        } catch (error: Exception) {
            AdminUserUpdateResult.Failure(AdminUserUpdateError.UNKNOWN)
        }
    }

    /** The one mapper from a user document to the domain model. */
    private fun DocumentSnapshot.toAdminUser(): AdminUser = AdminUser(
        uid = id,
        email = getString(EMAIL).orEmpty(),
        role = AdminUser.roleFromFirestore(getString(ROLE)),
        firstName = getString(AdminUserFields.FIRST_NAME).orEmpty(),
        lastName = getString(AdminUserFields.LAST_NAME).orEmpty(),
        fullName = getString(AdminUserFields.FULL_NAME).orEmpty(),
        phone = getString(AdminUserFields.PHONE).orEmpty(),
        schoolId = getString(AdminUserFields.SCHOOL_ID).orEmpty(),
        subjects = stringList(AdminUserFields.SUBJECTS),
        groupIds = stringList(AdminUserFields.GROUP_IDS),
        studentIds = stringList(AdminUserFields.STUDENT_IDS),
        active = getBoolean(ACTIVE) ?: false,
        photoUrl = AdminUser.photoField(getString(AdminUserFields.PHOTO_URL)),
        photoPublicId = AdminUser.photoField(getString(AdminUserFields.PHOTO_PUBLIC_ID)),
        notificationsEnabled = AdminUserFields.notificationsEnabledFrom(
            get(AdminUserFields.PREFERENCES),
        ),
    )

    /** Cached-session fallback: only fields the session actually carries are set. */
    private fun AppUser.toAdminUser(): AdminUser = AdminUser(
        uid = uid,
        email = email,
        role = role,
        firstName = "",
        lastName = "",
        fullName = fullName,
        phone = "",
        schoolId = schoolId,
        subjects = subjects,
        groupIds = emptyList(),
        studentIds = emptyList(),
        active = active,
        photoUrl = AdminUser.photoField(photoUrl),
    )

    private fun DocumentSnapshot.stringList(field: String): List<String> =
        (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val COLLECTION = "users"
        const val SECONDARY_APP_NAME = "admin_account_creation"

        const val EMAIL = "email"
        const val ROLE = "role"
        const val ACTIVE = "active"

        const val READ_TIMEOUT_MILLIS = 12_000L
        const val WRITE_TIMEOUT_MILLIS = 12_000L

        val WRITABLE_KEYS = setOf(
            AdminUserFields.FIRST_NAME,
            AdminUserFields.LAST_NAME,
            AdminUserFields.FULL_NAME,
            AdminUserFields.PHONE,
            AdminUserFields.SUBJECTS,
            AdminUserFields.GROUP_IDS,
            AdminUserFields.STUDENT_IDS,
            AdminUserFields.PREFERENCES,
            AdminUserFields.ACTIVE,
        )
    }
}
