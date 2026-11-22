package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.MAX_ROSTER_SIZE

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApprovedGroupTest {

    private fun input(grado: String, grupo: String) = ApprovedStudentInput(
        alumno = "Ana Pérez",
        tutorUid = "tutor-1",
        schoolId = "sch-1",
        createdBy = "directora",
        grado = grado,
        grupo = grupo,
        groupId = "",
    )

    @Test
    fun `a valid grade and section resolve to the deterministic classroom`() {
        val group = resolveApprovedGroup(input("3", "b"))

        assertEquals("grp-3b", group?.groupId)
        assertEquals("3° B", group?.name)
        assertEquals(3, group?.grade)
    }

    @Test
    fun `invalid grades and sections have no canonical group`() {
        assertNull(resolveApprovedGroup(input("7", "A")))
        assertNull(resolveApprovedGroup(input("3", "E")))
        assertNull(resolveApprovedGroup(input("", "A")))
    }

    @Test
    fun `the next roster number skips used slots and respects the limit`() {
        assertEquals(1, nextListNumberFrom(emptyList()))
        assertEquals(4, nextListNumberFrom(listOf(1, 3)))
        assertNull(nextListNumberFrom((1..MAX_ROSTER_SIZE).toList()))
    }
}
