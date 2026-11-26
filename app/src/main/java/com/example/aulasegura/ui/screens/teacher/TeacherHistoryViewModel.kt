package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.HistoryRefreshResult
import com.example.aulasegura.domain.repository.IncidentHistoryRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.TaskRepository
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Historial".
 *
 * Same shape as [TeacherGroupsViewModel]: the lists are cache `Flow`s, so they
 * survive a rotation, redraw when the sync worker writes and open without a
 * connection. The refresh runs once on open and its failure never clears a list —
 * a teacher who reported an incident in the classroom still has to be able to
 * read it an hour later at home, with no signal.
 *
 * Both segments are loaded together: switching between "Listas" and "Incidentes"
 * must not wait on the network for the second one.
 *
 * Expanding a row is a separate job per segment: only one row is open at a time,
 * and switching rows cancels the previous collection instead of leaving it
 * running.
 */
class TeacherHistoryViewModel(
    private val history: AttendanceHistoryRepository,
    private val incidents: IncidentHistoryRepository,
    private val tasks: TaskRepository,
    private val notices: NoticeRepository,
    private val today: () -> String = { AttendanceTimes.isoDate(System.currentTimeMillis()) },
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val refreshing = MutableStateFlow(true)

    /** `0` is the current month; the header arrows move it backwards. */
    private val monthOffset = MutableStateFlow(0)
    private val notice = MutableStateFlow<String?>(null)

    private val expandedId = MutableStateFlow<String?>(null)
    private val detail = MutableStateFlow<AttendanceSessionDetail?>(null)
    private val detailLoading = MutableStateFlow(false)

    private val expandedIncidentId = MutableStateFlow<String?>(null)
    private val evidence = MutableStateFlow<List<IncidentEvidence>>(emptyList())

    private var detailJob: Job? = null
    private var evidenceJob: Job? = null

    private data class Lists(
        val sessions: List<AttendanceSessionSummary>,
        val incidents: List<IncidentSummary>,
        val refreshing: Boolean,
        val notice: String?,
    )

    private data class Open(
        val sessionId: String?,
        val detail: AttendanceSessionDetail?,
        val detailLoading: Boolean,
        val incidentId: String?,
        val evidence: List<IncidentEvidence>,
    )

    /**
     * Tasks and notices of this device, merged later into one timeline.
     *
     * No refresh behind this one: both lists are Room-only by design (a docente
     * cannot prove the array filters the read rules require), so there is nothing
     * to pull from the server here.
     */
    private val publications = combine(
        tasks.observeMyTasks(),
        notices.observeMyNotices(),
    ) { taskList, noticeList -> Publications(tasks = taskList, notices = noticeList) }

    private data class Publications(
        val tasks: List<PublishedTask>,
        val notices: List<PublishedNotice>,
    )

    private val lists = combine(
        history.observeHistory(),
        incidents.observeIncidents(),
        refreshing,
        notice,
    ) { sessions, incidentList, isRefreshing, message ->
        Lists(
            sessions = sessions,
            incidents = incidentList,
            refreshing = isRefreshing,
            notice = message,
        )
    }

    private val open = combine(
        expandedId,
        detail,
        detailLoading,
        expandedIncidentId,
        evidence,
    ) { sessionId, sessionDetail, loading, incidentId, files ->
        Open(
            sessionId = sessionId,
            detail = sessionDetail,
            detailLoading = loading,
            incidentId = incidentId,
            evidence = files,
        )
    }

    val state: StateFlow<TeacherHistoryUi> = combine(lists, publications, open, monthOffset) { cached, posts, shown, offset ->
        // One month at a time, and the same month for the three segments: the
        // header is shared, so a list showing October next to incidents from
        // November would be the screen contradicting itself.
        val target = monthKeyBefore(now(), offset, zone)
        val sessions = cached.sessions.filter { monthKeyOfIso(it.date, zone) == target }
        val incidents = cached.incidents.filter { monthKey(it.occurredAt, zone) == target }
        val tasks = posts.tasks.filter { monthKey(it.createdAt, zone) == target }
        val notices = posts.notices.filter { monthKey(it.createdAt, zone) == target }

        TeacherHistoryUi(
            monthOffset = offset,
            monthLabel = monthLabel(now(), offset, zone),
            canGoNextMonth = offset < 0,
            sections = historySections(
                sessions = sessions,
                todayIso = today(),
                expandedId = shown.sessionId,
                detail = shown.detail,
                detailLoading = shown.detailLoading,
            ),
            incidentSections = incidentSections(
                incidents = incidents,
                todayIso = today(),
                expandedId = shown.incidentId,
                evidence = shown.evidence,
            ),
            publications = publicationRows(tasks = tasks, notices = notices),
            refreshing = cached.refreshing,
            notice = cached.notice,
            expandedId = shown.sessionId,
            expandedIncidentId = shown.incidentId,
            detailLoading = shown.detailLoading,
            totalSessions = sessions.size,
            totalIncidents = incidents.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TeacherHistoryUi(),
    )

    init {
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

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            val sessions = history.refreshHistory()
            val incidentList = incidents.refresh()

            // One banner for both: two notices stacked would say the same thing
            // twice, and the teacher has one retry button.
            notice.value = listOf(sessions, incidentList)
                .filterIsInstance<HistoryRefreshResult.Failed>()
                .firstOrNull()
                ?.reason
                ?.userMessage()
            refreshing.value = false
        }
    }

    /**
     * Opens [sessionId], or closes it when it is already open.
     *
     * The cached records paint first (a session taken on this phone already has
     * them), and the server read runs behind: expanding a row must not wait on the
     * network to show what the device already knows.
     */
    fun toggleSession(sessionId: String) {
        if (expandedId.value == sessionId) {
            closeDetail()
            return
        }

        expandedId.value = sessionId
        detail.value = null
        detailLoading.value = true

        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            history.observeDetail(sessionId).collect { detail.value = it }
        }

        viewModelScope.launch {
            // Best effort: with no connection the cache answers and the row still
            // opens, just without the records this device never saw.
            history.refreshRecords(sessionId)
            detailLoading.value = false
        }
    }

    /**
     * Opens [incidentId], or closes it when it is already open.
     *
     * There is no server read behind this one: the incident document is already
     * cached by the list refresh, and its evidence rows came with it.
     */
    fun toggleIncident(incidentId: String) {
        if (expandedIncidentId.value == incidentId) {
            closeIncident()
            return
        }

        expandedIncidentId.value = incidentId
        evidence.value = emptyList()

        evidenceJob?.cancel()
        evidenceJob = viewModelScope.launch {
            incidents.observeEvidence(incidentId).collect { evidence.value = it }
        }
    }

    private fun closeDetail() {
        detailJob?.cancel()
        detailJob = null
        expandedId.value = null
        detail.value = null
        detailLoading.value = false
    }

    private fun closeIncident() {
        evidenceJob?.cancel()
        evidenceJob = null
        expandedIncidentId.value = null
        evidence.value = emptyList()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun RosterFailure.userMessage(): String = when (this) {
    RosterFailure.OFFLINE -> "Sin conexión. Estás viendo el historial guardado."
    RosterFailure.DENIED -> "No tienes permiso para ver este historial."
    RosterFailure.NO_GROUP -> "Esta cuenta todavía no tiene un grupo asignado."
    RosterFailure.UNKNOWN -> "No se pudo actualizar el historial."
}
