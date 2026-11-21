package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.TeacherRequestFilter
import com.example.aulasegura.domain.repository.acceptAllTeacherUids
import com.example.aulasegura.domain.repository.pendingTeacherCount
import com.example.aulasegura.domain.repository.teacherRequests
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure rules of the director's teacher-request queue. */
class TeacherRequestUiTest {

    private fun teacher(
        uid: String,
        estado: String,
        verified: Boolean,
        fechaSolicitud: Long,
    ) = AdminUser(
        uid = uid,
        email = "$uid@escuela.mx",
        role = UserRole.TEACHER,
        firstName = uid,
        lastName = "Apellido",
        fullName = "$uid Apellido",
        phone = "",
        schoolId = "sch-1",
        subjects = listOf("Matemáticas"),
        groupIds = emptyList(),
        studentIds = emptyList(),
        active = true,
        emailVerified = verified,
        fechaSolicitud = fechaSolicitud,
        estado = estado,
    )

    private val all = listOf(
        teacher("pendiente-viejo", "pendiente", true, 100L),
        teacher("pendiente-nuevo", "pendiente", true, 300L),
        teacher("sin-verificar", "pendiente", false, 200L),
        teacher("aprobado", "aprobado", true, 50L),
        teacher("rechazado", "rechazado", true, 60L),
    )

    @Test
    fun `only verified pending teachers are in the queue, newest first`() {
        val queue = teacherRequests(all, TeacherRequestFilter.PENDIENTES)

        assertEquals(listOf("pendiente-nuevo", "pendiente-viejo"), queue.map { it.uid })
    }

    @Test
    fun `the pending count ignores unverified and decided teachers`() {
        assertEquals(2, pendingTeacherCount(all))
    }

    @Test
    fun `accept all targets exactly the verified pending uids`() {
        assertEquals(
            setOf("pendiente-viejo", "pendiente-nuevo"),
            acceptAllTeacherUids(all).toSet(),
        )
    }

    @Test
    fun `filters separate decided teachers`() {
        assertEquals(
            listOf("aprobado"),
            teacherRequests(all, TeacherRequestFilter.ACEPTADAS).map { it.uid },
        )
        assertEquals(
            listOf("rechazado"),
            teacherRequests(all, TeacherRequestFilter.RECHAZADAS).map { it.uid },
        )
        assertTrue(teacherRequests(all, TeacherRequestFilter.PENDIENTES).all { it.emailVerified })
        assertFalse(teacherRequests(all, TeacherRequestFilter.PENDIENTES).isEmpty())
    }
}
