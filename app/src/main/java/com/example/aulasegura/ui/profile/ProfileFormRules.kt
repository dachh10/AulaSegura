package com.example.aulasegura.ui.profile

import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult

/**
 * The rules every account form enforces, shared by the teacher and the tutor.
 *
 * The password policy has to be the same string for both roles: it is what the
 * checklist promises and what Firebase will accept. A copy per role drifts the
 * moment one of them is touched, and the form starts rejecting a password the
 * other one accepts.
 */

/** `Ricardo Salinas Ortega` → `RS`. */
fun profileInitials(fullName: String): String =
    fullName.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

/** `null` means the form is ready to save. */
fun profileEditError(fullName: String): String? =
    if (fullName.isBlank()) "Escribe tu nombre." else null

/** `null` means the password can be changed. Same rules the checklist displays. */
fun passwordChangeError(
    current: String,
    new: String,
    confirmation: String,
): String? = when {
    current.isBlank() -> "Escribe tu contraseña actual."
    new.isBlank() -> "Escribe la nueva contraseña."
    new.length < MIN_PASSWORD_CHARS -> "La contraseña debe tener al menos $MIN_PASSWORD_CHARS caracteres."
    new.none { it.isUpperCase() } -> "Incluye al menos una letra mayúscula."
    new.none { it.isDigit() } -> "Incluye al menos un número."
    new != confirmation -> "Las contraseñas no coinciden."
    else -> null
}

/** A rule shown in the form. `met` comes from the same predicate as the error. */
data class PasswordRuleUi(val label: String, val met: Boolean)

/** The Stitch checklist, driven by what the app actually enforces. */
fun passwordRules(new: String): List<PasswordRuleUi> = listOf(
    PasswordRuleUi("Mínimo $MIN_PASSWORD_CHARS caracteres", new.length >= MIN_PASSWORD_CHARS),
    PasswordRuleUi("Una letra mayúscula", new.any { it.isUpperCase() }),
    PasswordRuleUi("Un número", new.any { it.isDigit() }),
    PasswordRuleUi("Un símbolo (recomendado)", new.any { !it.isLetterOrDigit() }),
)

/** The "Coinciden" pill lives next to the field, not in the checklist. */
fun passwordsMatch(new: String, confirmation: String): Boolean =
    confirmation.isNotBlank() && new == confirmation

/** How many of the four Stitch strength segments are lit. */
fun passwordStrengthSteps(new: String): Int = passwordRules(new).count { it.met }

/** Short strength readout shown next to the new-password field. */
fun passwordStrengthLabel(new: String): String = when {
    new.isBlank() -> ""
    passwordStrengthSteps(new) >= 3 -> "Buena"
    else -> "Puede mejorar"
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

private const val MIN_PASSWORD_CHARS = 8
