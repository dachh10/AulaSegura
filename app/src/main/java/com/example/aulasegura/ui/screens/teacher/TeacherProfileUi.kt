package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult

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
    val saving: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
    val dialog: ProfileDialog = ProfileDialog.None,
    val editName: String = "",
    val editSubjects: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

/** The two editable dialogs. Permissions is a navigation target, not a dialog. */
sealed interface ProfileDialog {

    data object None : ProfileDialog

    data object EditProfile : ProfileDialog

    data object ChangePassword : ProfileDialog
}

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

/** `"Matemáticas, Ciencias"` — the editable form of the profile's list. */
fun subjectsInput(subjects: List<String>): String = subjects.joinToString(", ")

/** Trims, drops empties and repeats: the document stores a clean array. */
fun parseSubjects(input: String): List<String> =
    input.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

/** `null` means the form is ready to save. */
fun profileEditError(fullName: String): String? =
    if (fullName.isBlank()) "Escribe tu nombre." else null

/** `null` means the password can be changed. */
fun passwordChangeError(
    current: String,
    new: String,
    confirmation: String,
): String? = when {
    current.isBlank() -> "Escribe tu contraseña actual."
    new.isBlank() -> "Escribe la nueva contraseña."
    new.length < MIN_PASSWORD_CHARS -> "La contraseña debe tener al menos $MIN_PASSWORD_CHARS caracteres."
    new != confirmation -> "Las contraseñas no coinciden."
    else -> null
}

fun profileUpdateMessage(result: ProfileUpdateResult): String = when (result) {
    ProfileUpdateResult.Saved -> "Perfil actualizado."
    ProfileUpdateResult.SignedOut -> "Tu sesión expiró. Vuelve a iniciar sesión."
    ProfileUpdateResult.Rejected -> "No se pudo guardar el perfil. Revisa tu cuenta."
    ProfileUpdateResult.Failed -> "No se pudo guardar. Vuelve a intentarlo."
}

fun passwordChangeMessage(result: PasswordChangeResult): String = when (result) {
    PasswordChangeResult.Changed -> "Contraseña actualizada."
    PasswordChangeResult.WrongPassword -> "La contraseña actual no es correcta."
    PasswordChangeResult.WeakPassword -> "Esa contraseña es demasiado débil."
    PasswordChangeResult.RequiresRecentLogin ->
        "Vuelve a iniciar sesión para cambiar la contraseña."
    PasswordChangeResult.SignedOut -> "Tu sesión expiró. Vuelve a iniciar sesión."
    PasswordChangeResult.NetworkError -> "Sin conexión. Vuelve a intentarlo."
    PasswordChangeResult.Failed -> "No se pudo cambiar la contraseña."
}

private const val MIN_PASSWORD_CHARS = 6
