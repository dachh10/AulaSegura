package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import org.junit.Assert.assertEquals
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
}
