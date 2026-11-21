package com.example.aulasegura.ui.screens.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The QR screen behaves differently on first linking vs adding another child. */
class LinkChildModeTest {

    @Test
    fun `add mode starts with an empty name, never the previous child`() {
        val state = linkInitialState(LinkChildMode.ADD, profileChildName = "Ana Pérez")

        assertEquals("", state.childName)
        assertFalse(state.showQr)
        assertTrue(state.mode.title.contains("Agregar"))
    }

    @Test
    fun `pending mode uses the name already saved at registration`() {
        val state = linkInitialState(LinkChildMode.PENDING, profileChildName = "Ana Pérez")

        assertEquals("Ana Pérez", state.childName)
        assertTrue(state.showQr)
        assertFalse(state.mode.title.contains("Agregar"))
    }

    @Test
    fun `the QR appears in add mode only after a name is entered and generated`() {
        var state = linkInitialState(LinkChildMode.ADD, profileChildName = "Ana Pérez")

        state = onNameEntered(state, "Luis García")
        assertFalse("typing alone must not show the QR", state.showQr)

        state = onGenerateQr(state)
        assertTrue(state.showQr)
        assertEquals("Luis García", state.childName)
    }

    @Test
    fun `generating with a blank name does nothing`() {
        val state = linkInitialState(LinkChildMode.ADD, profileChildName = "")

        val result = onGenerateQr(state)

        assertFalse(result.showQr)
    }
}
