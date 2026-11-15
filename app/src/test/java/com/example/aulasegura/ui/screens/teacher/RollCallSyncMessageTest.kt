package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.RollCallSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The strip under the counters is the only place the teacher learns whether the
 * session left the device. These tests fix two things that would otherwise drift:
 * every phase has a distinct message, and "delivered" means Firestore has it.
 */
class RollCallSyncMessageTest {

    @Test
    fun `nothing saved yet leaves the strip to the validation messages`() {
        assertNull(rollCallSyncMessage(null))
    }

    @Test
    fun `a queued session says pending and not synced`() {
        val message = requireNotNull(rollCallSyncMessage(RollCallSync.PENDING))

        assertTrue(message.contains("Pendiente de sincronizar"))
        assertFalse(message.contains("sincronizada."))
    }

    @Test
    fun `a session in flight says it is syncing`() {
        assertEquals("Sincronizando la asistencia…", rollCallSyncMessage(RollCallSync.SYNCING))
    }

    @Test
    fun `a synced session is the only one reported as synced`() {
        assertEquals(
            "Asistencia guardada y sincronizada.",
            rollCallSyncMessage(RollCallSync.SYNCED),
        )
        assertTrue(RollCallSync.SYNCED.delivered)
    }

    @Test
    fun `a failed sync still tells the teacher the session is safe locally`() {
        val message = requireNotNull(rollCallSyncMessage(RollCallSync.FAILED))

        assertTrue(message.contains("guardada en el dispositivo"))
        assertTrue(message.contains("se reintenta"))
        assertFalse(RollCallSync.FAILED.delivered)
    }
}
