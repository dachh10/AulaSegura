package com.example.aulasegura.domain.telemetry

import com.example.aulasegura.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the event names and parameters against CONTEXT §9.
 *
 * Analytics has no compile-time contract: a renamed event is accepted silently
 * and only shows up as an empty dashboard weeks later. These assertions are the
 * contract, so a rename has to be a deliberate edit here too.
 */
class AnalyticsEventTest {

    @Test
    fun `event names match the documented contract`() {
        val names = listOf(
            AnalyticsEvent.LoginSuccess(UserRole.TEACHER).name,
            AnalyticsEvent.RollCallStarted.name,
            AnalyticsEvent.RollCallSaved(synced = true).name,
            AnalyticsEvent.IncidentCreated("alta", 2).name,
            AnalyticsEvent.TaskPublished.name,
            AnalyticsEvent.NoticePublished.name,
            AnalyticsEvent.ChatMessageSent.name,
            AnalyticsEvent.JustificationCreated.name,
            AnalyticsEvent.AiQuery(UserRole.TUTOR).name,
            AnalyticsEvent.SyncFailed("UPSERT_ATTENDANCE_SESSION", 2).name,
        )

        assertEquals(
            listOf(
                "login_success",
                "roll_call_started",
                "roll_call_saved",
                "incident_created",
                "task_published",
                "notice_published",
                "chat_message_sent",
                "justification_created",
                "ai_query",
                "sync_failed",
            ),
            names,
        )
    }

    @Test
    fun `login and ai query carry the firestore role value`() {
        assertEquals(
            mapOf("role" to "docente"),
            AnalyticsEvent.LoginSuccess(UserRole.TEACHER).params,
        )
        assertEquals(
            mapOf("role" to "tutor"),
            AnalyticsEvent.AiQuery(UserRole.TUTOR).params,
        )
    }

    @Test
    fun `roll call saved encodes the boolean as a long`() {
        assertEquals(
            mapOf("synced" to 1L),
            AnalyticsEvent.RollCallSaved(synced = true).params,
        )
        assertEquals(
            mapOf("synced" to 0L),
            AnalyticsEvent.RollCallSaved(synced = false).params,
        )
    }

    @Test
    fun `incident and sync failures carry their numeric parameters as longs`() {
        assertEquals(
            mapOf("severity" to "alta", "media_count" to 2L),
            AnalyticsEvent.IncidentCreated("alta", 2).params,
        )
        assertEquals(
            mapOf("op_type" to "UPSERT_ATTENDANCE_SESSION", "attempts" to 3L),
            AnalyticsEvent.SyncFailed("UPSERT_ATTENDANCE_SESSION", 3).params,
        )
    }

    @Test
    fun `every parameter is a type analytics accepts`() {
        val events = listOf(
            AnalyticsEvent.LoginSuccess(UserRole.ADMIN),
            AnalyticsEvent.RollCallSaved(synced = false),
            AnalyticsEvent.IncidentCreated("media", 1),
            AnalyticsEvent.AiQuery(UserRole.TUTOR),
            AnalyticsEvent.SyncFailed("op", 1),
        )

        events.flatMap { it.params.values }.forEach { value ->
            assertTrue(
                "unsupported parameter type ${value::class.simpleName}",
                value is String || value is Long || value is Double,
            )
        }
    }

    @Test
    fun `events without parameters send an empty map`() {
        assertTrue(AnalyticsEvent.RollCallStarted.params.isEmpty())
        assertTrue(AnalyticsEvent.TaskPublished.params.isEmpty())
        assertTrue(AnalyticsEvent.NoticePublished.params.isEmpty())
        assertTrue(AnalyticsEvent.ChatMessageSent.params.isEmpty())
        assertTrue(AnalyticsEvent.JustificationCreated.params.isEmpty())
    }
}
