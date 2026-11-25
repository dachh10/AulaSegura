package com.example.aulasegura.ui.screens.teacher

import org.junit.Assert.assertEquals
import org.junit.Test

/** The profile must show the configured school name, never the document id. */
class TeacherSchoolLabelTest {

    @Test
    fun `the configured name wins`() {
        assertEquals("Escuela Benito Juárez", schoolLabel("sch-benito-juarez", "Escuela Benito Juárez"))
    }

    @Test
    fun `a blank configured name falls back to the id`() {
        assertEquals("sch-benito-juarez", schoolLabel("sch-benito-juarez", "  "))
    }

    @Test
    fun `no data at all says unassigned`() {
        assertEquals("Sin asignar", schoolLabel("", null))
    }
}
