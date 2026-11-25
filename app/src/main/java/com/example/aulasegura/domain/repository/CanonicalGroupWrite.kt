package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.groupIdFor
import com.example.aulasegura.domain.model.groupNameFor

/**
 * The write plan for a classroom created from the director's manual form.
 *
 * The document id is derived from grade+section like everywhere else in the app:
 * a random id made "5° C" exist twice, with teachers attached to one of them and
 * students to the other. A repeat create must be a no-op for calculated fields.
 */
internal fun canonicalGroupId(grade: Int, section: String): String = groupIdFor(grade, section)

internal fun manualGroupDocument(
    grade: Int,
    section: String,
    name: String = groupNameFor(grade, section),
    schoolId: String,
): Map<String, Any> = mapOf(
    AdminGroupFields.NAME to name.trim(),
    AdminGroupFields.GRADE to grade,
    AdminGroupFields.SECTION to section.trim().uppercase(),
    AdminGroupFields.SCHOOL_ID to schoolId,
    AdminGroupFields.CYCLE to AdminGroupFields.DEFAULT_CYCLE,
)

/**
 * A subject slot as a nested map, usable in both `set(merge)` and `update`.
 * A dotted key like "subjectTeachers.g5-espanol-v" is stored literally by
 * `set()`, so later reads of `subjectTeachers` see nothing and allow a second
 * teacher to take the same subject.
 */
internal fun subjectTeachersEntry(subjectId: String, teacherUid: String): Map<String, Any> =
    mapOf("subjectTeachers" to mapOf(subjectId to teacherUid))
