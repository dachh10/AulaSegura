package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A teacher's subject belongs to the group, not to their profile. The profile
 * list is a projection of all the slots the teacher holds across groups.
 */
class TeacherSubjectProjectionTest {

    private val grade1 = mapOf("g1-espanol-i" to "docente-1", "g1-matematicas-i" to "docente-2")
    private val grade5 = mapOf("g5-espanol-v" to "docente-1")

    @Test
    fun `a subject already taken in the group is not offered`() {
        val free = unassignedSubjectsForGrade(grade = 1, subjectTeachers = grade1)

        assertFalse(free.any { it.id == "g1-espanol-i" })
        assertTrue(free.any { it.id == "g1-exploracion-naturaleza" })
    }

    @Test
    fun `the profile is the union of the teacher's subjects with their group`() {
        val projection = teacherSubjectProjection(
            mapOf("grp-1a" to grade1, "grp-5c" to grade5),
            teacherUid = "docente-1",
        )

        assertEquals(2, projection.size)
        assertEquals("Español I", projection.first { it.groupId == "grp-1a" }.label)
        assertEquals("Español V", projection.first { it.groupId == "grp-5c" }.label)
    }

    @Test
    fun `a teacher with no slots has an empty projection`() {
        assertTrue(teacherSubjectProjection(mapOf("grp-1a" to grade1), "otro").isEmpty())
    }

    @Test
    fun `deassigning frees the subject only in that group`() {
        val updated = withoutTeacher(grade1, "docente-2")

        assertFalse("g1-matematicas-i" in updated)
        assertEquals("docente-1", updated["g1-espanol-i"])
    }
}
