package com.example.aulasegura.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedIndexTest {
    @Test
    fun `the selected id maps to its position in the roster`() {
        assertEquals(1, indexForSelection("stu-b", listOf("stu-a", "stu-b", "stu-c")))
    }

    @Test
    fun `an unknown or missing id falls back to the first child`() {
        assertEquals(0, indexForSelection("stu-gone", listOf("stu-a", "stu-b")))
        assertEquals(0, indexForSelection(null, listOf("stu-a", "stu-b")))
    }

    @Test
    fun `an empty roster has index zero`() {
        assertEquals(0, indexForSelection("stu-a", emptyList()))
    }
}
