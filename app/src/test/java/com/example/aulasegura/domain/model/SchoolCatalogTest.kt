package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The fixed grade/section catalog and the deterministic group id it produces. */
class SchoolCatalogTest {

    @Test
    fun `grades are one to six and sections A to D`() {
        assertEquals((1..6).toList(), AvailableGrades)
        assertEquals(listOf("A", "B", "C", "D"), AvailableSections)
    }

    @Test
    fun `the group id is deterministic from grade and section`() {
        assertEquals("grp-3b", groupIdFor(3, "B"))
        assertEquals("grp-1a", groupIdFor(1, "A"))
        assertEquals("grp-6d", groupIdFor(6, "D"))
    }

    @Test
    fun `the group name is canonical`() {
        assertEquals("3° B", groupNameFor(3, "B"))
        assertEquals("1° A", groupNameFor(1, "a"))
    }

    @Test
    fun `invalid grades or sections are rejected before any write`() {
        assertFalse(isValidGrade(0))
        assertFalse(isValidGrade(7))
        assertTrue(isValidGrade(3))
        assertFalse(isValidSection("E"))
        assertFalse(isValidSection(""))
        assertTrue(isValidSection("C"))
    }
}
