package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherAssignmentTest {

    @Test
    fun `the catalog has exactly the six subjects of a classroom`() {
        assertEquals(
            listOf("espanol", "matematicas", "lectura", "historia", "geografia", "ciencias"),
            SchoolCatalog.SUBJECTS.map { it.id },
        )
        assertEquals("Matemáticas", subjectById("matematicas")?.label)
    }

    @Test
    fun `grade and section resolve to the deterministic group id`() {
        assertEquals("grp-3a", groupIdFor(3, "A"))
        assertEquals("grp-6d", groupIdFor(6, "d"))
    }

    @Test
    fun `a subject is free, taken by another teacher, or invalid`() {
        assertEquals(
            SubjectAvailability.FREE,
            subjectAvailability(emptyMap(), "ciencias", "teacher-1"),
        )
        assertEquals(
            SubjectAvailability.FREE,
            subjectAvailability(mapOf("ciencias" to "teacher-1"), "ciencias", "teacher-1"),
        )
        assertEquals(
            SubjectAvailability.TAKEN_BY_OTHER,
            subjectAvailability(mapOf("ciencias" to "teacher-2"), "ciencias", "teacher-1"),
        )
        assertEquals(
            SubjectAvailability.INVALID,
            subjectAvailability(emptyMap(), "musica", "teacher-1"),
        )
    }

    @Test
    fun `the form rejects an incomplete assignment`() {
        assertTrue(teacherAssignmentError(null, "A", "espanol")!!.contains("grado"))
        assertTrue(teacherAssignmentError(3, "", "espanol")!!.contains("sección"))
        assertTrue(teacherAssignmentError(3, "A", "")!!.contains("materia"))
        assertNull(teacherAssignmentError(3, "A", "espanol"))
    }
}
