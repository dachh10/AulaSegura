package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.util.AttendanceTimes
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * What "Detalle de grupo" renders, already resolved.
 *
 * Everything here is derived from what the school actually stores: the roster,
 * the closed roll calls of this group and the incidents reported for it. There
 * is no sample student, no fixed "94%" and no invented room: a group without
 * sessions shows an empty calendar and a group without incidents says so.
 */
data class TeacherGroupDetailUi(
    val groupId: String = "",
    /** `"3° B"` — the badge of the hero. */
    val label: String = "",
    val subject: String = "",
    /** `"Hoy · 08:00 – 09:00"`, from the group's timetable. `null` without a slot. */
    val slotLabel: String? = null,
    val listState: GroupListState = GroupListState.Loading,
    /** Average attendance of every session cached for this group; `null` with none. */
    val attendancePercent: Int? = null,
    /** Absent students in today's session; `null` when today has no roll call. */
    val absencesToday: Int? = null,
    val students: List<GroupStudentUi> = emptyList(),
    val monthOffset: Int = 0,
    val canGoNextMonth: Boolean = false,
    val calendar: GroupCalendarUi = GroupCalendarUi(),
    val sessions: List<GroupSessionUi> = emptyList(),
    val incidents: List<GroupIncidentUi> = emptyList(),
    val expandedIncidentId: String? = null,
    val refreshing: Boolean = true,
    val notice: String? = null,
)

/**
 * Which sentence the hero chip deserves today.
 *
 * [NoClass] is its own answer instead of falling into [Pending]: on a Saturday
 * there is nothing to take, and a "Lista pendiente" chip would send the teacher
 * to a screen that refuses to open (`SchoolCalendar`).
 */
enum class GroupListState {
    /** The group or its sessions are still being read. */
    Loading,

    Pending,
    Done,
    NoClass,
}

/** One row of the "Alumnos" tab. */
data class GroupStudentUi(
    val id: String,
    /** `"01"` — the list number, which is the roll call order. */
    val number: String,
    val fullName: String,
    val initials: String,
)

/** One weekday cell of the attendance calendar. */
data class GroupCalendarDayUi(
    val day: Int,
    /** Attendance of that day; `null` when no list was saved. */
    val percent: Int?,
    val isToday: Boolean,
)

/**
 * The calendar of one month.
 *
 * [leadingBlanks] is how many empty cells the first week needs: the grid is
 * Monday–Friday, so a month that starts on a Wednesday has to push two cells.
 * A month that starts on the weekend needs none — its first class is the Monday
 * of the following week.
 */
data class GroupCalendarUi(
    val monthLabel: String = "",
    val leadingBlanks: Int = 0,
    val days: List<GroupCalendarDayUi> = emptyList(),
)

/** One closed roll call of this group. */
data class GroupSessionUi(
    val id: String,
    /** `"Lun 3 nov"`. */
    val dayLabel: String,
    /** `"08:05"` — when the session was started. */
    val timeLabel: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val justified: Int,
    val sync: RollCallSync,
)

/** One incident reported for this group. */
data class GroupIncidentUi(
    val id: String,
    /** `"Leve"`; `null` when the stored wire value is not one this build knows. */
    val severityLabel: String?,
    val severity: IncidentSeverity?,
    /** `"Jue 30 oct · 11:20"`. */
    val occurredLabel: String,
    val title: String,
    val studentName: String,
    val categoryLabel: String,
    val statusLabel: String,
    val description: String,
    val locationLabel: String?,
    val mediaCount: Int,
    val expanded: Boolean,
)

/** The roster as the tab paints it: list order, initials and the padded number. */
fun groupStudents(roster: List<Student>): List<GroupStudentUi> = roster.map { student ->
    GroupStudentUi(
        id = student.id,
        number = student.listNumber.toString().padStart(2, '0'),
        fullName = student.fullName,
        initials = initialsOf(student.firstName, student.lastName),
    )
}

/**
 * Filters the roster by name or number.
 *
 * The number is matched with leading zeros stripped so typing "3" finds
 * "N° 03" — a teacher searching for a child by list number does not type the
 * padding.
 */
fun searchGroupStudents(students: List<GroupStudentUi>, query: String): List<GroupStudentUi> {
    val needle = query.trim()
    if (needle.isEmpty()) return students

    val digits = needle.filter { it.isDigit() }
    return students.filter { student ->
        student.fullName.contains(needle, ignoreCase = true) ||
            (digits.isNotEmpty() && student.number.toIntOrNull() == digits.toIntOrNull())
    }
}

/**
 * Attendance of one set of counters.
 *
 * `present + late` over everything marked: a late arrival sat in class, and a
 * justified absence did not, so it stays out of the numerator. `null` when the
 * session has no records, which is a session that cannot be averaged.
 */
fun attendancePercentOf(counts: AttendanceCounts): Int? {
    val total = counts.marked
    if (total <= 0) return null

    return ((counts.present + counts.late) * 100.0 / total).roundToInt()
}

/** Average attendance across the sessions cache; `null` when there are none. */
fun groupAttendancePercent(sessions: List<AttendanceSessionSummary>): Int? {
    val measured = sessions.mapNotNull { attendancePercentOf(it.counts) }
    if (measured.isEmpty()) return null

    return measured.average().roundToInt()
}

/** Absent students in the session of [isoDate]; `null` when that day has none. */
fun absencesOn(sessions: List<AttendanceSessionSummary>, isoDate: String): Int? =
    sessions.firstOrNull { it.date == isoDate }?.counts?.absent

/**
 * The calendar of [monthOffset] months before [now], in `[leadingBlanks] + days`.
 *
 * [todayIso] comes from the caller so the "today" ring is testable. Sessions are
 * already the ones of this group; a date without a session is not a zero, it is
 * "not taken", and the cell prints no number.
 */
fun groupCalendar(
    sessions: List<AttendanceSessionSummary>,
    now: Long,
    monthOffset: Int,
    todayIso: String,
    zone: TimeZone = TimeZone.getDefault(),
): GroupCalendarUi {
    val key = monthKeyBefore(now, monthOffset, zone)
    val year = key / 12
    // `monthKey` is `year * 12 + month`, and 0 is `Calendar.JANUARY`.
    val month = key % 12

    val calendar = Calendar.getInstance(zone).apply {
        clear()
        set(year, month, 1)
    }
    val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = mondayIndex(calendar.get(Calendar.DAY_OF_WEEK))
    val leadingBlanks = if (firstWeekday > FRIDAY_INDEX) 0 else firstWeekday

    val percentByDate = sessions
        .mapNotNull { session -> attendancePercentOf(session.counts)?.let { session.date to it } }
        .toMap()

    val days = buildList {
        for (day in 1..lastDay) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val weekday = mondayIndex(calendar.get(Calendar.DAY_OF_WEEK))
            if (weekday > FRIDAY_INDEX) continue

            val iso = AttendanceTimes.isoDate(calendar.timeInMillis, zone)
            add(
                GroupCalendarDayUi(
                    day = day,
                    percent = percentByDate[iso],
                    isToday = iso == todayIso,
                ),
            )
        }
    }

    return GroupCalendarUi(
        monthLabel = monthLabel(now, monthOffset, zone),
        leadingBlanks = leadingBlanks,
        days = days,
    )
}

/**
 * The roll calls of [monthOffset] months before [now], newest first.
 *
 * The month filter is the same `year * 12 + month` comparison the history uses,
 * so November here and November there mean the same days.
 */
fun groupSessionRows(
    sessions: List<AttendanceSessionSummary>,
    now: Long,
    monthOffset: Int,
    zone: TimeZone = TimeZone.getDefault(),
): List<GroupSessionUi> {
    val target = monthKeyBefore(now, monthOffset, zone)

    return sessions
        .filter { monthKeyOfIso(it.date, zone) == target }
        .sortedByDescending { it.startedAt }
        .map { session ->
            GroupSessionUi(
                id = session.sessionId,
                dayLabel = AttendanceTimes.dayLabel(session.startedAt, zone),
                timeLabel = AttendanceTimes.clockTime(session.startedAt, zone),
                present = session.counts.present,
                absent = session.counts.absent,
                late = session.counts.late,
                justified = session.counts.justified,
                sync = session.sync,
            )
        }
}

/**
 * The incidents of this group, newest first.
 *
 * [groupId] is matched against the summary instead of trusting the caller to
 * have filtered already: the same list feeds the home activity and the history,
 * and one screen forgetting the filter would show another group's incident.
 */
fun groupIncidentRows(
    incidents: List<IncidentSummary>,
    groupId: String,
    expandedId: String? = null,
    zone: TimeZone = TimeZone.getDefault(),
): List<GroupIncidentUi> = incidents
    .filter { it.groupId == groupId }
    .sortedByDescending { it.occurredAt }
    .map { incident ->
        GroupIncidentUi(
            id = incident.incidentId,
            severityLabel = incident.severity?.label,
            severity = incident.severity,
            occurredLabel = incidentTimestampLabel(incident.occurredAt, zone),
            title = incident.title.ifBlank {
                incident.description.lineSequence().firstOrNull().orEmpty().trim()
            },
            studentName = incident.studentName,
            categoryLabel = incident.category?.label.orEmpty(),
            statusLabel = incident.status.label,
            description = incident.description,
            locationLabel = incidentLocationLabel(
                incident.locationVerified,
                incident.locationDistanceMeters,
            ),
            mediaCount = incident.mediaCount,
            expanded = incident.incidentId == expandedId,
        )
    }

/**
 * `"Hoy · 08:00 – 09:00"` for the slot of this group on [weekday].
 *
 * The first slot of the day wins: the hero is a header, not a timetable, and a
 * group with two sessions shows the one it meets first today.
 */
fun groupSlotLabel(slots: List<ClassSlot>, groupId: String, weekday: Int): String? {
    val slot = slots
        .filter { it.groupId == groupId && it.weekday == weekday }
        .filter { it.startMinutes >= 0 && it.endMinutes > it.startMinutes }
        .minByOrNull { it.startMinutes }
        ?: return null

    return "Hoy · ${slot.startTime} – ${slot.endTime}"
}

/** `"Sincronizado"`, `"Pendiente"`, `"Enviando"` or `"Sin sincronizar"`. */
fun sessionSyncLabel(sync: RollCallSync): String = when (sync) {
    RollCallSync.PENDING -> "Pendiente"
    RollCallSync.SYNCING -> "Enviando"
    RollCallSync.SYNCED -> "Sincronizado"
    RollCallSync.FAILED -> "Sin sincronizar"
}

/** Monday = 0 … Sunday = 6, from `Calendar.DAY_OF_WEEK` (Sunday = 1). */
private fun mondayIndex(calendarDay: Int): Int = (calendarDay + 5) % 7

private const val FRIDAY_INDEX = 4
