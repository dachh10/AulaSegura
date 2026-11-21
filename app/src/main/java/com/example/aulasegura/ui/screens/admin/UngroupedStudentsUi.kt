package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminStudent

/** One planned student write of the bulk assignment. */
data class StudentAssignment(
    val studentId: String,
    val groupId: String,
    val grado: Int,
    val grupo: String,
    val groupName: String,
)

/** A student with no group is a QR approval not yet assigned by the director. */
fun ungroupedStudents(
    students: List<AdminStudent>,
    query: String,
): List<AdminStudent> {
    val needle = query.trim()
    return students.filter { student ->
        student.active &&
            student.groupId.isBlank() &&
            (needle.isBlank() ||
                student.fullName.contains(needle, ignoreCase = true) ||
                student.tutorIds.any { it.contains(needle, ignoreCase = true) })
    }
}

/** Resolves the grade/group selectors to the real group document. */
fun groupForAssignment(
    groups: List<AdminGroup>,
    grado: Int,
    grupo: String,
): AdminGroup? = groups.firstOrNull {
    it.grade == grado && it.section.equals(grupo.trim(), ignoreCase = true)
}

/** The batch payload: one update per selected student, all to the same group. */
fun assignmentUpdates(
    selectedIds: List<String>,
    target: AdminGroup,
): List<StudentAssignment> = selectedIds.map { id ->
    StudentAssignment(
        studentId = id,
        groupId = target.id,
        grado = target.grade,
        grupo = target.section,
        groupName = target.name,
    )
}
