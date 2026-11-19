package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The registration form must not trust the mockup's example code: real codes
 * are six characters, and only a real code can create a tutor account.
 */
class TutorRegistrationTest {

    private val valid = TutorRegistrationInput(
        fullName = "Mariana Gómez",
        email = "mariana@correo.com",
        phone = "5512345678",
        password = "AulaSegura#2024",
        code = "ABC123",
    )

    @Test
    fun `codes are normalised before validation`() {
        assertEquals("ABC123", normalizeStudentCode(" ab-c123 "))
        assertEquals("ABC123", normalizeStudentCode("abc123"))
        assertTrue(isValidStudentCode(normalizeStudentCode(" ab-c123 ")))
    }

    @Test
    fun `the mockup example and short codes are not valid`() {
        assertFalse(isValidStudentCode(normalizeStudentCode("ALU-2024-9821")))
        assertFalse(isValidStudentCode("ABC12"))
        assertFalse(isValidStudentCode("ABC-!23"))
    }

    @Test
    fun `validation checks name email password and code`() {
        assertNull(registrationError(valid))
        assertEquals("Escribe tu nombre completo.",
            registrationError(valid.copy(fullName = "  ")))
        assertEquals("Escribe un correo electrónico válido.",
            registrationError(valid.copy(email = "sin-arroba")))
        assertEquals("La contraseña debe tener al menos 8 caracteres.",
            registrationError(valid.copy(password = "corta")))
        assertEquals("Escribe el código de 6 caracteres que te dio la escuela.",
            registrationError(valid.copy(code = "ALU-2024-9821")))
    }

    @Test
    fun `rejections are explained to the family`() {
        assertEquals("Ese código no existe. Revisa los 6 caracteres que te dio la escuela.",
            registrationMessage(TutorRegistrationResult.InvalidCode))
        assertEquals("Ese código ya fue usado. Pide uno nuevo a la escuela.",
            registrationMessage(TutorRegistrationResult.CodeAlreadyUsed))
        assertEquals("Ese código venció. Pide uno nuevo a la escuela.",
            registrationMessage(TutorRegistrationResult.ExpiredCode))
        assertEquals("Ya existe una cuenta con ese correo. Inicia sesión.",
            registrationMessage(TutorRegistrationResult.EmailInUse))
    }
}
