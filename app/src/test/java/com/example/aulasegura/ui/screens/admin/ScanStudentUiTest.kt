package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.repository.ParentLinkQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The director's camera must never turn a foreign code into a student. The
 * decision is pure so it can be verified without a camera or a device.
 */
class ScanStudentUiTest {

    @Test
    fun `a valid code carries the tutor and the child to the review step`() {
        val raw = ParentLinkQr.encode(uid = "tutor-42", alumno = "Ana Pérez")

        val result = ScanStudentUi.from(raw)

        assertEquals("tutor-42", result.found?.uid)
        assertEquals("Ana Pérez", result.found?.alumno)
        assertNull(result.message)
    }

    @Test
    fun `an invalid code produces a message and no data`() {
        val result = ScanStudentUi.from("{\"v\":9,\"uid\":\"x\",\"alumno\":\"Y\"}")

        assertNull(result.found)
        assertEquals("QR no válido", result.message)
    }
}
