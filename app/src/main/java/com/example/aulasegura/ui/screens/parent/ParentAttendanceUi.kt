package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.Calendar
import java.util.TimeZone

/** What the tutor's attendance screen renders for one month and one child. */
sealed interface ParentAttendanceUiState {

    data object Loading : ParentAttendanceUiState

    data class Loaded(
        val childName: String,
        val groupLabel: String,
        /** How many children the tutor has; more than one makes the pill tappable. */
        val childCount: Int,
        val selected: Int,
        val monthLabel: String,
        val percent: Int?,
        val counts: ParentAttendanceCounts,
        val classDays: Int,
        /** Monday-to-Friday rows; `null` is an empty cell. */
        val weeks: List<List<ParentCalendarDay?>>,
        val recent: List<ParentAttendanceRecordUi>,
        val canGoNext: Boolean,
    ) : ParentAttendanceUiState

    data object NoChildren : ParentAttendanceUiState

    data class Failed(val message: String) : ParentAttendanceUiState
}

data class ParentCalendarDay(
    val day: Int,
    /** `null` when the group had no session that day. */
    val mark: AttendanceMark?,
    val isCurrent: Boolean,
    val isPast: Boolean,
)

data class ParentAttendanceRecordUi(
    val weekday: String,
    val day: String,
    val title: String,
    val subtitle: String,
    /** The pill's own label: "Presente", "Retardo", "Falta", "Justificada" or "Sin registro". */
    val pillText: String,
    val mark: AttendanceMark?,
)

/**
 * The month as Monday-to-Friday rows, exactly the shape the calendar card draws.
 *
 * Weekends are not cells: the screen has five columns, and a Saturday in a
 * fifth-column grid would be read as a class day. The leading blanks align the
 * first weekday of the month with its column.
 */
internal fun monthWeeks(
    year: Int,
    month: Int,
    marks: Map<Int, AttendanceMark?>,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    zone: TimeZone = TimeZone.getDefault(),
): List<List<ParentCalendarDay?>> {
    val calendar = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, 1)
    }
    val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    var firstClassDay = 1
    while (firstClassDay <= lastDay && weekdayIndex(year, month, firstClassDay, zone) == null) {
        firstClassDay++
    }

    val rows = mutableListOf<List<ParentCalendarDay?>>()
    var week = mutableListOf<ParentCalendarDay?>()

    if (firstClassDay <= lastDay) {
        repeat(weekdayIndex(year, month, firstClassDay, zone) ?: 0) { week += null }
    }

    for (day in firstClassDay..lastDay) {
        if (weekdayIndex(year, month, day, zone) == null) continue

        week += ParentCalendarDay(
            day = day,
            mark = marks[day],
            isCurrent = year == todayYear && month == todayMonth && day == todayDay,
            isPast = isBefore(
                year = year,
                month = month,
                day = day,
                todayYear = todayYear,
                todayMonth = todayMonth,
                todayDay = todayDay,
            ),
        )
        if (week.size == DAYS_PER_WEEK) {
            rows += week
            week = mutableListOf()
        }
    }

    if (week.isNotEmpty()) {
        while (week.size < DAYS_PER_WEEK) week += null
        rows += week
    }

    return rows
}

/**
 * One mark per day-of-month when the group had several subjects that day.
 *
 * The most severe wins (absent > late > justified > present): a day with one
 * absence is not a full attendance day, and showing the best mark would hide it.
 */
internal fun marksByDay(days: List<ParentDayAttendance>): Map<Int, AttendanceMark?> =
    days.groupBy { it.date.takeLast(2).toIntOrNull() }
        .mapNotNull { (day, ofDay) -> day?.let { it to worstMark(ofDay.map { row -> row.mark }) } }
        .toMap()

internal fun worstMark(marks: List<AttendanceMark?>): AttendanceMark? =
    marks.minByOrNull { severity(it) }?.takeIf { it != null }

private fun severity(mark: AttendanceMark?): Int = when (mark) {
    AttendanceMark.ABSENT -> 0
    AttendanceMark.LATE -> 1
    AttendanceMark.JUSTIFIED -> 2
    AttendanceMark.PRESENT -> 3
    null -> 4
}

internal fun ParentDayAttendance.toRecentUi(
    zone: TimeZone = TimeZone.getDefault(),
): ParentAttendanceRecordUi = ParentAttendanceRecordUi(
    weekday = AttendanceTimes.weekdayLabel(date, zone),
    day = date.takeLast(2),
    title = subject.ifBlank { "Día completo" },
    subtitle = when (mark) {
        AttendanceMark.PRESENT, AttendanceMark.LATE ->
            time?.let { "Entrada $it" } ?: "Sin hora registrada"
        AttendanceMark.JUSTIFIED -> note ?: "Justificada por Dirección"
        AttendanceMark.ABSENT -> note ?: "Requiere comprobante médico o nota"
        null -> "Sin registro"
    },
    pillText = when (mark) {
        AttendanceMark.PRESENT -> "Presente"
        AttendanceMark.LATE -> "Retardo"
        AttendanceMark.ABSENT -> "Falta"
        AttendanceMark.JUSTIFIED -> "Justificada"
        null -> "Sin registro"
    },
    mark = mark,
)

internal fun attendanceFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver la asistencia."
    RosterFailure.UNKNOWN -> "No se pudo cargar la asistencia."
}

private fun weekdayIndex(year: Int, month: Int, day: Int, zone: TimeZone): Int? {
    val calendar = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day)
    }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        else -> null
    }
}

private fun isBefore(
    year: Int,
    month: Int,
    day: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
): Boolean = when {
    year != todayYear -> year < todayYear
    month != todayMonth -> month < todayMonth
    else -> day < todayDay
}

private const val DAYS_PER_WEEK = 5
