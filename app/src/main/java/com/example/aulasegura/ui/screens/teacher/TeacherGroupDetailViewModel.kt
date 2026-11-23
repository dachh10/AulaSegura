package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.subjectForGroup
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.HistoryRefreshResult
import com.example.aulasegura.domain.repository.IncidentHistoryRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.RosterRefreshResult
import com.example.aulasegura.domain.repository.ScheduleLoadResult
import com.example.aulasegura.domain.repository.ScheduleRepository
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.util.AttendanceTimes
import com.example.aulasegura.util.SchoolCalendar
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Detalle de grupo".
 *
 * The three tabs read the same cached sources the rest of the teacher flow uses:
 * the roster of the group, the sessions this phone has saved and the incidents
 * reported for it. Opening the screen costs no Firestore reads when everything
 * is already cached, which is what makes it usable in a hallway with no signal.
 *
 * [open] is separate from the constructor because the group id comes from the
 * route, and the factory in `AppContainer` builds ViewModels without arguments —
 * the same pattern the conversation screen already uses.
 */
class TeacherGroupDetailViewModel(
    private val groups: GroupRepository,
    private val students: StudentRepository,
    private val history: AttendanceHistoryRepository,
    private val incidents: IncidentHistoryRepository,
    private val schedule: ScheduleRepository,
    private val auth: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val groupId = MutableStateFlow("")
    private val monthOffset = MutableStateFlow(0)
    private val expandedIncidentId = MutableStateFlow<String?>(null)
    private val refreshing = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)
    private val scheduleState = MutableStateFlow<ScheduleLoadResult?>(null)

    /** Same source as the group cards: the subject is a property of the teacher. */
    private val subjects: List<String> = auth.currentUser?.subjects.orEmpty()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roster = groupId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(emptyList()) else students.observeRoster(id)
    }

    private data class Basis(
        val group: Group?,
        val roster: List<Student>,
        val sessions: List<AttendanceSessionSummary>,
        val incidents: List<IncidentSummary>,
        val schedule: ScheduleLoadResult?,
    )

    /**
     * One cached read per source, filtered to this group here and not in the
     * screen: the session list and the incident list are the teacher's, and every
     * screen that shows them has to prove it narrowed them down.
     */
    private val basis = combine(
        groups.observeMyGroups(),
        roster,
        history.observeHistory(),
        incidents.observeIncidents(),
        scheduleState,
    ) { myGroups, groupRoster, sessions, incidentList, schedule ->
        val id = groupId.value
        Basis(
            group = myGroups.firstOrNull { it.id == id },
            roster = groupRoster,
            sessions = sessions.filter { it.groupId == id },
            incidents = incidentList.filter { it.groupId == id },
            schedule = schedule,
        )
    }

    private data class View(
        val groupId: String,
        val monthOffset: Int,
        val expandedIncidentId: String?,
        val refreshing: Boolean,
        val notice: String?,
    )

    private val view = combine(
        groupId,
        monthOffset,
        expandedIncidentId,
        refreshing,
        notice,
    ) { id, offset, expanded, isRefreshing, message ->
        View(
            groupId = id,
            monthOffset = offset,
            expandedIncidentId = expanded,
            refreshing = isRefreshing,
            notice = message,
        )
    }

    val state: StateFlow<TeacherGroupDetailUi> = combine(basis, view) { basis, view ->
        val id = view.groupId
        val todayMillis = now()
        val todayIso = AttendanceTimes.isoDate(todayMillis, zone)
        val sessions = basis.sessions

        TeacherGroupDetailUi(
            groupId = id,
            label = basis.group?.label ?: id,
            subject = subjectForGroup(subjects, basis.group?.grade ?: 0) ?: NO_SUBJECT,
            rollCallSubjects = rollCallSubjectsOf(
                slots = (basis.schedule as? ScheduleLoadResult.Loaded)?.slots.orEmpty(),
                groupId = id,
                grade = basis.group?.grade ?: 0,
                currentUid = auth.currentUser?.uid.orEmpty(),
            ),
            slotLabel = (basis.schedule as? ScheduleLoadResult.Loaded)?.let { loaded ->
                groupSlotLabel(
                    slots = loaded.slots,
                    groupId = id,
                    weekday = scheduleWeekday(todayMillis, zone),
                )
            },
            listState = listState(
                groupId = id,
                hasGroup = basis.group != null,
                hasSessionToday = sessions.any { it.date == todayIso },
                isClassDay = SchoolCalendar.isClassDay(todayMillis),
            ),
            attendancePercent = groupAttendancePercent(sessions),
            absencesToday = absencesOn(sessions, todayIso),
            students = groupStudents(basis.roster),
            monthOffset = view.monthOffset,
            canGoNextMonth = view.monthOffset < 0,
            calendar = groupCalendar(
                sessions = sessions,
                now = todayMillis,
                monthOffset = view.monthOffset,
                todayIso = todayIso,
                zone = zone,
            ),
            sessions = groupSessionRows(
                sessions = sessions,
                now = todayMillis,
                monthOffset = view.monthOffset,
                zone = zone,
            ),
            incidents = groupIncidentRows(
                incidents = basis.incidents,
                groupId = id,
                expandedId = view.expandedIncidentId,
                zone = zone,
            ),
            expandedIncidentId = view.expandedIncidentId,
            refreshing = view.refreshing,
            notice = view.notice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TeacherGroupDetailUi(),
    )

    /**
     * Binds the screen to the group of the route and pulls what this phone has
     * not seen yet. Calling it twice for the same group is a no-op, so a
     * recomposition never repeats the refresh.
     */
    fun open(groupId: String) {
        if (groupId.isBlank() || this.groupId.value == groupId) return

        this.groupId.value = groupId
        monthOffset.value = 0
        expandedIncidentId.value = null
        refresh()
    }

    /** `-1` goes back a month, `+1` forward; the current month is the ceiling. */
    fun changeMonth(delta: Int) {
        monthOffset.value = if (delta > 0) {
            nextMonthOffset(monthOffset.value)
        } else {
            monthOffset.value + delta
        }
    }

    /** Opens [incidentId], or closes it when it is already open. */
    fun toggleIncident(incidentId: String) {
        expandedIncidentId.value = if (expandedIncidentId.value == incidentId) {
            null
        } else {
            incidentId
        }
    }

    fun refresh() {
        val id = groupId.value
        if (id.isBlank()) return

        viewModelScope.launch {
            refreshing.value = true

            // The timetable is read live (it is not cached), so it is resolved
            // once per open and never blocks the tabs.
            if (scheduleState.value == null) {
                scheduleState.value = schedule.loadSchedule()
            }

            val groupsResult = groups.refreshMyGroups()
            val sessionsResult = history.refreshHistory()
            val incidentsResult = incidents.refresh()

            notice.value = listOfNotNull(
                (groupsResult as? RosterRefreshResult.Failed)?.reason?.userMessage(),
                (sessionsResult as? HistoryRefreshResult.Failed)?.reason?.userMessage(),
                (incidentsResult as? HistoryRefreshResult.Failed)?.reason?.userMessage(),
            ).firstOrNull()

            refreshing.value = false
        }
    }

    private fun listState(
        groupId: String,
        hasGroup: Boolean,
        hasSessionToday: Boolean,
        isClassDay: Boolean,
    ): GroupListState = when {
        groupId.isBlank() || !hasGroup -> GroupListState.Loading
        !isClassDay -> GroupListState.NoClass
        hasSessionToday -> GroupListState.Done
        else -> GroupListState.Pending
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val NO_SUBJECT = "Sin materia asignada"
    }
}

private fun RosterFailure.userMessage(): String = when (this) {
    RosterFailure.OFFLINE -> "Sin conexión. Estás viendo lo guardado en este dispositivo."
    RosterFailure.DENIED -> "No tienes permiso para ver este grupo."
    RosterFailure.UNKNOWN -> "No se pudo actualizar el grupo."
}
