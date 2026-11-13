package com.example.aulasegura.domain.telemetry

import com.example.aulasegura.domain.model.UserRole

/**
 * One business event, typed.
 *
 * The alternative — a string event name and a `Bundle` at every call site — is
 * how Analytics dashboards end up with `loginSuccess`, `login_success` and
 * `login` as three different events. Here the names live in one sealed hierarchy,
 * so a typo is a compile error and the parameter contract is visible.
 *
 * Only the events the app actually fires today are wired. The rest exist because
 * CONTEXT §9 fixes the naming, and they start emitting with the screens that
 * produce them — declaring an event nobody sends would look like coverage
 * without being it.
 *
 * Parameters are limited to what Firebase Analytics accepts: [String], [Long]
 * and [Double]. Booleans travel as `1L`/`0L` because Analytics has no boolean
 * type; see [RollCallSaved].
 */
sealed interface AnalyticsEvent {

    val name: String

    val params: Map<String, Any>

    /** Fired once per successful sign-in, with the role resolved from Firestore. */
    data class LoginSuccess(val role: UserRole) : AnalyticsEvent {
        override val name: String = "login_success"
        override val params: Map<String, Any>
            get() = mapOf("role" to role.firestoreValue)
    }

    data object RollCallStarted : AnalyticsEvent {
        override val name: String = "roll_call_started"
        override val params: Map<String, Any> get() = emptyMap()
    }

    /** [synced] tells whether the session reached Firestore in the same pass. */
    data class RollCallSaved(val synced: Boolean) : AnalyticsEvent {
        override val name: String = "roll_call_saved"
        override val params: Map<String, Any>
            get() = mapOf("synced" to if (synced) 1L else 0L)
    }

    data class IncidentCreated(val severity: String, val mediaCount: Int) : AnalyticsEvent {
        override val name: String = "incident_created"
        override val params: Map<String, Any>
            get() = mapOf(
                "severity" to severity,
                "media_count" to mediaCount.toLong(),
            )
    }

    data object TaskPublished : AnalyticsEvent {
        override val name: String = "task_published"
        override val params: Map<String, Any> get() = emptyMap()
    }

    data object NoticePublished : AnalyticsEvent {
        override val name: String = "notice_published"
        override val params: Map<String, Any> get() = emptyMap()
    }

    data object ChatMessageSent : AnalyticsEvent {
        override val name: String = "chat_message_sent"
        override val params: Map<String, Any> get() = emptyMap()
    }

    data object JustificationCreated : AnalyticsEvent {
        override val name: String = "justification_created"
        override val params: Map<String, Any> get() = emptyMap()
    }

    data class AiQuery(val role: UserRole) : AnalyticsEvent {
        override val name: String = "ai_query"
        override val params: Map<String, Any>
            get() = mapOf("role" to role.firestoreValue)
    }

    /** [attempts] is the attempt count *including* the one that just failed. */
    data class SyncFailed(val opType: String, val attempts: Int) : AnalyticsEvent {
        override val name: String = "sync_failed"
        override val params: Map<String, Any>
            get() = mapOf(
                "op_type" to opType,
                "attempts" to attempts.toLong(),
            )
    }
}
