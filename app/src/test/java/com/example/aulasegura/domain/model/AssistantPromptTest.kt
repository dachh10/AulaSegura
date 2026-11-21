package com.example.aulasegura.domain.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prompt is the privacy boundary of the assistant, so these tests read it the
 * way the model would: what is there, what is missing, and what never leaves the
 * device.
 */
class AssistantPromptTest {

    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")

    @Test
    fun `the prompt carries the children data in the tutor's words`() {
        val prompt = assistantPrompt(context(), "¿Qué tareas tiene mañana?", zone)

        assertTrue(prompt.contains("Mariana Gómez Ruiz"))
        assertTrue(prompt.contains("4° D"))
        assertTrue(prompt.contains("Asistencia de los últimos 30 días: 3 asistencias"))
        assertTrue(prompt.contains("Matemáticas: Fracciones — vence"))
        assertTrue(prompt.contains("Aviso de la escuela"))
        assertTrue(prompt.contains("Caída en el patio"))
        assertTrue(prompt.contains("PREGUNTA DEL TUTOR: ¿Qué tareas tiene mañana?"))
    }

    @Test
    fun `no identifier travels with the prompt`() {
        val prompt = assistantPrompt(context(), "¿Cómo va mi hija?", zone)

        assertFalse("the task id is the screen's business", prompt.contains(TASK_ID))
        assertFalse("the student id never reaches the model", prompt.contains(STUDENT_ID))
        assertFalse("the group id is replaced by its human name", prompt.contains(GROUP_ID))
    }

    @Test
    fun `a missing datum says sin registro instead of inventing one`() {
        val emptyChild = AssistantChild(
            fullName = "Diego Corral",
            firstName = "Diego",
            groupLabel = "5° A",
            attendance = ParentAttendanceCounts(),
            today = null,
            pendingTasks = emptyList(),
        )

        val prompt = assistantPrompt(
            AssistantContext(children = listOf(emptyChild), notices = emptyList(), incidents = emptyList()),
            "¿Qué pasó hoy?",
            zone,
        )

        assertTrue(prompt.contains("Asistencia de los últimos 30 días: sin registro"))
        assertTrue(prompt.contains("Hoy: sin registro"))
        assertTrue(prompt.contains("Avisos vigentes para sus grupos:\n- sin registro"))
        assertTrue(prompt.contains("Incidentes registrados de esos alumnos:\n- sin registro"))
    }

    @Test
    fun `without context the prompt says the read failed`() {
        val prompt = assistantPrompt(context = null, question = "¿Qué tareas hay?", zone = zone)

        assertTrue(prompt.contains("No hay datos de alumnos disponibles"))
        assertFalse(prompt.contains("Mariana"))
    }

    @Test
    fun `the 30 day window ignores sessions before it and missing records`() {
        val days = listOf(
            day("2026-11-19", AttendanceMark.PRESENT),
            day("2026-11-18", AttendanceMark.LATE),
            day("2026-11-17", AttendanceMark.ABSENT),
            day("2026-11-16", AttendanceMark.JUSTIFIED),
            // Before the window the loader asked for.
            day("2026-10-19", AttendanceMark.ABSENT),
            // A session with no record for this child is not an absence.
            day("2026-11-15", null),
        )

        val counts = assistantAttendanceCounts(days, fromIso = "2026-10-20")

        assertEquals(1, counts.present)
        assertEquals(1, counts.late)
        assertEquals(1, counts.absent)
        assertEquals(1, counts.justified)
        assertEquals(4, counts.total)
    }

    @Test
    fun `the ISO date does not follow the device language`() {
        val moment = Calendar.getInstance(zone).apply {
            clear()
            set(2026, 10, 19, 12, 0)
        }.timeInMillis

        assertEquals("2026-11-19", assistantIsoDate(moment, zone))
    }

    private fun context() = AssistantContext(
        children = listOf(
            AssistantChild(
                fullName = "Mariana Gómez Ruiz",
                firstName = "Mariana",
                groupLabel = "4° D",
                attendance = ParentAttendanceCounts(present = 2, late = 1, absent = 1, justified = 0),
                today = AssistantToday(mark = AttendanceMark.PRESENT, time = "07:58"),
                pendingTasks = listOf(
                    AssistantTask(
                        taskId = TASK_ID,
                        subject = "Matemáticas",
                        title = "Fracciones",
                        dueDateMillis = at(2026, 11, 20),
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
        incidents = listOf(
            AssistantIncident(
                studentName = "Mariana Gómez Ruiz",
                title = "Caída en el patio",
                severity = "leve",
                occurredAtMillis = at(2026, 11, 17),
                status = "revisado",
            ),
        ),
    )

    private fun day(date: String, mark: AttendanceMark?) = ParentDayAttendance(
        date = date,
        subject = "Matemáticas",
        startedAt = 0L,
        mark = mark,
        time = null,
        note = null,
    )

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, 12, 0)
    }.timeInMillis

    private companion object {
        const val TASK_ID = "task-9f1c-secret"
        const val STUDENT_ID = "stu-IqywT5pkCuXHIYz5vRRv-01-secret"
        const val GROUP_ID = "IqywT5pkCuXHIYz5vRRv"
    }
}
