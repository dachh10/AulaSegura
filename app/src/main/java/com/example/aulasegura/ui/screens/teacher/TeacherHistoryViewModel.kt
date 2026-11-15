package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.HistoryRefreshResult
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
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
 * Same shape as [TeacherGroupsViewModel]: the list is a cache `Flow`, so it
 * survives a rotation, redraws when the sync worker writes and opens without a
 * connection. The refresh runs once on open and its failure never clears the
 * list — a teacher who saved a roll call in the classroom still has to be able to
 * see it an hour later at home, with no signal.
 *
 * Expanding a row is a separate job: only one session is open at a time, and
 * switching rows cancels the previous collection instead of leaving it running.
 */
class TeacherHistoryViewModel(
    private val history: AttendanceHistoryRepository,
    private val today: () -> String = { AttendanceTimes.isoDate(System.currentTimeMillis()) },
) : ViewModel() {

    private val refreshing = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)
    private val expandedId = MutableStateFlow<String?>(null)
    private val detail = MutableStateFlow<AttendanceSessionDetail?>(null)
    private val detailLoading = MutableStateFlow(false)

    private var detailJob: Job? = null

    private data class Base(
        val sessions: List<AttendanceSessionSummary>,
        val refreshing: Boolean,
        val notice: String?,
    )

    private val base = combine(
        history.observeHistory(),
        refreshing,
        notice,
    ) { sessions, isRefreshing, message ->
        Base(sessions = sessions, refreshing = isRefreshing, notice = message)
    }

    val state: StateFlow<TeacherHistoryUi> = combine(
        base,
        expandedId,
        detail,
        detailLoading,
    ) { cached, expanded, sessionDetail, loading ->
        TeacherHistoryUi(
            sections = historySections(
                sessions = cached.sessions,
                todayIso = today(),
                expandedId = expanded,
                detail = sessionDetail,
                detailLoading = loading,
            ),
            refreshing = cached.refreshing,
            notice = cached.notice,
            expandedId = expanded,
            detailLoading = loading,
            totalSessions = cached.sessions.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TeacherHistoryUi(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            notice.value = when (val result = history.refreshHistory()) {
                is HistoryRefreshResult.Updated -> null
                HistoryRefreshResult.SignedOut -> null
                is HistoryRefreshResult.Failed -> result.reason.userMessage()
            }
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

    private fun closeDetail() {
        detailJob?.cancel()
        detailJob = null
        expandedId.value = null
        detail.value = null
        detailLoading.value = false
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun RosterFailure.userMessage(): String = when (this) {
    RosterFailure.OFFLINE -> "Sin conexión. Estás viendo el historial guardado."
    RosterFailure.DENIED -> "No tienes permiso para ver este historial."
    RosterFailure.UNKNOWN -> "No se pudo actualizar el historial."
}
