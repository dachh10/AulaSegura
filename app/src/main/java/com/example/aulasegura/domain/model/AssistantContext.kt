package com.example.aulasegura.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What the assistant may know about the signed-in tutor's children.
 *
 * Assembled from the same reads the family screens already do, so the model never
 * receives anything the tutor cannot already see. It carries **no identifiers**:
 * not the uid, not the student id, not the group id. The task id is the only
 * exception, and it exists for the screen (the "Ver tarea" card), never for the
 * prompt — [assistantPrompt] renders subject and title instead.
 *
 * `CONTEXT.md` §9.2 is the contract: attendance summary, pending tasks and
 * current notices of those students, nothing else.
 */
data class AssistantContext(
    val children: List<AssistantChild>,
    val notices: List<AssistantNotice>,
    val incidents: List<AssistantIncident>,
)

/** One child, as the assistant sees them. */
data class AssistantChild(
    val fullName: String,
    val firstName: String,
    /** Human name of the group ("4° D"); never the group id. */
    val groupLabel: String,
    /** Present/late/absent/justified over the window the screen asked for. */
    val attendance: ParentAttendanceCounts,
    /** Today's mark, or `null` when the school has not marked the child yet. */
    val today: AssistantToday?,
    /** Tasks still due, nearest first. Bounded by the loader, not by the prompt. */
    val pendingTasks: List<AssistantTask>,
)

data class AssistantToday(val mark: AttendanceMark, val time: String?)

/**
 * A due task.
 *
 * [taskId] never reaches the model: it is what the card needs to open the
 * detail. The model reads [subject], [title] and the due date.
 */
data class AssistantTask(
    val taskId: String,
    val subject: String,
    val title: String,
    val dueDateMillis: Long,
)

data class AssistantNotice(
    val title: String,
    val body: String,
    val urgent: Boolean,
    val createdAtMillis: Long,
)

data class AssistantIncident(
    val studentName: String,
    val title: String,
    /** `leve`, `moderado` or `grave`, as reported. */
    val severity: String,
    val occurredAtMillis: Long,
    /** `nuevo`, `revisado` or `cerrado`. */
    val status: String,
)

/**
 * `"2026-11-19"`, the format the loader compares against.
 *
 * ISO rather than a locale-dependent label: the comparison is lexical and must
 * not change with the device language.
 */
fun assistantIsoDate(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Date(epochMillis))

/**
 * Counters of the sessions that happened on or after [fromIso].
 *
 * The month document the home screen loads counts the whole calendar month, so
 * the 30-day window is recomputed from the sessions instead of reusing
 * `ParentMonthAttendance.counts`: on the 3rd of the month those two numbers are
 * not the same, and quoting the month while saying "last 30 days" would be a
 * lie the tutor can check against the calendar screen.
 *
 * A session with no record for the child is ignored: `mark == null` is a missing
 * record, not absence (the same rule the attendance screen follows).
 */
fun assistantAttendanceCounts(
    days: List<ParentDayAttendance>,
    fromIso: String,
): ParentAttendanceCounts {
    var present = 0
    var late = 0
    var absent = 0
    var justified = 0

    days.forEach { day ->
        if (day.date.isBlank() || day.date < fromIso) return@forEach
        when (day.mark) {
            AttendanceMark.PRESENT -> present++
            AttendanceMark.LATE -> late++
            AttendanceMark.ABSENT -> absent++
            AttendanceMark.JUSTIFIED -> justified++
            null -> Unit
        }
    }

    return ParentAttendanceCounts(
        present = present,
        late = late,
        absent = absent,
        justified = justified,
    )
}
