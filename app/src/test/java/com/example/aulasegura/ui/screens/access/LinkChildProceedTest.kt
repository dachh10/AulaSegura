package com.example.aulasegura.ui.screens.access

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A linked tutor visiting the QR to add another child must not be bounced home. */
class LinkChildProceedTest {

    @Test
    fun `a pending tutor proceeds when the director links them`() {
        assertTrue(shouldAutoProceed(wasLinkedOnEntry = false, vinculacion = "vinculado"))
    }

    @Test
    fun `an already linked tutor stays on the QR screen`() {
        assertFalse(shouldAutoProceed(wasLinkedOnEntry = true, vinculacion = "vinculado"))
    }

    @Test
    fun `a rejection never auto-proceeds`() {
        assertFalse(shouldAutoProceed(wasLinkedOnEntry = false, vinculacion = "rechazada"))
    }
}
