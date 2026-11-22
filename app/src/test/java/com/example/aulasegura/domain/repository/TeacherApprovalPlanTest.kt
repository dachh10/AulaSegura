package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TeacherApprovalPlanTest {

    @Test
    fun `a director created teacher stores an intent instead of direct group links`() {
        val document = AdminUserFields.newAccountDocument(
            uid = "teacher-1",
            email = "t@aulasegura.com",
            firstName = "Ana",
            lastName = "Pérez",
            role = AccountRole.DOCENTE,
            phone = "",
            subjects = listOf("Matemáticas"),
            groupIds = listOf("grp-3a"),
            schoolId = "sch-test",
        )

        assertEquals("grp-3a", document[AdminUserFields.REQUESTED_GROUP_ID])
        assertEquals("g3-matematicas-iii", document[AdminUserFields.REQUESTED_SUBJECT])
        assertEquals("pendiente", document[AdminUserFields.ESTADO])
        assertFalse(document.containsKey(AdminUserFields.GROUP_IDS))
    }

    @Test
    fun `a free subject produces the assignment plan`() {
        val plan = teacherApprovalPlan("grp-3a", "g3-matematicas-iii", "teacher-1", emptyMap())

        assertEquals("grp-3a", plan?.groupId)
        assertEquals("g3-matematicas-iii", plan?.subjectId)
        assertEquals("Matemáticas III", plan?.label)
    }

    @Test
    fun `a subject from another grade is never approved`() {
        assertNull(teacherApprovalPlan("grp-1a", "g4-historia-mexico-i", "teacher-1", emptyMap()))
    }

    @Test
    fun `a subject taken by another teacher cannot be approved`() {
        assertNull(
            teacherApprovalPlan(
                "grp-3a", "g3-matematicas-iii", "teacher-1",
                mapOf("g3-matematicas-iii" to "teacher-2"),
            ),
        )
    }

    @Test
    fun `an invalid or missing intent cannot be approved`() {
        assertNull(teacherApprovalPlan(null, "g3-matematicas-iii", "teacher-1", emptyMap()))
        assertNull(teacherApprovalPlan("grp-3a", "musica", "teacher-1", emptyMap()))
    }
}
