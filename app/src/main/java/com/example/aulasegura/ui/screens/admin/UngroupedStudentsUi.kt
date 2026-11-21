package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AvailableGrades
import com.example.aulasegura.domain.model.AvailableSections
import com.example.aulasegura.domain.model.groupIdFor
import com.example.aulasegura.domain.model.groupNameFor

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

/** The fixed catalogs shown by the selectors, available even with an empty DB. */
val assignableGrades: List<Int> get() = AvailableGrades
val assignableSections: List<String> get() = AvailableSections

/**
 * Resolves the grade/group decision to a canonical group identity. The group
 * document is created on demand by the repository using this deterministic id.
 */
fun groupForAssignment(grado: Int, grupo: String): AdminGroup? {
    if (grado !in AvailableGrades || grupo.trim().uppercase() !in AvailableSections) return null
    return AdminGroup(
        id = groupIdFor(grado, grupo),
        name = groupNameFor(grado, grupo),
        grade = grado,
        section = grupo.trim().uppercase(),
        schoolId = "",
        teacherIds = emptyList(),
        titularTeacherId = "",
        studentCount = 0,
        cycle = "2026-2027",
    )
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
