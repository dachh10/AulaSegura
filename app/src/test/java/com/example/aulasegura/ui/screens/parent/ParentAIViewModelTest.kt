package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AssistantChild
import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.model.AssistantTask
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AiFailure
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.usecase.AssistantContextResult
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The state machine of the assistant screen.
 *
 * The use cases and the prompt already have their own tests; what is pinned here
 * is the behaviour the tutor feels: the question never disappears on a failure,
 * the free text is closed when only the FAQ can answer, and a rate limit does not
 * silently downgrade the whole session to predefined answers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentAIViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")
    private val now = at(2026, 11, 19)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading the children fills the greeting with real names`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertTrue(state.welcome.contains("¡Hola, Diego!"))
        assertTrue(state.welcome.contains("Mariana"))
        assertNull(state.contextError)
    }

    @Test
    fun `a question reaches the model with the loaded context and renders the answer`() =
        runTest(dispatcher) {
            val asked = mutableListOf<Pair<String, AssistantContext?>>()
            val viewModel = viewModel(
                ask = { question, context ->
                    asked += question to context
                    AiAnswer.Answer("Tiene Matemáticas para mañana.")
                },
            )
            advanceUntilIdle()

            viewModel.setDraft("¿Qué tareas tiene mañana?")
            viewModel.send()
            advanceUntilIdle()

            assertEquals(1, asked.size)
            assertEquals("¿Qué tareas tiene mañana?", asked.single().first)
            assertNotNull("the data travels with the question", asked.single().second)

            val messages = viewModel.state.value.messages
            assertEquals(listOf(true, false), messages.map { it.mine })
            assertEquals("Tiene Matemáticas para mañana.", messages.last().text)
            assertTrue(viewModel.state.value.draft.isEmpty())
        }

    @Test
    fun `the tasks suggestion opens a card only when the app knows the task`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                ask = { _, _ -> AiAnswer.Answer("Mañana tienes Matemáticas.") },
            )
            advanceUntilIdle()

            viewModel.ask(ParentAIQuestion.TASKS)
            advanceUntilIdle()

            val answer = viewModel.state.value.messages.last()
            assertEquals(listOf("task-tomorrow"), answer.tasks.map { it.taskId })
        }

    @Test
    fun `a failed question keeps the text and offers the retry`() = runTest(dispatcher) {
        val viewModel = viewModel(
            ask = { _, _ -> AiAnswer.Failure(AiFailure.RATE_LIMITED) },
        )
        advanceUntilIdle()

        viewModel.setDraft("¿Cómo va Mariana?")
        viewModel.send()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AiFailure.RATE_LIMITED.userMessage, state.notice)
        assertEquals("¿Cómo va Mariana?", state.draft)
        assertFalse("a rate limit is not a permanent downgrade", state.faqOnly)
    }

    @Test
    fun `a disabled assistant answers the suggestion with the FAQ and closes free text`() =
        runTest(dispatcher) {
            var asked = 0
            val viewModel = viewModel(
                available = false,
                ask = { _, _ ->
                    asked++
                    AiAnswer.Answer("no debería llegar")
                },
            )
            advanceUntilIdle()

            assertTrue(viewModel.state.value.faqOnly)

            viewModel.ask(ParentAIQuestion.MONTH)
            advanceUntilIdle()

            assertEquals("the model is never called", 0, asked)
            assertTrue(viewModel.state.value.messages.last().text.contains("Mariana:"))
        }

    @Test
    fun `free text with only the FAQ asks for a suggestion instead of inventing`() =
        runTest(dispatcher) {
            val viewModel = viewModel(available = false)
            advanceUntilIdle()

            viewModel.setDraft("¿Quién es el director?")
            viewModel.send()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("El asistente no está disponible. Puedes usar las sugerencias.", state.notice)
            assertTrue(state.messages.none { it.mine })
        }

    @Test
    fun `a read failure is shown with a retry and blocks the send`() = runTest(dispatcher) {
        val viewModel = viewModel(
            load = { AssistantContextResult.Failed(RosterFailure.OFFLINE) },
        )
        advanceUntilIdle()

        assertEquals("Sin conexión. Revisa tu red e intenta de nuevo.", viewModel.state.value.contextError)

        viewModel.setDraft("¿Qué tareas hay?")
        viewModel.send()
        advanceUntilIdle()

        assertEquals("the question is not sent without data", 0, viewModel.state.value.messages.size)
    }

    @Test
    fun `a tutor without children is told, and the assistant does not promise one`() =
        runTest(dispatcher) {
            val viewModel = viewModel(load = { AssistantContextResult.NoChildren })
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("Tu cuenta todavía no tiene alumnos vinculados.", state.contextError)
            assertFalse(state.welcome.contains("Mariana"))
        }

    private fun viewModel(
        available: Boolean = true,
        load: suspend () -> AssistantContextResult = { AssistantContextResult.Loaded(context()) },
        ask: suspend (String, AssistantContext?) -> AiAnswer = { _, _ ->
            AiAnswer.Answer("Respuesta")
        },
    ) = ParentAIViewModel(
        currentUserName = { "Diego Corral Hernández" },
        loadContext = load,
        askAssistant = ask,
        isAvailable = { available },
        now = { now },
        zone = zone,
    )

    private fun context() = AssistantContext(
        children = listOf(
            AssistantChild(
                fullName = "Mariana Gómez Ruiz",
                firstName = "Mariana",
                groupLabel = "4° D",
                attendance = ParentAttendanceCounts(present = 2, late = 1, absent = 1),
                today = null,
                pendingTasks = listOf(
                    AssistantTask(
                        taskId = "task-tomorrow",
                        subject = "Matemáticas",
                        title = "Fracciones",
                        dueDateMillis = at(2026, 11, 20),
                    ),
                ),
            ),
        ),
        notices = emptyList(),
        incidents = emptyList(),
    )

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, 12, 0)
    }.timeInMillis
}
