package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.model.subjectIdFromLabel
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask
import com.example.aulasegura.util.AttendanceTimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The teacher's home, already resolved.
 *
 * Everything here is a number or a sentence the screen can print; the clock and
 * the time zone are parameters so the card can be tested on a Tuesday morning
 * without waiting for one.
 */
data class TeacherHomeUi(
    /** `null` when there is no class left today (or no timetable at all). */
    val nextClass: NextClassUi? = null,
    /** Why [nextClass] is null: printed under the hero title. */
    val scheduleNotice: String? = null,
    val summary: TodaySummaryUi = TodaySummaryUi(),
    val activity: List<ActivityRowUi> = emptyList(),
    val unreadNotifications: Int = 0,
    /** Families' reports waiting for the school; the card hides at zero. */
    val pendingJustifications: Int = 0,
    val loading: Boolean = true,
)

/** The class the hero card shows, in progress or still to come. */
data class NextClassUi(
    val groupId: String,
    val status: ClassStatus,
    /** `"10:00"` for a class to come, `"10:00 – 10:45"` for one in progress. */
    val timeLabel: String,
    val subject: String,
    val groupLabel: String,
    val studentCount: Int,
    /** Canonical id passed to the roll call; empty only for an unknown label. */
    val subjectId: String = "",
) {

    /** `"Matemáticas · 3° B"`; a slot without a subject prints only the group. */
    val headline: String
        get() = listOf(subject, groupLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
}

enum class ClassStatus {
    IN_PROGRESS,
    NEXT,
}

/** The three cards of "Resumen de hoy". */
data class TodaySummaryUi(
    val listsPassed: Int = 0,
    val groupsTotal: Int = 0,
    val absences: Int = 0,
    val pendingSync: Int = 0,
)

enum class ActivityKind {
    ROLL_CALL,
    INCIDENT,
    TASK,
    NOTICE,
}

data class ActivityRowUi(
    val id: String,
    val kind: ActivityKind,
    val title: String,
    val timeLabel: String,
    /** Only for ordering; the row prints [timeLabel]. */
    val at: Long,
)

/**
 * `java.util.Calendar`'s weekday is Sunday-first; the schedule document is
 * Monday-first. Getting this wrong moves every class by one day, which looks
 * plausible on a screenshot and is wrong every single day.
 */
fun scheduleWeekday(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val calendarDay = Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
        .get(Calendar.DAY_OF_WEEK)
    return ((calendarDay + 5) % 7) + 1
}

/** Minutes since local midnight, the unit the slots are compared in. */
fun minutesOfDay(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

/**
 * The class the card should show: the one in progress if there is one, otherwise
 * the next one that starts today.
 *
 * A class in progress wins on purpose — at 08:10 the teacher is standing in front
 * of that group, and "Siguiente clase 10:00" would send them to the wrong room.
 */
fun nextClassOf(
    slots: List<ClassSlot>,
    groups: Map<String, Group>,
    weekday: Int,
    nowMinutes: Int,
): NextClassUi? {
    val today = slots.filter { it.weekday == weekday && it.startMinutes >= 0 && it.endMinutes > it.startMinutes }

    val inProgress = today
        .filter { nowMinutes >= it.startMinutes && nowMinutes < it.endMinutes }
        .minByOrNull { it.startMinutes }
    val upcoming = today
        .filter { it.startMinutes > nowMinutes }
        .minByOrNull { it.startMinutes }

    val slot = inProgress ?: upcoming ?: return null
    val status = if (inProgress != null) ClassStatus.IN_PROGRESS else ClassStatus.NEXT
    val group = groups[slot.groupId]

    return NextClassUi(
        groupId = slot.groupId,
        status = status,
        timeLabel = if (status == ClassStatus.IN_PROGRESS) {
            "${slot.startTime} – ${slot.endTime}"
        } else {
            slot.startTime
        },
        subject = slot.subject,
        groupLabel = group?.label.orEmpty(),
        studentCount = group?.studentCount ?: 0,
        subjectId = subjectIdFromLabel(slot.subject, group?.grade).orEmpty(),
    )
}

/**
 * "Listas pasadas" counts **groups**, not sessions: the card reads `2/3`, and a
 * second session of the same group would otherwise print `3/3` with one group
 * still unvisited.
 */
fun todaySummary(
    sessions: List<AttendanceSessionSummary>,
    todayIso: String,
    groupsTotal: Int,
    pendingSync: Int,
): TodaySummaryUi {
    val today = sessions.filter { it.date == todayIso }
    val passed = today.map { it.groupId }.distinct().size

    return TodaySummaryUi(
        listsPassed = passed,
        groupsTotal = maxOf(groupsTotal, passed),
        absences = today.sumOf { it.counts.absent },
        pendingSync = pendingSync,
    )
}

/**
 * What the teacher did, newest first: closed lists, incidents, tasks and notices.
 *
 * Divided by kind instead of merged into a single "event" table because the four
 * sources already exist and none of them needs to know about the others.
 */
fun recentActivity(
    sessions: List<AttendanceSessionSummary>,
    incidents: List<IncidentSummary>,
    tasks: List<PublishedTask>,
    notices: List<PublishedNotice>,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
    limit: Int = 4,
): List<ActivityRowUi> = buildList {
    sessions.forEach { session ->
        add(
            ActivityRowUi(
                id = "roll-${session.sessionId}",
                kind = ActivityKind.ROLL_CALL,
                title = "Lista de ${session.groupLabel} guardada",
                timeLabel = activityTimeLabel(session.startedAt, now, zone),
                at = session.startedAt,
            ),
        )
    }
    incidents.forEach { incident ->
        add(
            ActivityRowUi(
                id = "incident-${incident.incidentId}",
                kind = ActivityKind.INCIDENT,
                title = "Incidente de ${incident.studentName}",
                timeLabel = activityTimeLabel(incident.occurredAt, now, zone),
                at = incident.occurredAt,
            ),
        )
    }
    tasks.forEach { task ->
        add(
            ActivityRowUi(
                id = "task-${task.taskId}",
                kind = ActivityKind.TASK,
                title = "Tarea «${task.title}» publicada",
                timeLabel = activityTimeLabel(task.createdAt, now, zone),
                at = task.createdAt,
            ),
        )
    }
    notices.forEach { notice ->
        add(
            ActivityRowUi(
                id = "notice-${notice.noticeId}",
                kind = ActivityKind.NOTICE,
                title = "Aviso «${notice.title}» publicado",
                timeLabel = activityTimeLabel(notice.createdAt, now, zone),
                at = notice.createdAt,
            ),
        )
    }
}.sortedByDescending { it.at }.take(limit)

/** `"07:55"` today, `"Ayer"` yesterday, `"14 nov"` before that. */
fun activityTimeLabel(
    at: Long,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): String {
    val atDate = AttendanceTimes.isoDate(at, zone)
    if (atDate == AttendanceTimes.isoDate(now, zone)) return AttendanceTimes.clockTime(at, zone)
    if (atDate == AttendanceTimes.isoDate(now - DAY_MILLIS, zone)) return "Ayer"

    return SimpleDateFormat("d MMM", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(at))
        .replace(".", "")
}

private const val DAY_MILLIS = 86_400_000L
private val SPANISH_MX = Locale("es", "MX")
