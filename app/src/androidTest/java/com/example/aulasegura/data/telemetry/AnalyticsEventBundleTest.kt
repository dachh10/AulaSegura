package com.example.aulasegura.data.telemetry

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Bundle conversion is the last step before Firebase decides whether to keep
 * a parameter, and it drops unsupported types without failing. That silence is
 * exactly why these types are asserted on a device rather than assumed.
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsEventBundleTest {

    @Test
    fun booleansBecomeLongs() {
        val bundle = AnalyticsEvent.RollCallSaved(synced = true).params.toEventParamsBundle()

        assertTrue(
            "Firebase drops a Boolean parameter; it must travel as Long",
            bundle.get("synced") is Long,
        )
        assertEquals(1L, bundle.getLong("synced"))
    }

    @Test
    fun aFailedSyncEncodesZeroNotFalse() {
        val bundle = AnalyticsEvent.RollCallSaved(synced = false).params.toEventParamsBundle()

        assertEquals(0L, bundle.getLong("synced"))
    }

    @Test
    fun stringsStayStrings() {
        val bundle = AnalyticsEvent.LoginSuccess(
            com.example.aulasegura.domain.model.UserRole.TEACHER,
        ).params.toEventParamsBundle()

        assertEquals("docente", bundle.getString("role"))
    }

    @Test
    fun intsAreWidenedToLong() {
        val bundle = AnalyticsEvent.IncidentCreated("alta", 2).params.toEventParamsBundle()

        assertEquals(2L, bundle.getLong("media_count"))
        assertEquals("alta", bundle.getString("severity"))
    }

    @Test
    fun emptyEventsProduceAnEmptyBundle() {
        assertEquals(0, AnalyticsEvent.TaskPublished.params.toEventParamsBundle().size())
    }
}
