package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.SubjectAvailability
import com.example.aulasegura.domain.model.subjectIdFromLabel
import com.example.aulasegura.domain.model.subjectAvailability
import com.example.aulasegura.domain.model.subjectById

import com.example.aulasegura.domain.model.AdminUser

/**
 * Read-only list of the school's users plus the two writes the principal makes:
 * editing an editable profile field of a user of their own school, and replacing
 * their own profile photo.
 *
 * [getUsers] and [getSelf] are deliberately one-shot reads, not listeners: the
 * screen loads once and refreshes after a write. Spark's daily read budget must
 * not be burned by a snapshot listener left open while the screen is on screen.
 */
interface AdminUserRepository {

    suspend fun getUsers(): List<AdminUser>

    /**
     * The signed-in user's own document, or null when there is no session.
     *
     * The profile screen needs fields the cached session does not carry (phone,
     * publicId), so it reads the document instead of inventing them.
     */
    suspend fun getSelf(): AdminUser?

    /**
     * Applies an already-built, whitelisted payload to `users/{uid}`.
     *
     * The payload must contain only [AdminUserFields] keys; the implementation
     * enforces that again at the data boundary before writing.
     */
    suspend fun updateProfile(uid: String, changes: Map<String, Any>): AdminUserUpdateResult

    /**
     * Replaces the signed-in user's photo metadata.
     *
     * The implementation writes exactly `photoUrl`, `photoPublicId` and
     * `updatedAt`. Identity fields are never part of this write, which is what
     * the rules' `changedKeys()` whitelist checks for the "own profile" branch.
     */
    suspend fun updatePhoto(
        uid: String,
        photoUrl: String,
        photoPublicId: String,
    ): AdminUserUpdateResult

    /**
     * Creates a docente or tutor account: the Auth user and its `users/{uid}`
     * document, in that order.
     *
     * The implementation signs the new user in through a SECONDARY FirebaseApp so
     * the principal's primary session is never replaced. [role] is deliberately a
     * type with only two values: creating another directivo is not representable
     * through this contract, which is also what the Firestore rule enforces.
     */
    /**
     * Director-only: approves or rejects a teacher by writing their `estado`.
     * The rules' principal whitelist allows this key; a user cannot change their
     * own state.
     */
    suspend fun setTeacherStatus(
        uid: String,
        estado: String,
    ): AdminUserUpdateResult

    /**
     * Approves every verified pending teacher in one Firestore batch. Returns
     * how many were decided so the UI can refresh once with a real number.
     */
    suspend fun acceptAllPendingTeachers(): BatchResult

    suspend fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: AccountRole,
        phone: String,
        subjects: List<String>,
        groupIds: List<String>,
    ): CreateAccountResult
}

/** Typed outcome of a profile edit, so the UI never has to guess at an exception. */
sealed interface BatchResult {
    data class Success(val changed: Int) : BatchResult
    data class Failure(val reason: AdminUserUpdateError) : BatchResult
}

sealed interface AdminUserUpdateResult {
    data object Success : AdminUserUpdateResult

    data class Failure(val reason: AdminUserUpdateError) : AdminUserUpdateResult
}

enum class AdminUserUpdateError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    /** The subject the teacher chose was already taken while the request waited. */
    SUBJECT_TAKEN,
    UNKNOWN,
}

/** What the approval transaction must commit for a teacher. */
data class TeacherApprovalPlan(
    val groupId: String,
    val subjectId: String,
    val label: String,
)

/** `null` means the intent is invalid or the subject is no longer free. */
fun teacherApprovalPlan(
    requestedGroupId: String?,
    requestedSubject: String?,
    uid: String,
    subjectTeachers: Map<String, String>,
): TeacherApprovalPlan? {
    val groupId = requestedGroupId?.takeIf { it.isNotBlank() } ?: return null
    val subjectId = requestedSubject?.takeIf { it.isNotBlank() } ?: return null
    val label = subjectById(subjectId)?.label ?: return null
    if (subjectAvailability(subjectTeachers, subjectId, uid) != SubjectAvailability.FREE) return null
    return TeacherApprovalPlan(groupId, subjectId, label)
}

/** The only two roles the principal is allowed to create from the app. */
enum class AccountRole(val firestoreValue: String) {
    DOCENTE("docente"),
    TUTOR("tutor"),
}

/**
 * Typed outcome of an account creation.
 *
 * [Failure.orphaned] is true only when the Auth user was created, the Firestore
 * write failed and the best-effort cleanup failed too. That is the one case where a
 * login exists without a profile document, and the UI must say so instead of
 * pretending the operation rolled back cleanly.
 */
sealed interface CreateAccountResult {
    data class Success(val uid: String) : CreateAccountResult

    data class Failure(
        val reason: AccountCreationError,
        val orphaned: Boolean = false,
    ) : CreateAccountResult
}

/**
 * Pure verdict for the best-effort cleanup after a failed Firestore write.
 * `delete().await()` returns null on success, so only the outcome of the call
 * and the presence of a user to delete are meaningful here.
 */
fun orphanedAfterCleanup(hadUser: Boolean, cleanupSucceeded: Boolean): Boolean =
    !hadUser || !cleanupSucceeded

enum class AccountCreationError {
    NO_SESSION,
    DENIED,
    INVALID_EMAIL,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    TIMEOUT,
    UNKNOWN,
}

/**
 * Field names of the principal-editable user profile.
 *
 * They cover both writes: the editable-field whitelist used by
 * [AdminUserRepository.updateProfile], the photo keys used by
 * [AdminUserRepository.updatePhoto], and the identity keys required by the
 * `users/{uid}` create rule used by [AdminUserRepository.createAccount]. Kept here
 * so the form payload builders and the Firestore writes cannot drift apart.
 *
 * The editable keys are a strict subset: a profile edit must never travel with an
 * identity field, while an account creation must carry all of them.
 */
object AdminUserFields {
    const val UID = "uid"
    const val EMAIL = "email"
    const val ROLE = "role"
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val FULL_NAME = "fullName"
    const val PHONE = "phone"
    const val SCHOOL_ID = "schoolId"
    const val PHOTO_URL = "photoUrl"
    const val PHOTO_PUBLIC_ID = "photoPublicId"
    const val SUBJECTS = "subjects"
    const val GROUP_IDS = "groupIds"
    const val STUDENT_IDS = "studentIds"
    const val PREFERENCES = "preferences"
    const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
    const val ACTIVE = "active"

    /**
     * Reads `preferences.notificationsEnabled` from a raw Firestore value.
     * Absent, null or a wrong type all mean "notifications on", so a legacy
     * document never silently turns the directora's notifications off.
     */
    fun notificationsEnabledFrom(preferences: Any?): Boolean =
        (preferences as? Map<*, *>)?.get(NOTIFICATIONS_ENABLED) as? Boolean ?: true

    /** The exact whitelisted payload of the notifications toggle. */
    fun notificationsUpdate(enabled: Boolean): Map<String, Any> =
        mapOf(PREFERENCES to mapOf(NOTIFICATIONS_ENABLED to enabled))
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"
    const val ESTADO = "estado"
    const val EMAIL_VERIFIED = "emailVerified"
    const val FECHA_SOLICITUD = "fechaSolicitud"
const val REQUESTED_GROUP_ID = "requestedGroupId"
const val REQUESTED_SUBJECT = "requestedSubject"

    /**
     * Marker used by the pure new-account document builder for a field Firestore
     * must fill with its own timestamp. The data layer replaces it with
     * `FieldValue.serverTimestamp()` at the write boundary, so Firebase types never
     * appear in domain or UI code.
     */
    object ServerTimestamp

    /**
     * The exact `users/{uid}` document the create rule requires.
     *
     * All rule-required literal keys are present, plus the optional phone, the
     * docente-only subjects, the optional group links and the tutor's empty
     * studentIds. [CREATED_AT] and [UPDATED_AT] hold [ServerTimestamp]; the data
     * layer replaces both with a real server timestamp before writing.
     *
     * The rule expects a docente or a tutor, so `role` comes from the closed
     * [AccountRole] enum and can never be `directivo`.
     */
    fun newAccountDocument(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        role: AccountRole,
        phone: String,
        subjects: List<String>,
        groupIds: List<String>,
        schoolId: String,
    ): Map<String, Any> {
        val first = firstName.trim()
        val last = lastName.trim()
        val document = mutableMapOf<String, Any>(
            UID to uid,
            EMAIL to email.trim(),
            ROLE to role.firestoreValue,
            FIRST_NAME to first,
            LAST_NAME to last,
            FULL_NAME to "$first $last".trim(),
            SCHOOL_ID to schoolId,
            ACTIVE to true,
            PREFERENCES to emptyMap<String, Any>(),
            CREATED_AT to ServerTimestamp,
            UPDATED_AT to ServerTimestamp,
        )
        if (phone.isNotBlank()) document[PHONE] = phone.trim()
        if (role == AccountRole.DOCENTE) {
            val labels = subjects.map { it.trim() }.filter { it.isNotEmpty() }
            document[SUBJECTS] = labels
            // A director-created teacher goes through the same intent -> approval
            // path as a self-registered one. No direct groupIds write here.
            document[ESTADO] = "pendiente"
            groupIds.firstOrNull()?.let { document[REQUESTED_GROUP_ID] = it }
            labels.firstNotNullOfOrNull { subjectIdFromLabel(it) }?.let {
                document[REQUESTED_SUBJECT] = it
            }
        }
        if (groupIds.isNotEmpty() && role != AccountRole.DOCENTE) {
            document[GROUP_IDS] = groupIds
        }
        if (role == AccountRole.TUTOR) document[STUDENT_IDS] = emptyList<String>()
        return document
    }
}
