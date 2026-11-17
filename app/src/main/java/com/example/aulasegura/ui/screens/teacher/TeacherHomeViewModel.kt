package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.IncidentHistoryRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import com.example.aulasegura.domain.repository.NotificationRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.ScheduleLoadResult
import com.example.aulasegura.domain.repository.ScheduleRepository
import com.example.aulasegura.domain.repository.SyncQueueRepository
import com.example.aulasegura.domain.repository.TaskRepository
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The teacher's home, built from what the app already knows.
 *
 * Nothing here is invented: the next class comes from the timetable, the summary
 * from today's sessions and the outbox, and the activity feed from the four
 * sources the teacher writes. When a source has nothing, the screen says so
 * instead of printing a number that looks real.
 */
class TeacherHomeViewModel(
    groups: GroupRepository,
    schedule: ScheduleRepository,
    history: AttendanceHistoryRepository,
    incidents: IncidentHistoryRepository,
    tasks: TaskRepository,
    notices: NoticeRepository,
    notifications: NotificationRepository,
    pending: SyncQueueRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val scheduleState = MutableStateFlow<ScheduleLoadResult?>(null)

    private val myGroups = groups.observeMyGroups()
    private val sessions = history.observeHistory()
    private val unread = notifications.observeMyNotifications().map { unreadNotifications(it) }

    init {
        viewModelScope.launch { scheduleState.value = schedule.loadSchedule() }
    }

    private val activity = combine(
        sessions,
        incidents.observeIncidents(),
        tasks.observeMyTasks(),
        notices.observeMyNotices(),
    ) { sessions, incidents, tasks, notices ->
        recentActivity(sessions, incidents, tasks, notices, now = now(), zone = zone)
    }

    private val summary = combine(
        sessions,
        myGroups,
        pending.observePendingCount(),
    ) { sessions, groups, pendingCount ->
        todaySummary(
            sessions = sessions,
            todayIso = AttendanceTimes.isoDate(now(), zone),
            groupsTotal = groups.size,
            pendingSync = pendingCount,
        )
    }

    val state: StateFlow<TeacherHomeUi> = combine(
        myGroups,
        scheduleState,
        activity,
        summary,
        unread,
    ) { groups, schedule, activity, summary, unread ->
        val nextClass = (schedule as? ScheduleLoadResult.Loaded)?.let { loaded ->
            nextClassOf(
                slots = loaded.slots,
                groups = groups.associateBy { it.id },
                weekday = scheduleWeekday(now(), zone),
                nowMinutes = minutesOfDay(now(), zone),
            )
        }

        TeacherHomeUi(
            nextClass = nextClass,
            scheduleNotice = if (nextClass != null) null else scheduleNotice(schedule),
            summary = summary,
            activity = activity,
            unreadNotifications = unread,
            loading = schedule == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_MILLIS),
        initialValue = TeacherHomeUi(),
    )

    /**
     * Four different sentences, because "no hay clases" and "no pude leer el
     * horario" mean opposite things to a teacher standing at the gate.
     */
    private fun scheduleNotice(schedule: ScheduleLoadResult?): String? = when (schedule) {
        null -> null
        ScheduleLoadResult.SignedOut -> "No se pudo leer el horario."
        is ScheduleLoadResult.Failed -> when (schedule.reason) {
            RosterFailure.OFFLINE -> "Sin conexión para revisar el horario."
            RosterFailure.DENIED -> "No tienes acceso al horario del plantel."
            RosterFailure.UNKNOWN -> "No se pudo leer el horario."
        }
        is ScheduleLoadResult.Loaded ->
            if (schedule.slots.isEmpty()) {
                "Aún no hay horario programado."
            } else {
                "No hay más clases hoy."
            }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
