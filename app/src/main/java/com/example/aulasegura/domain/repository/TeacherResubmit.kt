package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.subjectById

/**
 * A rejected teacher reopens their application without a new Auth account.
 *
 * Email, role, school and the approved `groupIds` are deliberately absent: the
 * rules only allow the state and the requested assignment to change, so a
 * rejected teacher can never promote themselves or move to another school.
 */
internal fun teacherResubmitPatch(
    groupId: String,
    subjectId: String,
    now: Long,
): Map<String, Any> = mapOf(
    "estado" to "pendiente",
    "requestedGroupId" to groupId,
    "requestedSubject" to subjectId,
    "subjects" to listOfNotNull(subjectById(subjectId)?.label),
    "updatedAt" to now,
)

/** `null` when the teacher has not chosen a complete assignment. */
internal fun validateResubmit(groupId: String, subjectId: String): TeacherResubmitInput? =
    if (groupId.isBlank() || subjectId.isBlank()) {
        null
    } else {
        TeacherResubmitInput(groupId.trim(), subjectId.trim())
    }

internal data class TeacherResubmitInput(
    val groupId: String,
    val subjectId: String,
)
