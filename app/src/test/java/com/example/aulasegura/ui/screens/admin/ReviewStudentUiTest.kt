package com.example.aulasegura.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The review step turns a scanned QR plus a grade/group choice into one write
 * plan. Pure so the rules (duplicate, sin grupo, group ids) are verified without
 * Firestore or a device.
 */
class ReviewStudentUiTest {

    private fun form(
        grado: String = "3",
        grupo: String = "B",
        sinGrupo: Boolean = false,
    ) = ReviewStudentForm(grado = grado, grupo = grupo, sinGrupo = sinGrupo)

    @Test
    fun `a student with no group keeps grade and group empty`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "Ana Pérez",
            form = form(grado = "3", grupo = "B", sinGrupo = true),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertEquals("", plan.grado)
        assertEquals("", plan.grupo)
        assertEquals("", plan.groupId)
        assertNull(plan.duplicateStudentId)
    }

    @Test
    fun `an assigned student carries the grade and group id`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "Ana Pérez",
            form = form(grado = "3", grupo = "B"),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertEquals("3", plan.grado)
        assertEquals("B", plan.grupo)
        assertEquals("grp-3b", plan.groupId)
    }

    @Test
    fun `an existing name is offered as a second tutor instead of a duplicate`() {
        val plan = approvalPlan(
            tutorUid = "tutor-2",
            alumno = "Luis García",
            form = form(),
            schoolId = "sch-1",
            createdBy = "directora-1",
            existing = listOf(
                ExistingStudent(name = "Luis García", studentId = "stu-2a-01", groupId = "grp-2a"),
            ),
        )

        assertEquals("stu-2a-01", plan.duplicateStudentId)
        assertFalse(plan.grupo.isBlank())
    }

    @Test
    fun `an empty name is rejected before any write`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "   ",
            form = form(),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertNull(plan.student)
        assertTrue(plan.error != null)
    }
}
