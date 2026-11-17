package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser

/**
 * What the profile screen renders, already resolved from the session profile.
 *
 * The screen never reads Firebase or a repository: it renders this and calls a
 * callback. The same split as the incident and publication forms.
 */
data class TeacherProfileUi(
    val fullName: String = "",
    val email: String = "",
    val schoolId: String = "",
    val subjects: List<String> = emptyList(),
    val photoUrl: String? = null,
    val uploading: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
)

/** Maps the cached session profile into the screen model. */
internal fun AppUser.toProfileUi(): TeacherProfileUi = TeacherProfileUi(
    fullName = fullName,
    email = email,
    schoolId = schoolId,
    subjects = subjects,
    photoUrl = photoUrl,
)

/**
 * `Ricardo Salinas Ortega` → `RS`.
 *
 * The mockup used a fixed `RS`; deriving it keeps the fallback avatar honest for
 * every teacher, including a two-word name and the one whose name has not loaded.
 */
fun profileInitials(fullName: String): String =
    fullName.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/** `Docente · Matemáticas`, or just `Docente` when the profile has no subjects. */
fun profileSubtitle(subjects: List<String>): String =
    subjects.firstOrNull()?.takeIf { it.isNotBlank() }
        ?.let { "Docente · $it" }
        ?: "Docente"

/** The greeting of the home screen, derived from the same cached profile. */
fun teacherGreeting(fullName: String): String =
    fullName.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() }
        ?.let { "Hola, profe $it" }
        ?: "Hola, profe"
