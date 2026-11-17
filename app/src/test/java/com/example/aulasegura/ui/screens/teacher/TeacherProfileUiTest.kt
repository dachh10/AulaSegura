package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The mockup had "RS", "Hola, profe Ricardo" and "Docente · Matemáticas" written
 * by hand. These helpers are the whole difference between a shell and a profile
 * that belongs to whoever signed in.
 */
class TeacherProfileUiTest {

    private val teacher = AppUser(
        uid = "uid-1",
        email = "ricardo@escuela.mx",
        role = UserRole.TEACHER,
        fullName = "Ricardo Salinas Ortega",
        schoolId = "sch-benito-juarez",
        active = true,
        photoUrl = "https://res.cloudinary.com/demo/image/upload/aulasegura/profiles/uid-1.jpg",
        subjects = listOf("Matemáticas", "Ciencias"),
    )

    @Test
    fun `the avatar fallback uses the first two names`() {
        assertEquals("RS", profileInitials(teacher.fullName))
    }

    @Test
    fun `a one word name still yields one initial`() {
        assertEquals("R", profileInitials("Ricardo"))
        assertEquals("?", profileInitials("   "))
    }

    @Test
    fun `the profile card reads the real subjects`() {
        assertEquals("Docente · Matemáticas", profileSubtitle(teacher.subjects))
        assertEquals("Docente", profileSubtitle(emptyList()))
    }

    @Test
    fun `the home greeting uses the first real name`() {
        assertEquals("Hola, profe Ricardo", teacherGreeting(teacher.fullName))
        assertEquals("Hola, profe", teacherGreeting(""))
    }

    @Test
    fun `the cached profile fills every field of the screen model`() {
        val ui = teacher.toProfileUi()

        assertEquals(teacher.fullName, ui.fullName)
        assertEquals(teacher.email, ui.email)
        assertEquals(teacher.schoolId, ui.schoolId)
        assertEquals(teacher.subjects, ui.subjects)
        assertEquals(teacher.photoUrl, ui.photoUrl)
        assertEquals(false, ui.uploading)
        assertEquals(null, ui.message)
    }

    @Test
    fun `subjects round trip through the editable field`() {
        val input = subjectsInput(listOf("Matemáticas", "Ciencias"))

        assertEquals("Matemáticas, Ciencias", input)
        assertEquals(listOf("Matemáticas", "Ciencias"), parseSubjects(input))
    }

    @Test
    fun `subjects input trims repeats and empties`() {
        assertEquals(
            listOf("Matemáticas", "Ciencias"),
            parseSubjects(" Matemáticas , , Ciencias , Matemáticas "),
        )
        assertEquals(emptyList<String>(), parseSubjects("   "))
    }

    @Test
    fun `the profile form refuses an empty name`() {
        assertEquals("Escribe tu nombre.", profileEditError("   "))
        assertNull(profileEditError("Ricardo Salinas"))
    }

    @Test
    fun `the password form checks length and confirmation`() {
        assertEquals(
            "Escribe tu contraseña actual.",
            passwordChangeError("", "abc123", "abc123"),
        )
        assertEquals(
            "La contraseña debe tener al menos 6 caracteres.",
            passwordChangeError("vieja", "abc", "abc"),
        )
        assertEquals(
            "Las contraseñas no coinciden.",
            passwordChangeError("vieja", "abc123", "otra123"),
        )
        assertNull(passwordChangeError("vieja", "abc123", "abc123"))
    }

    @Test
    fun `results explain themselves in the teacher language`() {
        assertEquals("Perfil actualizado.", profileUpdateMessage(ProfileUpdateResult.Saved))
        assertEquals(
            "La contraseña actual no es correcta.",
            passwordChangeMessage(PasswordChangeResult.WrongPassword),
        )
        assertEquals(
            "Vuelve a iniciar sesión para cambiar la contraseña.",
            passwordChangeMessage(PasswordChangeResult.RequiresRecentLogin),
        )
    }
}
