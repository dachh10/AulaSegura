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
        assertEquals("matematicas", document[AdminUserFields.REQUESTED_SUBJECT])
        assertEquals("pendiente", document[AdminUserFields.ESTADO])
        assertFalse(document.containsKey(AdminUserFields.GROUP_IDS))
    }

    @Test
    fun `a free subject produces the assignment plan`() {
        val plan = teacherApprovalPlan("grp-3a", "matematicas", "teacher-1", emptyMap())

        assertEquals("grp-3a", plan?.groupId)
        assertEquals("matematicas", plan?.subjectId)
        assertEquals("Matemáticas", plan?.label)
    }

    @Test
    fun `a subject taken by another teacher cannot be approved`() {
        assertNull(
            teacherApprovalPlan(
                "grp-3a", "matematicas", "teacher-1",
                mapOf("matematicas" to "teacher-2"),
            ),
        )
    }

    @Test
    fun `an invalid or missing intent cannot be approved`() {
        assertNull(teacherApprovalPlan(null, "matematicas", "teacher-1", emptyMap()))
        assertNull(teacherApprovalPlan("grp-3a", "musica", "teacher-1", emptyMap()))
    }
}
