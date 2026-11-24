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
 * ones. [JustificationInboxRepository] commits the review together with the
 * attendance flip; this screen only reports the result.
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

    // Read-only on purpose: the teacher who marked the absence owns the decision.
    // The principal keeps this screen only for supervision.

    fun noticeShown() = _state.update { it.copy(notice = null) }

    fun errorShown() = _state.update { it.copy(error = null) }
}
