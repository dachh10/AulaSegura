package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminScheduleSlot

/**
 * The principal's group management: one-shot reads plus the three writes this
 * block owns.
 *
 * Reads are one-shot on purpose. A snapshot listener left open while the screen is
 * on screen burns Spark's daily read budget, and the list only needs to refresh
 * after a write — which the ViewModel does explicitly.
 */
interface AdminGroupRepository {

    /** All groups of the signed-in principal's school. Schedules are not loaded here. */
    suspend fun getGroups(): List<AdminGroup>

    /** One group of this school with its schedule slots, or null if it does not exist. */
    suspend fun getGroup(groupId: String): AdminGroup?

    suspend fun createGroup(name: String, grade: Int, section: String): AdminGroupResult

    /**
     * Renames an existing group. The implementation writes only name, grade,
     * section and updatedAt: schoolId and the derived counters stay untouched.
     */
    suspend fun updateGroup(
        groupId: String,
        name: String,
        grade: Int,
        section: String,
    ): AdminGroupResult

    /**
     * Deletes a group and its schedule subcollection in ONE WriteBatch.
     *
     * When the group has students, [destinationGroupId] is required: every student
     * is moved to that group (their `active` flag is never touched) and both
     * `studentCount` counters are adjusted in the same batch. An empty group needs
     * no destination. The implementation rejects a missing, identical or foreign
     * destination before writing anything.
     */
    suspend fun deleteGroup(
        groupId: String,
        destinationGroupId: String? = null,
    ): AdminGroupResult

    /**
     * Assigns or deassigns a teacher. Both sides of the link are written in ONE
     * WriteBatch by the implementation; a caller never sees a half-written link.
     */
    suspend fun assignTeacher(
        groupId: String,
        teacherUid: String,
        assign: Boolean,
    ): AdminGroupResult

    /** Idempotent upsert of a group's schedule, keyed by a deterministic slot id. */
    suspend fun saveSchedule(groupId: String, slots: List<AdminScheduleSlot>): AdminGroupResult
}

/** Typed outcome of every group write, so the UI never has to guess at an exception. */
sealed interface AdminGroupResult {
    data object Success : AdminGroupResult

    /**
     * [detail] carries a short, sanitized diagnostic from the data layer when the
     * platform error was a permission denial or an unknown failure. It is optional
     * so existing callers that only know the typed [reason] stay unchanged.
     */
    data class Failure(
        val reason: AdminGroupError,
        val detail: String? = null,
    ) : AdminGroupResult
}

enum class AdminGroupError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    HAS_STUDENTS,
    DESTINATION_REQUIRED,
    DESTINATION_SAME_AS_SOURCE,
    DESTINATION_NOT_FOUND,
    UNKNOWN,
}

/**
 * Field names and the pure builders shared by the UI form and the Firestore write.
 *
 * Keeping them in one object is what stops a form payload from drifting away from
 * the document the data layer actually writes. Firebase types never appear here:
 * server timestamps are added at the data boundary.
 */
object AdminGroupFields {
    const val COLLECTION = "groups"
    const val USER_COLLECTION = "users"
    const val SCHEDULE_COLLECTION = "schedule"

    const val NAME = "name"
    const val GRADE = "grade"
    const val SECTION = "section"
    const val SCHOOL_ID = "schoolId"
    const val TEACHER_IDS = "teacherIds"
    const val STUDENT_COUNT = "studentCount"
    const val CYCLE = "cycle"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"

    const val WEEKDAY = "weekday"
    const val START_TIME = "startTime"
    const val END_TIME = "endTime"
    const val SUBJECT = "subject"
    const val TEACHER_ID = "teacherId"

    const val DEFAULT_CYCLE = "2026-2027"

    fun groupPath(groupId: String): String = "$COLLECTION/$groupId"
    fun userPath(teacherUid: String): String = "$USER_COLLECTION/$teacherUid"

    /** Fields of a new group document, without the server timestamps. */
    fun newGroupDocument(
        name: String,
        grade: Int,
        section: String,
        schoolId: String,
    ): Map<String, Any> = mapOf(
        NAME to name.trim(),
        GRADE to grade,
        SECTION to section.trim(),
        SCHOOL_ID to schoolId,
        // A group has several teachers and no single "titular". The legacy field
        // is left untouched on existing documents but is never written on new ones.
        TEACHER_IDS to emptyList<String>(),
        STUDENT_COUNT to 0,
        CYCLE to DEFAULT_CYCLE,
    )

    /**
     * Fields of an existing group the principal may edit. Deliberately excludes
     * schoolId and the derived counters; the data layer adds updatedAt.
     */
    fun groupUpdateDocument(
        name: String,
        grade: Int,
        section: String,
    ): Map<String, Any> = mapOf(
        NAME to name.trim(),
        GRADE to grade,
        SECTION to section.trim(),
    )

    /**
     * Deterministic slot id. The same weekday and start time always map to the
     * same document, so saving a schedule twice cannot duplicate a slot.
     */
    fun slotId(weekday: Int, startTime: String): String =
        "$weekday-${startTime.replace(":", "")}"

    /** Firestore payload of one schedule slot. */
    fun slotDocument(slot: AdminScheduleSlot): Map<String, Any> = mapOf(
        WEEKDAY to slot.weekday,
        START_TIME to slot.startTime.trim(),
        END_TIME to slot.endTime.trim(),
        SUBJECT to slot.subject.trim(),
        TEACHER_ID to slot.teacherId.trim(),
    )
}

/**
 * A human-readable, testable description of the two-sided assignment write.
 *
 * The data layer derives its batch from the same field names; this description is
 * what the UI can show and what the unit test asserts. Naming BOTH paths here is a
 * regression guard: a future refactor that drops one side fails the test instead of
 * silently desynchronizing `groups/{id}.teacherIds` from `users/{uid}.groupIds`.
 */
data class GroupAssignmentDescription(
    val groupPath: String,
    val groupField: String,
    val userPath: String,
    val userField: String,
    val assign: Boolean,
) {
    val operation: String get() = if (assign) "arrayUnion" else "arrayRemove"

    val summary: String get() = "$groupPath.$groupField $operation; $userPath.$userField $operation"
}

/** Builds the description from the same paths the Firestore batch writes. */
fun describeGroupAssignment(
    groupId: String,
    teacherUid: String,
    assign: Boolean,
): GroupAssignmentDescription = GroupAssignmentDescription(
    groupPath = AdminGroupFields.groupPath(groupId),
    groupField = AdminGroupFields.TEACHER_IDS,
    userPath = AdminGroupFields.userPath(teacherUid),
    userField = AdminUserFields.GROUP_IDS,
    assign = assign,
)
