package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherAssignmentTest {

    @Test
    fun `each grade has exactly its own six subjects`() {
        assertEquals(
            listOf(
                "Español I", "Matemáticas I", "Exploración de la Naturaleza",
                "Convivencia y Valores (Cívica I)", "Educación Física I", "Artes I",
            ),
            subjectsForGrade(1).map { it.label },
        )
        assertEquals(
            listOf(
                "Español VI", "Matemáticas VI", "Ciencias Naturales VI",
                "Historia Universal", "Geografía Universal II",
                "Formación Cívica y Ética VI",
            ),
            subjectsForGrade(6).map { it.label },
        )
        SchoolCatalog.GRADES.forEach { grade ->
            val subjects = subjectsForGrade(grade)
            assertEquals(6, subjects.size)
            assertEquals(grade, subjects.first().grade)
            assertEquals(subjects.size, subjects.map { it.id }.distinct().size)
        }
        assertEquals(36, SchoolCatalog.SUBJECTS.size)
    }

    @Test
    fun `grade and section resolve to the deterministic group id`() {
        assertEquals("grp-3a", groupIdFor(3, "A"))
        assertEquals("grp-6d", groupIdFor(6, "d"))
        assertEquals(3 to "A", gradeSectionFromGroupId("grp-3a"))
        assertNull(gradeSectionFromGroupId("grp-9a"))
    }

    @Test
    fun `a subject is free, taken by another teacher, or invalid`() {
        assertEquals(
            SubjectAvailability.FREE,
            subjectAvailability(emptyMap(), "g1-artes-i", "teacher-1"),
        )
        assertEquals(
            SubjectAvailability.TAKEN_BY_OTHER,
            subjectAvailability(mapOf("g1-artes-i" to "teacher-2"), "g1-artes-i", "teacher-1"),
        )
        assertEquals(
            SubjectAvailability.INVALID,
            subjectAvailability(emptyMap(), "musica", "teacher-1"),
        )
    }

    @Test
    fun `the form rejects an incomplete or cross-grade assignment`() {
        assertTrue(teacherAssignmentError(null, "A", "g1-espanol-i")!!.contains("grado"))
        assertTrue(teacherAssignmentError(1, "", "g1-espanol-i")!!.contains("sección"))
        assertTrue(teacherAssignmentError(1, "A", "")!!.contains("materia"))
        assertTrue(teacherAssignmentError(1, "A", "g4-historia-mexico-i")!!.contains("materia"))
        assertNull(teacherAssignmentError(1, "A", "g1-espanol-i"))
    }
}
