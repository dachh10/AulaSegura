package com.example.aulasegura.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The rules use a blacklist of immutable fields, not a whitelist of editable
 * ones. Sending only name and subjects is what keeps a profile edit from
 * overwriting a role or school change made by the director at the same time.
 */
class ProfileUpdateMapperTest {

    @Test
    fun `a profile edit sends only name and subjects`() {
        val update = profileUpdate(
            fullName = "Ricardo Salinas",
            subjects = listOf("Matemáticas", "Ciencias"),
        )

        assertEquals(
            setOf("fullName", "subjects"),
            update.keys,
        )
        assertEquals("Ricardo Salinas", update["fullName"])
        assertEquals(listOf("Matemáticas", "Ciencias"), update["subjects"])
    }

    @Test
    fun `a profile edit never carries identity fields`() {
        val update = profileUpdate(fullName = "Ricardo", subjects = emptyList())

        assertFalse(update.containsKey("email"))
        assertFalse(update.containsKey("role"))
        assertFalse(update.containsKey("schoolId"))
        assertFalse(update.containsKey("active"))
        assertEquals(emptyList<String>(), update["subjects"])
    }
}
