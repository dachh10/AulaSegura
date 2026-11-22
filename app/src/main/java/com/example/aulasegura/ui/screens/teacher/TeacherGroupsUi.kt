package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.Group

/**
 * What a group card needs, already resolved for the screen.
 *
 * The nullable fields are the honest part: today only the name, the roster size
 * and the subject come from Firestore. Attendance percentage needs attendance
 * sessions, the "lista hecha" pill needs to know whether today's session exists
 * and the schedule lives in a subcollection the seed does not create yet. Until
 * those arrive the card hides the chip instead of printing a number nobody
 * measured — a fake "94%" is worse than an empty ring, because it gets believed.
 */
data class TeacherGroupUi(
    val id: String,
    val label: String,
    val subject: String,
    val students: Int,
    val time: String? = null,
    val attendance: Int? = null,
    val rollCallDone: Boolean? = null,
    val isToday: Boolean = true,
)

fun Group.toUi(subject: String?): TeacherGroupUi = TeacherGroupUi(
    id = id,
    label = label,
    subject = subject ?: NO_SUBJECT,
    students = studentCount,
)

/** Every distinct subject in the roster, not just the first card's one. */
fun distinctGroupSubjects(groups: List<TeacherGroupUi>): List<String> =
    groups.map { it.subject }.filter { it.isNotBlank() }.distinct()

private const val NO_SUBJECT = "Sin materia asignada"
