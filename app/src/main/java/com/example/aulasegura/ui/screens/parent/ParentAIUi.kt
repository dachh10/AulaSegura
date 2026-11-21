package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What the assistant screen renders, already resolved.
 *
 * The history lives here and only here: `CONTEXT.md` §9.2 forbids persisting the
 * conversation, so closing the screen (or the process) is the end of it.
 */
data class ParentAIUi(
    /** Always drawn at the top, as the design does. */
    val welcome: String = "",
    val messages: List<ParentAIMessageUi> = emptyList(),
    val draft: String = "",
    /** A question is being answered right now: the composer is blocked. */
    val sending: Boolean = false,
    /** The children's data is still being read. */
    val loading: Boolean = true,
    /**
     * The assistant cannot answer: `ai_assistant_enabled` is off, the project is
     * not configured, or App Check refused. Only the suggestions work, with the
     * predefined answers `CONTEXT.md` §9.2 requires.
     */
    val faqOnly: Boolean = false,
    /** The data could not be read; the tutor gets the reason and a retry. */
    val contextError: String? = null,
    /** The last failure of a question, already in the tutor's words. */
    val notice: String? = null,
)

data class ParentAIMessageUi(
    val id: Long,
    val text: String,
    /** `true` for the tutor's own question. */
    val mine: Boolean,
    /** `"10:41"` or `""` while the assistant is still answering. */
    val time: String,
    /**
     * Real tasks this answer is about. Only filled when the app knows them (the
     * "tareas de mañana" suggestion); a free-text answer never invents a card.
     */
    val tasks: List<ParentAITaskUi> = emptyList(),
)

/** A task the answer can open, taken from the data the screen already loaded. */
data class ParentAITaskUi(
    val taskId: String,
    val subject: String,
    val title: String,
)

/**
 * The three suggestions of the design, with the exact sentence each one asks.
 *
 * They are an enum and not three loose strings because both paths need them: the
 * AI question and the offline FAQ answer. A label that says one thing and a
 * question that asks another is how the assistant ends up answering something
 * nobody asked.
 */
enum class ParentAIQuestion(val label: String, val prompt: String) {
    TASKS(
        label = "¿Qué tareas tiene mañana?",
        prompt = "¿Qué tareas tiene mañana?",
    ),
    JUSTIFY(
        label = "¿Cómo justifico una falta?",
        prompt = "¿Cómo justifico una falta?",
    ),
    MONTH(
        label = "Resumen de asistencia del mes",
        prompt = "Resumen de asistencia del mes",
    ),
}

/**
 * The greeting, built from what the app really knows.
 *
 * The design says "¡Hola, Carlos! ... de Mariana." A name or a child that is not
 * in the session is not invented: without a name it greets without a name, and
 * without children it does not promise a child.
 */
fun assistantWelcome(fullName: String, childNames: List<String>): String {
    val firstName = fullName.trim().split(' ').firstOrNull().orEmpty()
    val greeting = if (firstName.isEmpty()) "¡Hola!" else "¡Hola, $firstName!"

    return when {
        childNames.isEmpty() ->
            "$greeting Soy tu asistente escolar. Puedo ayudarte con tareas, faltas y avisos."

        else ->
            "$greeting Soy tu asistente escolar. Puedo ayudarte con tareas, faltas y avisos de " +
                childNames.joinToSpanish()
    }
}

/**
 * The predefined answer for [question], from the data the screen already loaded.
 *
 * This is the fallback `CONTEXT.md` §9.2 requires when the AI is unavailable. It
 * is deterministic on purpose: the same data the assistant would have received,
 * printed without a model, so the tutor is never left with a dead screen. No
 * answer invents a child, a task or a date.
 */
fun assistantFaqAnswer(
    question: ParentAIQuestion,
    context: AssistantContext?,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (context == null || context.children.isEmpty()) {
        return "No pude leer los datos de tus hijos, así que no tengo el registro."
    }

    return when (question) {
        ParentAIQuestion.TASKS -> assistantTasksAnswer(context, now, zone)
        ParentAIQuestion.MONTH -> context.children.joinToString("\n") { child ->
            "${child.firstName}: ${child.attendance.describeForFaq()}"
        }
        ParentAIQuestion.JUSTIFY ->
            "En AulaSegura, abre Faltas y toca «Justificar» en la falta del día. " +
                "Elige el motivo, adjunta el comprobante (foto o PDF) y envía el justificante; " +
                "la escuela lo revisa y la falta queda como justificada. " +
                "Si no hay internet, se guarda en el teléfono y se envía después."
    }
}

/** The real tasks a "tareas de mañana" answer can open; empty for any other. */
fun assistantTasksFor(
    question: ParentAIQuestion?,
    context: AssistantContext?,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentAITaskUi> {
    if (question != ParentAIQuestion.TASKS || context == null) return emptyList()

    val tomorrow = isoDate(now, TOMORROW_OFFSET, zone)
    return context.children
        .flatMap { child ->
            child.pendingTasks
                .filter { isoDate(it.dueDateMillis, 0, zone) == tomorrow }
                .map { ParentAITaskUi(taskId = it.taskId, subject = it.subject, title = it.title) }
        }
        .distinctBy { it.taskId }
        .take(MAX_TASK_CARDS)
}

private fun assistantTasksAnswer(context: AssistantContext, now: Long, zone: TimeZone): String {
    val tomorrow = isoDate(now, TOMORROW_OFFSET, zone)

    val lines = context.children.mapNotNull { child ->
        val due = child.pendingTasks.filter { isoDate(it.dueDateMillis, 0, zone) == tomorrow }
        if (due.isEmpty()) null else {
            "${child.firstName}: " + due.joinToString("; ") { "${it.subject} — ${it.title}" }
        }
    }

    return if (lines.isEmpty()) {
        "No hay tareas registradas para mañana."
    } else {
        "Tareas para mañana:\n" + lines.joinToString("\n")
    }
}

private fun ParentAttendanceCounts.describeForFaq(): String {
    if (total == 0) return "sin registro de asistencia en los últimos 30 días."

    val late = if (late == 0) "" else " ($late ${if (late == 1) "retardo" else "retardos"})"
    return "$attended asistencias$late, $absent ${if (absent == 1) "falta" else "faltas"}, " +
        "$justified ${if (justified == 1) "justificada" else "justificadas"} " +
        "de $total sesiones con registro."
}

/** `"Mariana, Diego y Sofía"` — the Spanish list the greeting needs. */
private fun List<String>.joinToSpanish(): String = when (size) {
    0 -> ""
    1 -> first()
    2 -> "${first()} y ${last()}"
    else -> dropLast(1).joinToString(", ") + " y " + last()
}

private fun isoDate(epochMillis: Long, dayOffset: Int, zone: TimeZone): String {
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
    calendar.add(Calendar.DAY_OF_MONTH, dayOffset)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Date(calendar.timeInMillis))
}

private const val TOMORROW_OFFSET = 1
private const val MAX_TASK_CARDS = 3
