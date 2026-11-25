package com.example.aulasegura.domain.model

/** One teacher's subject in one group, as the profile should present it. */
data class TeacherSubjectAssignment(
    val groupId: String,
    val subjectId: String,
    val label: String,
) {
    /** "Español V (5° C)" — the union the director asked for, with context. */
    val displayLabel: String get() = "$label (${displayGroupLabel(groupId, null)})"
}
