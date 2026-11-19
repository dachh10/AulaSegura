package com.example.aulasegura.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mapping one `students/{id}` document into the tutor's child.
 *
 * The regression this file pins: a student deactivated in the school roster kept
 * the link in `tutorIds`, but the family screen said "no child linked" because
 * the mapper dropped `active = false`.
 */
class ParentChildMappingTest {

    @Test
    fun `a deactivated student is still the tutor's child`() {
        val child = parentChildFrom(
            id = "stu-1",
            data = mapOf(
                "fullName" to "Diego Corral Hernández",
                "firstName" to "Diego",
                "listNumber" to 1,
                "groupId" to "grp-4d",
                "schoolId" to "sch-1",
                "active" to false,
                "tutorIds" to listOf("uid-1"),
            ),
        )

        assertEquals("stu-1", child?.studentId)
        assertEquals("Diego Corral Hernández", child?.fullName)
        assertEquals("grp-4d", child?.groupId)
        assertEquals(1, child?.listNumber)
    }

    @Test
    fun `fullName is rebuilt from first and last name when missing`() {
        val child = parentChildFrom(
            id = "stu-2",
            data = mapOf(
                "firstName" to "Mariana",
                "lastName" to "Gómez",
                "groupId" to "grp-3b",
            ),
        )

        assertEquals("Mariana Gómez", child?.fullName)
        assertEquals("Mariana", child?.firstName)
    }

    @Test
    fun `firstName falls back to the first word of the full name`() {
        val child = parentChildFrom(
            id = "stu-3",
            data = mapOf(
                "fullName" to "Carlos Gómez",
                "groupId" to "grp-4d",
            ),
        )

        assertEquals("Carlos", child?.firstName)
        assertEquals(0, child?.listNumber)
    }

    @Test
    fun `a student without a group cannot be placed on a screen`() {
        assertNull(parentChildFrom("stu-4", mapOf("fullName" to "Sin Grupo")))
    }

    @Test
    fun `a student without a name is not rendered as an empty row`() {
        assertNull(parentChildFrom("stu-5", mapOf("groupId" to "grp-4d")))
        assertNull(
            parentChildFrom(
                "stu-6",
                mapOf("groupId" to "grp-4d", "firstName" to "  ", "lastName" to " "),
            ),
        )
    }
}
