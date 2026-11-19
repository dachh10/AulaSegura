package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentTaskBrief
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.Locale
import java.util.TimeZone

/**
 * What the tutor's home renders.
 *
 * Same split as the teacher's home: the ViewModel produces a state that already
 * carries the strings, and the screen only lays it out. A design-time constant
 * cannot leak into a card because there is no constant left to read.
 */
sealed interface ParentHomeUiState {

    data object Loading : ParentHomeUiState

    data class Loaded(
        /** "HOY · MAR 18 NOV" for the summary header. */
        val todayLabel: String,
        val children: List<ParentChildUi>,
        val selected: Int,
        val tasks: List<ParentTaskUi>,
    ) : ParentHomeUiState {

        val child: ParentChildUi get() = children[selected]
    }

    /** A tutor account with no linked student yet: the code was never redeemed. */
    data object NoChildren : ParentHomeUiState

    data class Failed(val message: String) : ParentHomeUiState
}

data class ParentChildUi(
    val studentId: String,
    val name: String,
    val grade: String,
    val initials: String,
    /** `null` when the month has no record of this child yet. */
    val percent: Int?,
    val mark: AttendanceMark?,
    val headline: String,
    val status: String,
)

data class ParentTaskUi(
    val subject: String,
    val title: String,
    val dueLabel: String,
    /** Today or tomorrow: the row paints its pill amber, like the Stitch design. */
    val dueSoon: Boolean,
)

internal fun ParentChildOverview.toChildUi(): ParentChildUi {
    val name = child.firstName.ifBlank { child.fullName }

    return ParentChildUi(
        studentId = child.studentId,
        name = name,
        grade = child.groupLabel,
        initials = initialsOf(child.fullName),
        percent = month.counts.percent,
        mark = today?.mark,
        headline = headlineFor(name, today?.mark),
        status = statusFor(today?.mark, today?.time),
    )
}

/**
 * First letter of the first two words: "Carlos Gómez" becomes "CG".
 *
 * One letter is enough for a single-word name; a blank name keeps the avatar
 * empty instead of printing a placeholder initial that is not the person's.
 */
internal fun initialsOf(fullName: String): String = fullName
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .map { it.first().uppercaseChar() }
    .joinToString("")

internal fun headlineFor(name: String, mark: AttendanceMark?): String = when (mark) {
    AttendanceMark.PRESENT -> "$name asistió a clase"
    AttendanceMark.LATE -> "$name llegó tarde"
    AttendanceMark.ABSENT -> "$name faltó hoy"
    AttendanceMark.JUSTIFIED -> "$name tiene falta justificada"
    null -> "Sin pase de lista hoy"
}

/** The pill under the headline. The arrival time is shown only when it exists. */
internal fun statusFor(mark: AttendanceMark?, time: String?): String = when (mark) {
    AttendanceMark.PRESENT -> time?.let { "Presente · $it" } ?: "Presente"
    AttendanceMark.LATE -> time?.let { "Retardo · $it" } ?: "Retardo"
    AttendanceMark.ABSENT -> "Falta"
    AttendanceMark.JUSTIFIED -> "Justificada"
    null -> "Sin registro"
}

internal fun ParentTaskBrief.toUi(
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentTaskUi {
    val dueIso = AttendanceTimes.isoDate(dueDateMillis, zone)
    val todayIso = AttendanceTimes.isoDate(now, zone)
    val tomorrowIso = AttendanceTimes.isoDate(now + DAY_MILLIS, zone)

    val label = when (dueIso) {
        todayIso -> "Hoy"
        tomorrowIso -> "Mañana"
        else -> AttendanceTimes.dayLabel(dueDateMillis, zone)
    }

    return ParentTaskUi(
        subject = subject,
        title = title,
        dueLabel = label,
        dueSoon = dueIso == todayIso || dueIso == tomorrowIso,
    )
}

internal fun homeFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver estos datos."
    RosterFailure.UNKNOWN -> "No se pudieron cargar los datos."
}

/** The header uses the same Spanish label the teacher's roll call prints. */
internal fun todayHeader(now: Long, zone: TimeZone = TimeZone.getDefault()): String =
    "HOY · " + AttendanceTimes.dayLabel(now, zone).uppercase(Locale("es", "MX"))

private const val DAY_MILLIS = 86_400_000L
