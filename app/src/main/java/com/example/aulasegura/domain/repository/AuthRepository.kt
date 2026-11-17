package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/**
 * Authentication contract.
 *
 * Implementations live in `data/`; the UI only ever sees this interface and the
 * domain models it returns.
 */
interface AuthRepository {

    /** `uid` of the persisted session, or `null` when nobody is signed in. */
    val currentUserId: String?

    /**
     * Profile of the open session, as resolved by the last sign-in or restore.
     *
     * The rest of the app needs more than a uid — `schoolId` to build queries,
     * `subjects` to label a group, `fullName` to greet the teacher. Re-reading
     * `users/{uid}` in every screen would duplicate the resolution logic and add
     * a document read per screen; this is the same profile, cached for the life
     * of the session and cleared on sign-out.
     */
    val currentUser: AppUser?

    /** Signs in and resolves the role from `users/{uid}`. */
    suspend fun signIn(email: String, password: String): AuthResult

    /** Resolves the persisted session, if any. Used by Splash. */
    suspend fun restoreSession(): AuthResult

    /**
     * Watches `users/{uid}.active` while a session is open.
     *
     * Emits only on an explicit document value; a read failure emits nothing so
     * a flaky network never signs anybody out by mistake.
     */
    fun observeAccountActive(): Flow<Boolean>

    /**
     * Sends the password reset email.
     *
     * Firebase deliberately does not reveal whether an address is registered, so
     * an unknown account reports [PasswordResetResult.Sent] as well: the caller
     * must never branch on account existence.
     */
    suspend fun sendPasswordReset(email: String): PasswordResetResult

    /**
     * Stores the avatar coordinates in `users/{uid}` and refreshes the cached
     * profile, so the new picture survives a restart without another network read.
     *
     * No network call belongs here: compression and the Cloudinary upload happen
     * in the caller. This method only writes the two fields the profile owns.
     */
    suspend fun updateProfilePhoto(
        photoUrl: String,
        photoPublicId: String,
    ): ProfilePhotoResult

    fun signOut()
}

/** Outcome of an avatar write, kept typed so the UI can explain a rejection. */
sealed interface ProfilePhotoResult {

    /** Firestore accepted it and the in-memory and cached profiles were updated. */
    data object Saved : ProfilePhotoResult

    /** The session ended while the picture was being uploaded. */
    data object SignedOut : ProfilePhotoResult

    /** The rules refused the write — e.g. the account was deactivated meanwhile. */
    data object Rejected : ProfilePhotoResult

    /** Transport failure; the picture is in Cloudinary but the profile still points to the old one. */
    data object Failed : ProfilePhotoResult
}

sealed interface PasswordResetResult {

    data object Sent : PasswordResetResult

    data object InvalidEmail : PasswordResetResult

    data object NetworkError : PasswordResetResult

    data object Unknown : PasswordResetResult
}

sealed interface AuthResult {

    data class Success(val user: AppUser) : AuthResult

    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure {
    INVALID_CREDENTIALS,
    USER_DISABLED,
    NO_SESSION,
    PROFILE_MISSING,
    PROFILE_INACTIVE,
    ROLE_UNKNOWN,
    NETWORK,
    UNKNOWN,
}
