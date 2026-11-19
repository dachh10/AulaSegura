package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.data.remote.cloudinary.CloudinaryTransformations
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.AdminUserUpdateError
import com.example.aulasegura.domain.repository.PasswordChangeError

/**
 * What the principal's Profile screen renders.
 *
 * Everything derived — the transformed avatar URL, the initials fallback, the
 * field rows and the upload labels — is resolved here and not in the composable:
 * a wrong transformation or a blank row is invisible on a screenshot and obvious
 * in a test. No Firebase and no Android framework types in this layer.
 */
data class AdminProfileUi(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val email: String = "",
    val schoolId: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val notificationsEnabled: Boolean = true,
    val loading: Boolean = true,
    val upload: AdminPhotoUploadUi = AdminPhotoUploadUi(),
    val edit: AdminProfileEditUi = AdminProfileEditUi(),
    val settings: AdminProfileSettingsUi = AdminProfileSettingsUi(),
    val passwordChange: AdminChangePasswordUi = AdminChangePasswordUi(),
)

/** Outcome of the notifications preference write, shown honestly in the banner. */
data class AdminProfileSettingsUi(
    val phase: AdminSettingsPhase = AdminSettingsPhase.IDLE,
    val message: String? = null,
)

enum class AdminSettingsPhase { IDLE, SAVING, SUCCESS, FAILURE }

/** Reuses the existing status banner: the two settings share the same visual states. */
fun AdminProfileSettingsUi.toEditStatus(): AdminProfileEditUi = AdminProfileEditUi(
    phase = when (phase) {
        AdminSettingsPhase.IDLE -> AdminProfileEditPhase.IDLE
        AdminSettingsPhase.SAVING -> AdminProfileEditPhase.SAVING
        AdminSettingsPhase.SUCCESS -> AdminProfileEditPhase.SUCCESS
        AdminSettingsPhase.FAILURE -> AdminProfileEditPhase.FAILURE
    },
    message = message,
)

/** Outcome of a password change, from reauthentication to the final write. */
data class AdminChangePasswordUi(
    val phase: AdminPasswordChangePhase = AdminPasswordChangePhase.IDLE,
    val message: String? = null,
)

enum class AdminPasswordChangePhase { IDLE, SAVING, SUCCESS, FAILURE }

/** Reuses the existing status banner for the password-change outcome. */
fun AdminChangePasswordUi.toEditStatus(): AdminProfileEditUi = AdminProfileEditUi(
    phase = when (phase) {
        AdminPasswordChangePhase.IDLE -> AdminProfileEditPhase.IDLE
        AdminPasswordChangePhase.SAVING -> AdminProfileEditPhase.SAVING
        AdminPasswordChangePhase.SUCCESS -> AdminProfileEditPhase.SUCCESS
        AdminPasswordChangePhase.FAILURE -> AdminProfileEditPhase.FAILURE
    },
    message = message,
)

/** Outcome of one profile edit, shown honestly after the write is attempted. */
data class AdminProfileEditUi(
    val phase: AdminProfileEditPhase = AdminProfileEditPhase.IDLE,
    val message: String? = null,
)

enum class AdminProfileEditPhase {
    IDLE,
    SAVING,
    SUCCESS,
    FAILURE,
}

/** The three fields the directora can edit about herself. */
data class AdminProfileEditForm(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
)

/** Current and new password typed into the internal change-password dialog. */
data class AdminChangePasswordForm(
    val current: String = "",
    val newPassword: String = "",
)

/** One photo change, from compression to the Firestore write. */
data class AdminPhotoUploadUi(
    val phase: AdminPhotoUploadPhase = AdminPhotoUploadPhase.IDLE,
    val message: String? = null,
)

enum class AdminPhotoUploadPhase {
    IDLE,
    COMPRESSING,
    UPLOADING,
    SAVING,
    SUCCESS,
    FAILURE,
}

/** One "label / value" row of the profile data card. */
data class AdminProfileFieldRow(val label: String, val value: String)

/** The directivo's role badge. Localized copy, kept out of the composable. */
const val ADMIN_PROFILE_ROLE_LABEL: String = "Directora"

/** CONTEXT §7.4: the profile rendition, never the original. */
const val ADMIN_AVATAR_TRANSFORMATION: String = "w_512,h_512,c_fill,g_face,q_auto,f_auto"

private const val EMPTY_VALUE: String = "—"

/**
 * URL the avatar actually paints.
 *
 * The transform goes between `/upload/` and the version segment; reusing
 * [CloudinaryTransformations.apply] keeps the idempotence guard (a URL that is
 * already transformed is returned untouched). A blank URL means "no photo", so
 * the screen falls back to initials instead of downloading anything.
 */
fun adminAvatarUrl(photoUrl: String?): String? {
    val url = photoUrl?.takeIf { it.isNotBlank() } ?: return null
    return CloudinaryTransformations.apply(url, ADMIN_AVATAR_TRANSFORMATION)
}

/** Initials shown when there is no photo: first letters of the first two names. */
fun adminInitials(fullName: String?): String {
    val parts = fullName?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Honest, short status for each step of a photo change. */
fun uploadPhaseLabel(phase: AdminPhotoUploadPhase): String = when (phase) {
    AdminPhotoUploadPhase.IDLE -> ""
    AdminPhotoUploadPhase.COMPRESSING -> "Comprimiendo foto…"
    AdminPhotoUploadPhase.UPLOADING -> "Subiendo foto…"
    AdminPhotoUploadPhase.SAVING -> "Guardando cambios…"
    AdminPhotoUploadPhase.SUCCESS -> "Foto actualizada"
    AdminPhotoUploadPhase.FAILURE -> "No se pudo actualizar la foto"
}

fun adminProfileFieldRows(user: AdminUser): List<AdminProfileFieldRow> =
    adminProfileFieldRows(user.fullName, user.email, user.schoolId, user.phone)

/** A blank field prints an em dash rather than an empty, unlabeled space. */
fun adminProfileFieldRows(
    fullName: String,
    email: String,
    schoolId: String,
    phone: String,
): List<AdminProfileFieldRow> = listOf(
    AdminProfileFieldRow("Nombre", fullName.ifBlank { EMPTY_VALUE }),
    AdminProfileFieldRow("Correo", email.ifBlank { EMPTY_VALUE }),
    AdminProfileFieldRow("Escuela", schoolId.ifBlank { EMPTY_VALUE }),
    AdminProfileFieldRow("Teléfono", phone.ifBlank { EMPTY_VALUE }),
)

/** Error under the edit form, or null when it can be saved. */
fun profileValidationError(firstName: String, lastName: String): String? = when {
    firstName.isBlank() -> "El nombre es obligatorio."
    lastName.isBlank() -> "El apellido es obligatorio."
    else -> null
}

/**
 * Exact whitelisted payload of a self profile edit: the two name parts, the derived
 * fullName and the phone. No identity field can reach the wire; the repository adds
 * `updatedAt` at the data boundary.
 */
fun buildProfileUpdateMap(
    firstName: String,
    lastName: String,
    phone: String,
): Map<String, Any> {
    val first = firstName.trim()
    val last = lastName.trim()
    return mapOf(
        AdminUserFields.FIRST_NAME to first,
        AdminUserFields.LAST_NAME to last,
        AdminUserFields.FULL_NAME to "$first $last".trim(),
        AdminUserFields.PHONE to phone.trim(),
    )
}

/** The exact whitelisted payload of the notifications toggle: no identity field. */
fun buildNotificationsUpdateMap(enabled: Boolean): Map<String, Any> =
    AdminUserFields.notificationsUpdate(enabled)

/** Guard shared by the change-password dialog and its ViewModel. */
fun changePasswordValidationError(current: String, newPassword: String): String? = when {
    current.isBlank() -> "Escribe tu contraseña actual."
    newPassword.length < 6 -> "La nueva contraseña debe tener al menos 6 caracteres."
    else -> null
}

/** Honest, typed copy for a password change that Firebase refused. */
fun passwordChangeMessage(error: PasswordChangeError): String = when (error) {
    PasswordChangeError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    PasswordChangeError.WRONG_PASSWORD -> "La contraseña actual no es correcta."
    PasswordChangeError.WEAK_PASSWORD ->
        "La nueva contraseña es demasiado débil. Usa al menos 6 caracteres."

    PasswordChangeError.REQUIRES_RECENT_LOGIN ->
        "Por seguridad, vuelve a iniciar sesión antes de cambiar la contraseña."

    PasswordChangeError.NETWORK -> "Sin conexión. La contraseña no se cambió; inténtalo de nuevo."
    PasswordChangeError.UNKNOWN -> "No se pudo cambiar la contraseña."
}

/** Honest copy when the notifications preference could not be saved. */
fun notificationsFailureMessage(error: AdminUserUpdateError): String = when (error) {
    AdminUserUpdateError.NO_SESSION -> "La sesión terminó. Vuelve a iniciar sesión."
    AdminUserUpdateError.NETWORK,
    AdminUserUpdateError.TIMEOUT,
    -> "Sin conexión. La preferencia no se guardó; inténtalo de nuevo."

    AdminUserUpdateError.DENIED -> "No tienes permiso para cambiar esta preferencia."
    AdminUserUpdateError.UNKNOWN -> "No se pudo guardar la preferencia."
}
