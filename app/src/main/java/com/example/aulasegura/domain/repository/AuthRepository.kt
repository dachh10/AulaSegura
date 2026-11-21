package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AppUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** The subset of `users/{uid}` that decides whether the tutor may enter. */
data class LinkingState(
    val vinculacion: String = "",
    val hijoSolicitado: String = "",
    val estado: String = "",
)

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

    /**
     * Creates a tutor account from a linking code. The three writes (profile,
     * student link, code redemption) are one atomic batch; on success the session
     * profile and its Room cache are already refreshed.
     */
    suspend fun registerTutor(input: TutorRegistrationInput): TutorRegistrationResult

    /**
     * New code-free registration. Creates the Auth account, sends the
     * verification email and writes `users/{uid}` with `rol`, `estado`,
     * `vinculacion` and `hijoSolicitado` or `fechaSolicitud` as appropriate.
     */
    suspend fun registerAccount(input: AccountRegistrationInput): AccountRegistrationResult =
        AccountRegistrationResult.Failed

    /** `true` when the current Auth user has confirmed their email. */
    suspend fun isEmailVerified(): Boolean = true

    /** Re-sends the verification link to the current Auth user. */
    suspend fun resendVerificationEmail(): Result<Unit> = Result.success(Unit)

    /** Re-reads `users/{uid}` so a pending approval or linking takes effect. */
    suspend fun refreshProfile(): AuthResult =
        AuthResult.Failure(AuthFailure.NO_SESSION)

    /** Resolves the persisted session, if any. Used by Splash. */
    suspend fun restoreSession(): AuthResult

    /**
     * Watches `users/{uid}.active` while a session is open.
     *
     * Emits only on an explicit document value; a read failure emits nothing so
     * a flaky network never signs anybody out by mistake.
     */
    fun observeAccountActive(): Flow<Boolean>

/** Live document listener: no refresh tap should be needed to be linked. */
fun observeLinkingState(): Flow<LinkingState> = emptyFlow()

/** Corrects the child's name before a rejected QR is generated again. */
suspend fun updateChildName(name: String): Result<Unit> = Result.success(Unit)

/**
 * Persists `emailVerified = true` on the signed-in user's document after the
 * owner confirms the verification email. The director's request queue reads
 * this flag to decide which teachers may be approved.
 */
suspend fun markEmailVerified(): Result<Unit> = Result.success(Unit)

/**
 * Director-only: decides a tutor's linking after scanning their QR.
 * The rules allow `vinculacion` only through the principal's whitelist.
 */
suspend fun setTutorLinking(uid: String, vinculacion: String): Result<Unit> =
    Result.success(Unit)

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

    /**
     * Edits the identity fields this app owns: name and subjects.
     *
     * Email, role, school and active are written by the director's flows and
     * rejected by the rules from a teacher device. As with the avatar, the
     * in-memory profile and its Room cache are refreshed here so the greeting
     * changes without a reload.
     */
    suspend fun updateProfile(
        fullName: String,
        subjects: List<String>,
    ): ProfileUpdateResult

    /**
     * Reauthenticates the open session with its current password, then sets a new
     * one. Reauthentication is what Firebase requires for a sensitive change; the
     * implementation never signs the user out, so the principal keeps their session.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult

    fun signOut()
}

/** Outcome of an identity edit. */
sealed interface ProfileUpdateResult {

    /** Firestore accepted it, and memory and cache were refreshed. */
    data object Saved : ProfileUpdateResult

    data object SignedOut : ProfileUpdateResult

    /** The rules refused it — e.g. the account was deactivated meanwhile. */
    data object Rejected : ProfileUpdateResult

    data object Failed : ProfileUpdateResult
}

/**
 * Password-change outcome, shared by the director's and the teacher's screens.
 *
 * Seven variants model what actually happened, which is what the teacher UI maps
 * to a message. [Failure] is the director's typed view of the same rejections:
 * every error variant carries the corresponding [PasswordChangeError] reason, so
 * an `is Failure` branch can read `reason` without losing the finer variant.
 * [Success] is the director's success marker and is simply the [Changed] value.
 */
sealed interface PasswordChangeResult {

    // Every variant carries Failure so `is Failure` is exhaustive for the
    // director's when; on the success path its reason is never read because the
    // Success branch is evaluated first.
    data object Changed :
        Failure(PasswordChangeError.UNKNOWN), PasswordChangeResult

    data object WrongPassword :
        Failure(PasswordChangeError.WRONG_PASSWORD), PasswordChangeResult

    data object WeakPassword :
        Failure(PasswordChangeError.WEAK_PASSWORD), PasswordChangeResult

    data object RequiresRecentLogin :
        Failure(PasswordChangeError.REQUIRES_RECENT_LOGIN), PasswordChangeResult

    data object SignedOut :
        Failure(PasswordChangeError.NO_SESSION), PasswordChangeResult

    data object NetworkError :
        Failure(PasswordChangeError.NETWORK), PasswordChangeResult

    data object Failed :
        Failure(PasswordChangeError.UNKNOWN), PasswordChangeResult

    /** Typed cause behind a rejection, as consumed by the director's profile screen. */
    open class Failure(val reason: PasswordChangeError)

    companion object {

        /** Success alias for callers that only need to know the write succeeded. */
        val Success: PasswordChangeResult = Changed
    }
}

enum class PasswordChangeError {
    NO_SESSION,
    WRONG_PASSWORD,
    WEAK_PASSWORD,
    REQUIRES_RECENT_LOGIN,
    NETWORK,
    UNKNOWN,
}

/**
 * Pure mapping from a Firebase Auth error code to the typed password-change error.
 * Keeping it here makes the classification testable without an Auth instance.
 */
fun passwordChangeErrorFromCode(code: String?): PasswordChangeError = when (code) {
    "ERROR_WRONG_PASSWORD",
    "ERROR_INVALID_CREDENTIAL",
    -> PasswordChangeError.WRONG_PASSWORD

    "ERROR_WEAK_PASSWORD" -> PasswordChangeError.WEAK_PASSWORD
    "ERROR_REQUIRES_RECENT_LOGIN" -> PasswordChangeError.REQUIRES_RECENT_LOGIN
    "ERROR_NETWORK_REQUEST_FAILED" -> PasswordChangeError.NETWORK
    else -> PasswordChangeError.UNKNOWN
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
