package com.example.aulasegura.data.remote

import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.ApprovedGroup
import com.google.firebase.firestore.FieldValue

/** Sentinel for a Firestore `increment`, so the write plan is testable on the JVM. */
internal data class Increment(val by: Int)

/**
 * What a roster change writes to the classroom document.
 *
 * `teacherIds` and `subjectTeachers` are calculated by the approval/assignment
 * flows, not by adding students: merging the template's empty `teacherIds` into
 * an existing group silently unassigns every teacher the director put there.
 * Only a newly created group gets the empty list.
 */
internal fun assignmentGroupFields(
    groupExists: Boolean,
    name: String,
    grade: Int,
    section: String,
    schoolId: String,
    newStudents: Int,
): Map<String, Any> = buildMap {
    put(AdminGroupFields.NAME, name.trim())
    put(AdminGroupFields.GRADE, grade)
    put(AdminGroupFields.SECTION, section.trim())
    put(AdminGroupFields.SCHOOL_ID, schoolId)
    put(AdminGroupFields.CYCLE, AdminGroupFields.DEFAULT_CYCLE)
    if (!groupExists) put(AdminGroupFields.TEACHER_IDS, emptyList<String>())
    put(AdminGroupFields.STUDENT_COUNT, Increment(newStudents))
}

/** Fields of a QR approval, before Firestore sentinels are applied. */
internal fun approvedGroupFields(
    group: ApprovedGroup,
    schoolId: String,
    groupExists: Boolean,
): Map<String, Any> = assignmentGroupFields(
    groupExists = groupExists,
    name = group.name,
    grade = group.grade,
    section = group.section,
    schoolId = schoolId,
    newStudents = 1,
)

/**
 * The `groups/{id}` document written when a QR approval assigns a grade+section.
 * Shared by the online approval and the offline replay so both create the group
 * on demand and increment its counter instead of resetting it.
 */
fun approvedGroupDocument(
    group: ApprovedGroup,
    schoolId: String,
    groupExists: Boolean,
): Map<String, Any> = approvedGroupFields(group, schoolId, groupExists)
    .mapValues { (_, value) -> if (value is Increment) FieldValue.increment(value.by.toLong()) else value }
    .toMutableMap()
    .apply { put(AdminGroupFields.UPDATED_AT, FieldValue.serverTimestamp()) }
