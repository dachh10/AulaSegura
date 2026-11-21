package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The QR is the only data that crosses from the tutor's phone to the director's
 * phone. Its format is a contract with the scanner (T3), so it is tested as a
 * plain string before any bitmap or camera is involved.
 */
class ParentLinkQrTest {

    @Test
    fun `the payload is the documented json with version first`() {
        assertEquals(
            """{"v":1,"uid":"tutor-42","alumno":"Ana Pérez"}""",
            ParentLinkQr.encode(uid = "tutor-42", alumno = "Ana Pérez"),
        )
    }

    @Test
    fun `a valid payload round trips`() {
        val raw = ParentLinkQr.encode(uid = "tutor-42", alumno = "Ana Pérez")

        val decoded = ParentLinkQr.decode(raw)

        assertEquals(1, decoded?.v)
        assertEquals("tutor-42", decoded?.uid)
        assertEquals("Ana Pérez", decoded?.alumno)
    }

    @Test
    fun `a foreign or malformed code is rejected without throwing`() {
        assertNull(ParentLinkQr.decode("not json"))
        assertNull(ParentLinkQr.decode("""{"v":2,"uid":"u","alumno":"A"}"""))
        assertNull(ParentLinkQr.decode("""{"v":1,"uid":"","alumno":"A"}"""))
        assertNull(ParentLinkQr.decode("""{"v":1,"uid":"u","alumno":"  "}"""))
        assertNull(ParentLinkQr.decode("""{"uid":"u","alumno":"A"}"""))
    }
}
