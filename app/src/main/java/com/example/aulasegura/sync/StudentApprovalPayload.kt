package com.example.aulasegura.sync

import com.example.aulasegura.domain.repository.ApprovedStudentInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything the sync handler needs to reproduce the approval batch offline:
 * the student document plus the two links added to the tutor's profile.
 */
@Serializable
data class PendingStudentApproval(
    val studentId: String,
    val alumno: String,
    val tutorUid: String,
    val schoolId: String,
    val createdBy: String,
    val grado: String,
    val grupo: String,
    val groupId: String,
    val duplicateStudentId: String? = null,
) {
    fun toInput(): ApprovedStudentInput = ApprovedStudentInput(
        alumno = alumno,
        tutorUid = tutorUid,
        schoolId = schoolId,
        createdBy = createdBy,
        grado = grado,
        grupo = grupo,
        groupId = groupId,
        duplicateStudentId = duplicateStudentId,
    )

    companion object {
        fun from(input: ApprovedStudentInput, studentId: String) = PendingStudentApproval(
            studentId = studentId,
            alumno = input.alumno,
            tutorUid = input.tutorUid,
            schoolId = input.schoolId,
            createdBy = input.createdBy,
            grado = input.grado,
            grupo = input.grupo,
            groupId = input.groupId,
            duplicateStudentId = input.duplicateStudentId,
        )
    }
}

private val json = Json { ignoreUnknownKeys = true }

fun encodeStudentApproval(approval: PendingStudentApproval): String =
    json.encodeToString(PendingStudentApproval.serializer(), approval)

fun decodeStudentApproval(raw: String): PendingStudentApproval? = runCatching {
    json.decodeFromString(PendingStudentApproval.serializer(), raw)
}.getOrNull()?.takeIf { it.studentId.isNotBlank() && it.tutorUid.isNotBlank() }
