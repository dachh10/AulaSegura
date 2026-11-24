package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.repository.RosterFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A brand-new student has roll-call sessions in the group but no record of
 * their own yet. That is an empty month, not a failed load — the difference is
 * what the tutor sees as "sin registros" instead of "No se pudo cargar".
 */
class ParentRecordReadTest {

    @Test
    fun `sessions without records for the child are not a load failure`() {
        assertNull(firstRecordFailure(listOf(RecordRead.Missing, RecordRead.Missing)))
    }

    @Test
    fun `found records mixed with missing ones are not a failure`() {
        assertNull(firstRecordFailure(listOf(RecordRead.Found("a"), RecordRead.Missing)))
    }

    @Test
    fun `a denied read keeps its real reason instead of a generic error`() {
        assertEquals(
            RosterFailure.DENIED,
            firstRecordFailure(
                listOf(RecordRead.Missing, RecordRead.Failed(RosterFailure.DENIED)),
            ),
        )
    }

    @Test
    fun `an offline read keeps the offline reason`() {
        assertEquals(
            RosterFailure.OFFLINE,
            firstRecordFailure(listOf(RecordRead.Failed(RosterFailure.OFFLINE))),
        )
    }
}
