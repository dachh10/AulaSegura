package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.JustificationDecision
import com.example.aulasegura.domain.model.JustificationReview
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.example.aulasegura.domain.repository.JustificationReviewResult
import com.example.aulasegura.ui.screens.justifications.justificationFailureMessage
import com.example.aulasegura.ui.screens.justifications.justificationReviewFailureMessage
import com.example.aulasegura.ui.screens.justifications.justificationSections
import com.example.aulasegura.ui.screens.justifications.toCardUi
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The principal reads every report of the school and decides on the pending
 * ones. The write is a four-field update in [JustificationInboxRepository]; the
 * attendance record is deliberately left alone here.
 */
class AdminJustificationsViewModel(
    private val inbox: JustificationInboxRepository,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(AdminJustificationsUiState())
    val state: StateFlow<AdminJustificationsUiState> = _state.asStateFlow()

    /** One read answers both the initial composition and the resume. */
    private var refreshing = false

    init {
        refresh()
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

    private suspend fun load() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = inbox.loadForSchool()) {
            is JustificationInboxResult.Loaded -> {
                val sections = justificationSections(result.justifications)
                _state.update {
                    it.copy(
                        loading = false,
                        pending = sections.pending.map { record -> record.toCardUi(zone) },
                        reviewed = sections.reviewed.map { record -> record.toCardUi(zone) },
                    )
                }
            }

            JustificationInboxResult.SignedOut -> _state.update {
                it.copy(loading = false, error = "Vuelve a iniciar sesión.")
            }

            is JustificationInboxResult.Failed -> _state.update {
                it.copy(loading = false, error = justificationFailureMessage(result.reason))
            }
        }
    }

    fun decide(justificationId: String, approve: Boolean, note: String) {
        // A double tap on the confirmation button would otherwise fire two writes.
        if (_state.value.busyId != null) return
        _state.update { it.copy(busyId = justificationId, error = null, notice = null) }
        viewModelScope.launch {
            val request = JustificationReview(
                justificationId = justificationId,
                decision = if (approve) JustificationDecision.APPROVE else JustificationDecision.REJECT,
                note = note,
            )
            when (val result = inbox.review(request)) {
                JustificationReviewResult.Success -> {
                    _state.update {
                        it.copy(
                            busyId = null,
                            notice = if (approve) {
                                "Justificación aprobada."
                            } else {
                                "Justificación rechazada."
                            },
                        )
                    }
                    // Reload in place: refresh() would be skipped if a resume read
                    // happened to be in flight, leaving the card on the old status.
                    load()
                }

                is JustificationReviewResult.Failed -> _state.update {
                    it.copy(
                        busyId = null,
                        error = justificationReviewFailureMessage(result.reason),
                    )
                }

                JustificationReviewResult.SignedOut -> _state.update {
                    it.copy(busyId = null, error = "Vuelve a iniciar sesión.")
                }
            }
        }
    }

    fun noticeShown() = _state.update { it.copy(notice = null) }

    fun errorShown() = _state.update { it.copy(error = null) }
}
