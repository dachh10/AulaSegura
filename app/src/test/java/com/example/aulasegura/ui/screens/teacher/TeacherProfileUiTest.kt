package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.ui.profile.passwordChangeError
import com.example.aulasegura.ui.profile.passwordChangeMessage
import com.example.aulasegura.ui.profile.passwordRules
import com.example.aulasegura.ui.profile.passwordStrengthLabel
import com.example.aulasegura.ui.profile.passwordStrengthSteps
import com.example.aulasegura.ui.profile.passwordsMatch
import com.example.aulasegura.ui.profile.profileEditError
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.profile.profileUpdateMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `the password form checks every rule it displays`() {
        assertEquals(
            "Escribe tu contraseña actual.",
            passwordChangeError("", "Abc12345", "Abc12345"),
        )
        assertEquals(
            "La contraseña debe tener al menos 8 caracteres.",
            passwordChangeError("vieja", "abc", "abc"),
        )
        assertEquals(
            "Incluye al menos una letra mayúscula.",
            passwordChangeError("vieja", "abcdefgh", "abcdefgh"),
        )
        assertEquals(
            "Incluye al menos un número.",
            passwordChangeError("vieja", "ABCDEFGH", "ABCDEFGH"),
        )
        assertEquals(
            "Las contraseñas no coinciden.",
            passwordChangeError("vieja", "Abc12345", "otra1234"),
        )
        // A symbol is recommended in the design but not required by the form.
        assertNull(passwordChangeError("vieja", "Abc12345", "Abc12345"))
    }

    @Test
    fun `the checklist reflects the same rules as the validation`() {
        val rules = passwordRules("Abc12345")

        assertEquals(4, rules.size)
        assertEquals(listOf(true, true, true, false), rules.map { it.met })
        assertEquals("Mínimo 8 caracteres", rules[0].label)
        assertEquals("Un símbolo (recomendado)", rules[3].label)
        assertEquals(listOf(false, false, false, false), passwordRules("").map { it.met })
        assertTrue(passwordsMatch("Abc12345", "Abc12345"))
        assertFalse(passwordsMatch("Abc12345", ""))
        assertFalse(passwordsMatch("Abc12345", "otra1234"))
    }

    @Test
    fun `the strength bar follows the enforced rules`() {
        assertEquals(0, passwordStrengthSteps(""))
        assertEquals(1, passwordStrengthSteps("abcdefgh"))
        assertEquals(3, passwordStrengthSteps("Abc12345"))
        assertEquals(4, passwordStrengthSteps("Abc12345!"))
        assertEquals("", passwordStrengthLabel(""))
        assertEquals("Puede mejorar", passwordStrengthLabel("abc"))
        assertEquals("Buena", passwordStrengthLabel("Abc12345"))
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
