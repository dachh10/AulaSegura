package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AppUser

/** Which tab of the registration form the user is filling. */
enum class RegistrationKind {
    TUTOR,
    DOCENTE,
}

/**
 * Registration without a student code. The child's name travels in the tutor's
 * profile and, later, inside the QR the directora scans. The teacher's subject
 * is what the directora reviews before approving the account.
 */
data class AccountRegistrationInput(
    val kind: RegistrationKind,
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val childName: String = "",
    val subject: String = "",
    val accepted: Boolean = false,
)

sealed interface AccountRegistrationResult {

    data class Success(val user: AppUser) : AccountRegistrationResult

    data object EmailInUse : AccountRegistrationResult
    data object WeakPassword : AccountRegistrationResult
    data object NetworkError : AccountRegistrationResult

    /** Firestore refused the profile write, usually because the rules are stale. */
    data object Rejected : AccountRegistrationResult
    data object Failed : AccountRegistrationResult
}

/** Four-segment strength meter used by the registration form. 0..4. */
fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it.isLetter() && it.isUpperCase() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score.coerceAtMost(4)
}

/** `null` means the form is ready to submit. */
fun accountRegistrationError(input: AccountRegistrationInput): String? = when {
    input.fullName.isBlank() -> "Escribe tu nombre completo."
    !input.email.contains("@") || !input.email.contains(".") ->
        "Escribe un correo electrónico válido. Usa un correo real: te enviaremos un enlace de verificación."
    input.password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
    input.password != input.confirmPassword -> "Las contraseñas no coinciden."
    !input.accepted -> "Acepta el aviso de privacidad y los términos de uso."
    input.kind == RegistrationKind.TUTOR && input.childName.isBlank() ->
        "Escribe el nombre completo de tu hijo."
    input.kind == RegistrationKind.DOCENTE && input.subject.isBlank() ->
        "Escribe la materia o grado que impartes."
    else -> null
}

fun accountRegistrationMessage(result: AccountRegistrationResult): String = when (result) {
    is AccountRegistrationResult.Success -> "Cuenta creada. Revisa tu correo."
    AccountRegistrationResult.EmailInUse ->
        "Ya existe una cuenta con ese correo. Inicia sesión."
    AccountRegistrationResult.WeakPassword -> "La contraseña es demasiado débil."
    AccountRegistrationResult.NetworkError ->
        "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AccountRegistrationResult.Rejected ->
        "La escuela no pudo crear la cuenta. Contacta a la administración."
    AccountRegistrationResult.Failed ->
        "No pudimos crear la cuenta. Inténtalo de nuevo."
}
