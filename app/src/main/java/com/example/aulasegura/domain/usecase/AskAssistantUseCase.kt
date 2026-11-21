package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.model.assistantPrompt
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AiAssistant
import com.example.aulasegura.domain.repository.AiFailure
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import java.util.TimeZone
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * The one way a screen reaches the assistant.
 *
 * Five things happen here and nowhere else:
 * 1. The remote flag is checked, so switching the assistant off is an app-wide
 *    kill switch rather than a UI-only hide.
 * 2. The question is trimmed and an empty send is refused before it costs a
 *    request.
 * 3. `ai_query` is logged. It is logged *before* the call, not after: the free
 *    tier charges the attempt, so a burst of rate-limited questions is exactly
 *    what the counter has to show. Logging only successes would hide the day a
 *    tutor hit the limit.
 * 4. The prompt is assembled with [assistantPrompt]: the data the screen loaded
 *    and the tutor's text. The screen never builds a prompt, so what leaves the
 *    device is decided in one place with its rules next to it.
 * 5. The whole call is capped. A provider that accepts the connection and never
 *    answers is a real case, and the screen must end in a message, not a spinner.
 */
class AskAssistantUseCase(
    private val assistant: AiAssistant,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
    private val zone: TimeZone = TimeZone.getDefault(),
) {

    /** Whether the tutor-facing assistant may answer right now (Remote Config). */
    fun isAvailable(): Boolean = assistant.isAvailable()

    suspend operator fun invoke(
        role: UserRole,
        question: String,
        context: AssistantContext? = null,
    ): AiAnswer {
        if (!assistant.isAvailable()) {
            return AiAnswer.Failure(AiFailure.DISABLED)
        }

        val trimmed = question.trim()
        if (trimmed.isEmpty()) {
            return AiAnswer.Failure(AiFailure.EMPTY_PROMPT)
        }

        analytics.log(AnalyticsEvent.AiQuery(role))
        val prompt = assistantPrompt(context = context, question = trimmed, zone = zone)

        return try {
            withTimeout(ANSWER_BUDGET_MILLIS) { assistant.ask(prompt) }
        } catch (timedOut: TimeoutCancellationException) {
            // The provider can accept the connection and never send a response -
            // observed, not theoretical. Without this ceiling the tutor's spinner
            // never stops, and cancelling here also cancels the in-flight request.
            AiAnswer.Failure(AiFailure.TIMEOUT)
        }
    }

    private companion object {
        /**
         * How long the user waits before being told to retry.
         *
         * Deliberately in the domain and not only in the SDK: a transport
         * timeout is cooperative, this one is guaranteed by the coroutine.
         */
        const val ANSWER_BUDGET_MILLIS = 60_000L
    }
}
