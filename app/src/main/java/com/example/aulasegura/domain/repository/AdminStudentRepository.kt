package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminStudent

/**
 * The principal's students: one-shot reads plus the writes this block owns.
 *
 * A student created here is immediately visible to the teacher's roll call: the
 * document carries `groupId`, `schoolId` and `active = true`, and creation updates
 * `groups/{groupId}.studentCount` together with the roster numbers in one atomic
 * transaction.
 *
 * Reads are one-shot on purpose. A snapshot listener left open while the screen is
 * on screen burns Spark's daily read budget, and the list only needs to refresh
 * after a write — which the ViewModel does explicitly.
 */
interface AdminStudentRepository {

    /** All students of the signed-in principal's school, active and inactive. */
    suspend fun getStudents(): List<AdminStudent>

    /**
     * Creates an active student and increments the group's derived count in one
     * transaction. The list number is not supplied by the caller: it is computed
     * from the alphabetical order of the group's active roster, and every active
     * student whose number changes is renumbered in the same transaction.
     */
    suspend fun createStudent(input: AdminStudentInput): CreateStudentResult

    /**
     * Logical baja/alta: writes `active` and the matching count delta in one batch.
     * The document is never deleted, so history and tutor links survive.
     */
    suspend fun setActive(studentId: String, active: Boolean): AdminStudentResult

    /**
     * Approves the QR of a tutor: creates the student with the tutor linked and
     * moves the tutor's `vinculacion` to `vinculado`. Both writes belong to the
     * director, so they go in one batch.
     */
    suspend fun approveStudent(input: ApprovedStudentInput): ApproveStudentResult

    /** Creates a one-use linking code for a tutor and returns it to the UI. */
    suspend fun createLinkingCode(studentId: String): LinkingCodeResult

    /**
     * The still-valid, unredeemed linking code of a student, or null when there is
     * none. Used by the detail dialog so the principal can recover a code instead
     * of generating another one.
     */
    suspend fun getLinkingCode(studentId: String): String?
}

/**
 * The principal's input for one new student; identity, links and the list number
 * are never input. The number comes from the roster order at creation time.
 */
data class AdminStudentInput(
    val firstName: String,
    val lastName: String,
    val curp: String,
    val groupId: String,
)

/** The director's approval of a scanned tutor QR. */
data class ApprovedStudentInput(
    val alumno: String,
    val tutorUid: String,
    val schoolId: String,
    val createdBy: String,
    val grado: String,
    val grupo: String,
    val groupId: String,
    /** When a student with the same name already exists, add this tutor to it. */
    val duplicateStudentId: String? = null,
)

/**
 * Fields added to the tutor's `users/{uid}` on approval. `studentIds` and
 * `groupIds` are the denormalised links the Firestore rules read to decide
 * which groups' attendance, tasks and notices the tutor may see. Without them
 * the student document is visible but every other screen is DENIED.
 */
fun tutorProfileOnApproval(
    studentId: String,
    groupId: String,
): Map<String, Any> = buildMap {
    put("studentIds", listOf(studentId))
    if (groupId.isNotBlank()) put("groupIds", listOf(groupId))
    put("vinculacion", "vinculado")
}

sealed interface ApproveStudentResult {
    data class Success(val studentId: String) : ApproveStudentResult
    data class Failure(val reason: AdminStudentError) : ApproveStudentResult
}

/** Result of a create, carrying the id needed for the linking code. */
sealed interface CreateStudentResult {
    data class Success(val studentId: String) : CreateStudentResult

    /** [detail] is an optional, sanitized platform diagnostic for DENIED/UNKNOWN. */
    data class Failure(
        val reason: AdminStudentError,
        val detail: String? = null,
    ) : CreateStudentResult
}

/** Typed outcome of a logical baja/alta. */
sealed interface AdminStudentResult {
    data object Success : AdminStudentResult

    /** [detail] is an optional, sanitized platform diagnostic for DENIED/UNKNOWN. */
    data class Failure(
        val reason: AdminStudentError,
        val detail: String? = null,
    ) : AdminStudentResult
}

/** Result of a linking-code creation, carrying the code the principal must share. */
sealed interface LinkingCodeResult {
    data class Success(val code: String) : LinkingCodeResult

    /** [detail] is an optional, sanitized platform diagnostic for DENIED/UNKNOWN. */
    data class Failure(
        val reason: AdminStudentError,
        val detail: String? = null,
    ) : LinkingCodeResult
}

enum class AdminStudentError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    DUPLICATE_LIST_NUMBER,
    UNKNOWN,
}

/**
 * Field names and pure builders shared by the UI form and the Firestore write.
 *
 * Keeping them in one object is what stops a form payload from drifting away from
 * the document the data layer actually writes. Firebase types never appear here:
 * server timestamps are added at the data boundary.
 */
object AdminStudentFields {
    const val COLLECTION = "students"
    const val GROUP_COLLECTION = "groups"
    const val LINKING_CODES_COLLECTION = "linkingCodes"

    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val FULL_NAME = "fullName"
    const val CURP = "curp"
    const val LIST_NUMBER = "listNumber"
    const val GROUP_ID = "groupId"
    const val SCHOOL_ID = "schoolId"
    const val TUTOR_IDS = "tutorIds"
    const val ACTIVE = "active"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"

    // Linking-flow vocabulary from the requirement doc. Written alongside the
    // legacy keys until every reader has moved, never instead of them.
    const val NOMBRE = "nombre"
    const val TUTORES = "tutores"
    const val GRADO = "grado"
    const val GRUPO = "grupo"
    const val CREADO_POR = "creadoPor"
    const val SYNC_STATUS = "syncStatus"
    const val SYNCED = "SYNCED"

    // linkingCodes/{code}
    const val STUDENT_ID = "studentId"
    const val CREATED_BY = "createdBy"
    const val EXPIRES_AT = "expiresAt"
    const val USED_BY = "usedBy"
    const val USED_AT = "usedAt"

    const val LINKING_CODE_LENGTH = 6
    const val LINKING_CODE_VALIDITY_DAYS = 7

    fun studentPath(studentId: String): String = "$COLLECTION/$studentId"
    fun groupPath(groupId: String): String = "$GROUP_COLLECTION/$groupId"
    fun linkingCodePath(code: String): String = "$LINKING_CODES_COLLECTION/$code"

    /**
     * Deterministic student id, aligned with the seed script's format:
     * `grp-3b` + list number 7 becomes `stu-3b-07`. A collision with a deactivated
     * document is resolved at the data boundary with a UUID suffix.
     */
    fun studentId(groupId: String, listNumber: Int): String {
        val bareGroup = groupId.removePrefix("grp-")
        val number = listNumber.toString().padStart(2, '0')
        return "stu-$bareGroup-$number"
    }

    /** Fields of a new student document, without the server timestamps. */
    fun newStudentDocument(
        input: AdminStudentInput,
        schoolId: String,
        listNumber: Int,
        createdBy: String = "",
    ): Map<String, Any> {
        val firstName = input.firstName.trim()
        val lastName = input.lastName.trim()
        val fullName = "$firstName $lastName".trim()
        val group = input.groupId.trim()
        return mapOf(
            FIRST_NAME to firstName,
            LAST_NAME to lastName,
            FULL_NAME to fullName,
            CURP to input.curp.trim().uppercase(),
            LIST_NUMBER to listNumber,
            GROUP_ID to group,
            SCHOOL_ID to schoolId,
            TUTOR_IDS to emptyList<String>(),
            ACTIVE to true,
            NOMBRE to fullName,
            TUTORES to emptyList<String>(),
            GRADO to "",
            GRUPO to group,
            CREADO_POR to createdBy,
            SYNC_STATUS to SYNCED,
        )
    }
}
