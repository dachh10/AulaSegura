package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.model.JustificationDecision
import com.example.aulasegura.domain.model.JustificationReview
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.example.aulasegura.domain.repository.JustificationReviewResult
import com.example.aulasegura.ui.screens.justifications.justificationReviewFailureMessage
import com.example.aulasegura.ui.screens.justifications.justificationFailureMessage
import com.example.aulasegura.ui.screens.justifications.justificationSections
import com.example.aulasegura.ui.screens.justifications.toCardUi
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The teacher's justification inbox: the families' reports for the groups they
 * teach.
 *
 * The groups come from the same cache the rest of the teacher screens use; the
 * reports come online because a cached empty list would hide a pending report.
 */
class TeacherJustificationsViewModel(
    private val groups: GroupRepository,
    private val inbox: JustificationInboxRepository,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<TeacherJustificationsUiState>(
        TeacherJustificationsUiState.Loading,
    )
    val state: StateFlow<TeacherJustificationsUiState> = _state.asStateFlow()

    /**
     * Guards the first frame: the screen re-reads on resume, and the initial
     * composition is a resume too. One load answers both.
     */
    private var refreshing = false

    init {
        refresh()
    }

    fun decide(justificationId: String, approve: Boolean, note: String) {
        val current = _state.value as? TeacherJustificationsUiState.Loaded ?: return
        if (current.busyId != null) return
        _state.value = current.copy(busyId = justificationId, error = null, notice = null)
        viewModelScope.launch {
            val request = JustificationReview(
                justificationId = justificationId,
                decision = if (approve) JustificationDecision.APPROVE else JustificationDecision.REJECT,
                note = note,
            )
            when (val result = inbox.review(request)) {
                JustificationReviewResult.Success -> {
                    load()
                    (_state.value as? TeacherJustificationsUiState.Loaded)?.let {
                        _state.value = it.copy(
                            notice = if (approve) {
                                "Justificación aprobada. La falta marcó como justificada."
                            } else {
                                "Justificación rechazada."
                            },
                        )
                    }
                }
                is JustificationReviewResult.Failed ->
                    (_state.value as? TeacherJustificationsUiState.Loaded)?.let {
                        _state.value = it.copy(
                            busyId = null,
                            error = justificationReviewFailureMessage(result.reason),
                        )
                    }
                JustificationReviewResult.SignedOut ->
                    (_state.value as? TeacherJustificationsUiState.Loaded)?.let {
                        _state.value = it.copy(busyId = null, error = "Vuelve a iniciar sesión.")
                    }
            }
        }
    }

    fun noticeShown() {
        (_state.value as? TeacherJustificationsUiState.Loaded)?.let {
            _state.value = it.copy(notice = null)
        }
    }

    fun errorShown() {
        (_state.value as? TeacherJustificationsUiState.Loaded)?.let {
            _state.value = it.copy(error = null)
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try {
                load()
            } finally {
                refreshing = false
            }
        }
    }

    /** Called directly after a decision so a in-flight resume cannot skip the reload. */
    private suspend fun load() {
        _state.value = TeacherJustificationsUiState.Loading

        // A roster read that fails leaves the inbox empty rather than claiming
        // "no reports": the repository still gets a chance to answer for the
        // groups it knows.
        val groupIds = runCatching {
            groups.observeMyGroups().first().map { it.id }
        }.getOrDefault(emptyList())

        when (val result = inbox.loadForGroups(groupIds)) {
            is JustificationInboxResult.Loaded -> {
                val sections = justificationSections(result.justifications)
                val previous = _state.value as? TeacherJustificationsUiState.Loaded
                _state.value = TeacherJustificationsUiState.Loaded(
                    pending = sections.pending.map { it.toCardUi(zone) },
                    reviewed = sections.reviewed.map { it.toCardUi(zone) },
                    notice = previous?.notice,
                    error = previous?.error,
                )
            }

            JustificationInboxResult.SignedOut ->
                _state.value = TeacherJustificationsUiState.Failed("Vuelve a iniciar sesión.")

            is JustificationInboxResult.Failed ->
                _state.value = TeacherJustificationsUiState.Failed(
                    justificationFailureMessage(result.reason),
                )
        }
    }
}
