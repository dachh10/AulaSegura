package com.example.aulasegura.domain.model

/**
 * A class group as the teacher screens understand it.
 *
 * A domain model, not the Room entity: the UI has no business knowing that
 * `teacherIds` travels as a converted string list, and it must not be able to
 * mutate the cache by accident.
 */
data class Group(
    val id: String,
    val name: String,
    val grade: Int,
    val section: String,
    val schoolId: String,
    val titularTeacherId: String?,
    val teacherIds: List<String>,
    val studentCount: Int,
    val cycle: String,
) {
    /** The group label the mockup prints in the badge ("3° B"). */
    val label: String get() = name
}

/**
 * A student inside a roster, with only what the teacher needs to take attendance.
 *
 * `curp` and the tutor links are deliberately absent: a roster is a list of names
 * and numbers, and pulling sensitive identifiers into the UI layer for no reason
 * is how they end up in a log line.
 */
data class Student(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val listNumber: Int,
    val groupId: String,
    val schoolId: String,
    val active: Boolean,
    val photoUrl: String?,
)
