package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A classroom is one document per grade+section, however it was created.
 * The manual form, the QR approval and the roster assignment must all write the
 * same canonical id and the same nested subjectTeachers shape.
 */
class CanonicalGroupWriteTest {

    @Test
    fun `the manual form derives the canonical id instead of a random one`() {
        val id = canonicalGroupId(grade = 5, section = " C ")

        assertEquals("grp-5c", id)
    }

    @Test
    fun `creating the same classroom twice produces the same merge payload`() {
        val first = manualGroupDocument(grade = 5, section = "C", name = "5° C", schoolId = "sch")
        val second = manualGroupDocument(grade = 5, section = "C", name = "5° C", schoolId = "sch")

        assertEquals(first, second)
        assertFalse("teacherIds must not be reset by a repeat create", "teacherIds" in first)
        assertFalse("studentCount must not be reset by a repeat create", "studentCount" in first)
    }

    @Test
    fun `subjectTeachers is nested, never a key with dots`() {
        val payload = subjectTeachersEntry("g5-espanol-v", "docente-1")

        assertTrue(payload.containsKey("subjectTeachers"))
        assertFalse(payload.keys.any { key -> "." in key })
        assertEquals(mapOf("g5-espanol-v" to "docente-1"), payload["subjectTeachers"])
    }
}
