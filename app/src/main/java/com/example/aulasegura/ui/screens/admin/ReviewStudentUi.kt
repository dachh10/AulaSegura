package com.example.aulasegura.ui.screens.admin

/**
 * The grade/group choice of the review form.
 *
 * A tutor with no group still has a grade (the school year), but the group is
 * empty: the student enters "Sin grupo" and is assigned later in bulk (T7).
 */
data class ReviewStudentForm(
    val grado: String = "",
    val grupo: String = "",
    val sinGrupo: Boolean = false,
    val linkAsSecondTutor: Boolean = false,
)

data class ExistingStudent(
    val name: String,
    val studentId: String,
    val groupId: String,
)

/** The single write plan produced by the review screen, before any I/O. */
data class ApprovalPlan(
    val student: ApprovedStudent?,
    val duplicateStudentId: String? = null,
    val error: String? = null,
    val grado: String = "",
    val grupo: String = "",
    val groupId: String = "",
)

data class ApprovedStudent(
    val tutorUid: String,
    val alumno: String,
    val schoolId: String,
    val createdBy: String,
    val grado: String,
    val grupo: String,
    val groupId: String,
)

/**
 * Decides how a scanned QR and a form become a write. Kept pure: a mistake here
 * would link the wrong tutor or create a duplicate student, so it is tested with
 * no database involved.
 */
fun approvalPlan(
    tutorUid: String,
    alumno: String,
    form: ReviewStudentForm,
    schoolId: String,
    createdBy: String,
    existing: List<ExistingStudent> = emptyList(),
): ApprovalPlan {
    val name = alumno.trim()
    if (tutorUid.isBlank() || name.isBlank()) {
        return ApprovalPlan(student = null, error = "Faltan datos del alumno o del tutor.")
    }

    // The data model leaves both empty for a "Sin grupo" student: the grade is
    // assigned later, in bulk, together with the group (T7).
    val degree = if (form.sinGrupo) "" else form.grado.trim()
    val group = if (form.sinGrupo) "" else form.grupo.trim()
    val groupId = if (form.sinGrupo || group.isBlank() || degree.isBlank()) {
        ""
    } else {
        "grp-${degree.replace(Regex("[^0-9]"), "")}${group.lowercase()}"
    }

    val duplicate = existing.firstOrNull {
        it.name.trim().equals(name, ignoreCase = true)
    }?.studentId

    return ApprovalPlan(
        student = ApprovedStudent(
            tutorUid = tutorUid,
            alumno = name,
            schoolId = schoolId,
            createdBy = createdBy,
            grado = degree,
            grupo = group,
            groupId = groupId,
        ),
        duplicateStudentId = duplicate,
        grado = degree,
        grupo = group,
        groupId = groupId,
    )
}
