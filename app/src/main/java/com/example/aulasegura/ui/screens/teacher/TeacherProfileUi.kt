package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser

/**
 * What the profile screen renders, already resolved from the session profile.
 *
 * The screen never reads Firebase or a repository: it renders this and calls a
 * callback. The same split as the incident and publication forms.
 *
 * The account rules and the password form itself live in `ui/profile`: they are
 * identical for the tutor, so they are not duplicated per role.
 */
data class TeacherProfileUi(
    val fullName: String = "",
    val email: String = "",
    val schoolId: String = "",
    /** Configured school name; the id is only a fallback for legacy documents. */
    val schoolName: String = "",
    val subjects: List<String> = emptyList(),
    val groups: List<String> = emptyList(),
    val photoUrl: String? = null,
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
    val screen: ProfileSection = ProfileSection.None,
    val editName: String = "",
    val editSubjects: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

/** The two full-screen account forms. Permissions is a navigation target. */
sealed interface ProfileSection {

    data object None : ProfileSection

    data object EditProfile : ProfileSection

    data object ChangePassword : ProfileSection
}

/** Maps the cached session profile into the screen model. */
internal fun AppUser.toProfileUi(): TeacherProfileUi = TeacherProfileUi(
    fullName = fullName,
    email = email,
    schoolId = schoolId,
    subjects = subjectAssignments.map { it.displayLabel }.ifEmpty { subjects },
    photoUrl = photoUrl,
)

/** The configured name wins; the raw id is a last resort, never the normal copy. */
fun schoolLabel(schoolId: String, configuredName: String?): String =
    configuredName?.takeIf { it.isNotBlank() }
        ?: schoolId.takeIf { it.isNotBlank() }
        ?: "Sin asignar"

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

/** `"Matemáticas, Ciencias"` — the editable form of the profile's list. */
fun subjectsInput(subjects: List<String>): String = subjects.joinToString(", ")

/** Trims, drops empties and repeats: the document stores a clean array. */
fun parseSubjects(input: String): List<String> =
    input.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
