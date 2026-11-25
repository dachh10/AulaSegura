package com.example.aulasegura.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Test

/** The director's group list must offer every grade of the school catalog. */
class AdminGroupGradeFilterTest {

    @Test
    fun `all six grades have a chip`() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            AdminGroupGradeFilter.entries.mapNotNull { it.grade },
        )
    }

    @Test
    fun `all grades plus the all chip are offered`() {
        assertEquals(7, AdminGroupGradeFilter.entries.size)
    }
}
