package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.repository.AdminGroupFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Adding students must not unassign the teachers the director already approved
 * for the classroom. The write used to merge `newGroupDocument()`, whose empty
 * `teacherIds` overwrote the real ones on every roster change.
 */
class AssignGroupWriteTest {

    @Test
    fun `an existing group keeps its teacher links`() {
        val fields = assignmentGroupFields(
            groupExists = true,
            name = "1° A",
            grade = 1,
            section = "A",
            schoolId = "sch-benito-juarez",
            newStudents = 2,
        )

        assertFalse("teacherIds must not be written for an existing group", AdminGroupFields.TEACHER_IDS in fields)
        assertEquals(Increment(2), fields[AdminGroupFields.STUDENT_COUNT])
        assertEquals("1° A", fields[AdminGroupFields.NAME])
        assertEquals("A", fields[AdminGroupFields.SECTION])
        assertEquals("sch-benito-juarez", fields[AdminGroupFields.SCHOOL_ID])
        assertEquals(AdminGroupFields.DEFAULT_CYCLE, fields[AdminGroupFields.CYCLE])
    }

    @Test
    fun `a new group starts with no teachers`() {
        val fields = assignmentGroupFields(
            groupExists = false,
            name = "1° A",
            grade = 1,
            section = "A",
            schoolId = "sch-benito-juarez",
            newStudents = 1,
        )

        assertEquals(emptyList<String>(), fields[AdminGroupFields.TEACHER_IDS])
        assertTrue(AdminGroupFields.TEACHER_IDS in fields)
        assertEquals(Increment(1), fields[AdminGroupFields.STUDENT_COUNT])
    }

    @Test
    fun `the legacy titular field is never written`() {
        val fields = assignmentGroupFields(false, "1° A", 1, "A", "sch", 1)
        assertFalse("titularTeacherId" in fields)
    }
}
