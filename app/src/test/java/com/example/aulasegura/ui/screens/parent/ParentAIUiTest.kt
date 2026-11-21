package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AssistantChild
import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.model.AssistantNotice
import com.example.aulasegura.domain.model.AssistantTask
import com.example.aulasegura.domain.model.AssistantToday
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The FAQ is what the tutor sees when the assistant cannot answer, so it is
 * pinned like copy: real data, no invented child, and the same sentence the chip
 * shows.
 */
class ParentAIUiTest {

    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")
    private val now = at(2026, 11, 19)

    @Test
    fun `the greeting uses the tutor's first name and the children they have`() {
        val welcome = assistantWelcome("Diego Corral Hernández", listOf("Mariana", "Diego"))

        assertEquals(
            "¡Hola, Diego! Soy tu asistente escolar. Puedo ayudarte con tareas, faltas y avisos de Mariana y Diego",
            welcome,
        )
    }

    @Test
    fun `without a name or children nothing is invented`() {
        val welcome = assistantWelcome("", emptyList())

        assertFalse(welcome.contains("Hola,"))
        assertTrue(welcome.contains("Soy tu asistente escolar"))
        assertFalse(welcome.contains("Mariana"))
    }

    @Test
    fun `the tasks suggestion answers with tomorrow's real tasks`() {
        val answer = assistantFaqAnswer(
            question = ParentAIQuestion.TASKS,
            context = context(),
            now = now,
            zone = zone,
        )

        assertTrue(answer.contains("Mariana: Matemáticas — Fracciones"))
        assertFalse("a task due later is not due tomorrow", answer.contains("Español"))
    }

    @Test
    fun `the tasks suggestion with nothing due says so instead of inventing`() {
        val answer = assistantFaqAnswer(
            question = ParentAIQuestion.TASKS,
            context = context(tasks = emptyList()),
            now = now,
            zone = zone,
        )

        assertEquals("No hay tareas registradas para mañana.", answer)
    }

    @Test
    fun `the month suggestion prints the real counters`() {
        val answer = assistantFaqAnswer(
            question = ParentAIQuestion.MONTH,
            context = context(),
            now = now,
            zone = zone,
        )

        assertTrue(answer.contains("Mariana: 3 asistencias (1 retardo), 1 falta, 0 justificadas de 4"))
    }

    @Test
    fun `the justify suggestion names the real steps of the app`() {
        val answer = assistantFaqAnswer(
            question = ParentAIQuestion.JUSTIFY,
            context = context(),
            now = now,
            zone = zone,
        )

        assertTrue(answer.contains("«Justificar»"))
        assertTrue(answer.contains("adjunta el comprobante"))
        assertTrue(answer.contains("envía el justificante"))
    }

    @Test
    fun `without context the FAQ says the read failed`() {
        val answer = assistantFaqAnswer(
            question = ParentAIQuestion.MONTH,
            context = null,
            now = now,
            zone = zone,
        )

        assertTrue(answer.contains("No pude leer los datos de tus hijos"))
    }

    @Test
    fun `only the tasks suggestion yields openable cards`() {
        val tasks = assistantTasksFor(ParentAIQuestion.TASKS, context(), now, zone)

        assertEquals(listOf("task-tomorrow"), tasks.map { it.taskId })
        assertEquals("Matemáticas", tasks.single().subject)
        assertTrue(assistantTasksFor(ParentAIQuestion.MONTH, context(), now, zone).isEmpty())
    }

    private fun context(tasks: List<AssistantTask>? = null) = AssistantContext(
        children = listOf(
            AssistantChild(
                fullName = "Mariana Gómez Ruiz",
                firstName = "Mariana",
                groupLabel = "4° D",
                attendance = ParentAttendanceCounts(present = 2, late = 1, absent = 1, justified = 0),
                today = AssistantToday(mark = AttendanceMark.PRESENT, time = "07:58"),
                pendingTasks = tasks ?: listOf(
                    AssistantTask(
                        taskId = "task-tomorrow",
                        subject = "Matemáticas",
                        title = "Fracciones",
                        dueDateMillis = at(2026, 11, 20),
                    ),
                    AssistantTask(
                        taskId = "task-later",
                        subject = "Español",
                        title = "Ensayo",
                        dueDateMillis = at(2026, 11, 25),
                    ),
                ),
            ),
        ),
        notices = listOf(
            AssistantNotice(
                title = "Aviso de la escuela",
                body = "El viernes no hay clases.",
                urgent = false,
                createdAtMillis = at(2026, 11, 18),
            ),
        ),
        incidents = emptyList(),
    )

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, 12, 0)
    }.timeInMillis
}
