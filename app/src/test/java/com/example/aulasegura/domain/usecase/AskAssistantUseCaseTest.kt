package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AiAssistant
import com.example.aulasegura.domain.repository.AiFailure
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the rules the use case exists for: the remote switch, the empty send and
 * the analytics event.
 *
 * `ai_query` is asserted on failures too — the free tier charges the attempt,
 * so a rate-limited burst has to be visible in Analytics or the dashboard will
 * claim the school uses the assistant less than it does.
 */
class AskAssistantUseCaseTest {

    @Test
    fun `a disabled assistant answers without spending a request`() = runTest {
        val assistant = FakeAssistant(enabled = false)
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = AskAssistantUseCase(assistant, analytics = { events += it })

        val answer = useCase(UserRole.TUTOR, "¿Qué tareas tiene Mariana?")

        assertEquals(AiAnswer.Failure(AiFailure.DISABLED), answer)
        assertEquals(0, assistant.calls)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a blank question never reaches the network`() = runTest {
        val assistant = FakeAssistant(enabled = true)
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = AskAssistantUseCase(assistant, analytics = { events += it })

        val answer = useCase(UserRole.TUTOR, "   ")

        assertEquals(AiAnswer.Failure(AiFailure.EMPTY_PROMPT), answer)
        assertEquals(0, assistant.calls)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the question is trimmed and the role travels with the event`() = runTest {
        val assistant = FakeAssistant(enabled = true)
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = AskAssistantUseCase(assistant, analytics = { events += it })

        val answer = useCase(UserRole.TUTOR, "  ¿Cómo justifico una falta?  ")

        assertEquals(AiAnswer.Answer("respuesta"), answer)
        assertTrue(
            assistant.lastPrompt.orEmpty()
                .contains("PREGUNTA DEL TUTOR: ¿Cómo justifico una falta?"),
        )
        assertEquals(listOf(AnalyticsEvent.AiQuery(UserRole.TUTOR)), events)
    }

    @Test
    fun `a failed query is still counted`() = runTest {
        val assistant = FakeAssistant(
            enabled = true,
            answer = AiAnswer.Failure(AiFailure.RATE_LIMITED),
        )
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = AskAssistantUseCase(assistant, analytics = { events += it })

        val answer = useCase(UserRole.TUTOR, "¿Resumen del mes?")

        assertEquals(AiAnswer.Failure(AiFailure.RATE_LIMITED), answer)
        assertEquals(listOf(AnalyticsEvent.AiQuery(UserRole.TUTOR)), events)
    }

    @Test
    fun `a provider that never answers ends in a timeout, not a spinner`() = runTest {
        // The provider can accept the connection and never respond; the screen
        // must still end in a message the tutor can act on.
        val assistant = FakeAssistant(enabled = true, answer = null)
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = AskAssistantUseCase(assistant, analytics = { events += it })

        val answer = useCase(UserRole.TUTOR, "¿Resumen del mes?")

        assertEquals(AiAnswer.Failure(AiFailure.TIMEOUT), answer)
        assertEquals(listOf(AnalyticsEvent.AiQuery(UserRole.TUTOR)), events)
    }

    private class FakeAssistant(
        private val enabled: Boolean,
        private val answer: AiAnswer? = AiAnswer.Answer("respuesta"),
    ) : AiAssistant {

        var calls: Int = 0
            private set

        var lastPrompt: String? = null
            private set

        override fun isAvailable(): Boolean = enabled

        override suspend fun ask(prompt: String): AiAnswer {
            calls++
            lastPrompt = prompt
            return answer ?: awaitCancellation()
        }
    }
}
