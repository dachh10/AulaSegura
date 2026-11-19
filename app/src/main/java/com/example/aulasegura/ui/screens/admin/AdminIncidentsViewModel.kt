package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.repository.AdminIncidentRepository
import com.example.aulasegura.domain.repository.AdminIncidentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the incident inbox with domain models and typed results only.
 *
 * No Firebase types here: the repository returns `AdminIncident` values, so the
 * whole state machine can be exercised with a fake in a unit test.
 */
class AdminIncidentsViewModel(
    private val incidents: AdminIncidentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminIncidentsUiState())
    val state: StateFlow<AdminIncidentsUiState> = _state

    /** Stale list responses from an earlier request must not overwrite a newer one. */
    private var requestId = 0

    init {
        refresh()
    }

    fun setSeverityFilter(severity: IncidentSeverity?) {
        if (_state.value.severityFilter == severity) return
        _state.update { it.copy(severityFilter = severity) }
    }

    fun setStatusFilter(status: IncidentStatus?) {
        if (_state.value.statusFilter == status) return
        _state.update { it.copy(statusFilter = status) }
    }

    /** Status is the only incident field this flow writes; evidence is never deleted. */
    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    fun setStatus(incidentId: String, status: IncidentStatus) {
        if (_state.value.updating) return
        _state.update { it.copy(updating = true, actionError = null) }
        viewModelScope.launch {
            when (val result = incidents.setStatus(incidentId, status)) {
                is AdminIncidentResult.Success -> {
                    _state.update { it.copy(updating = false, actionError = null) }
                    refresh()
                }
                is AdminIncidentResult.Failure -> _state.update {
                    it.copy(updating = false, actionError = result.reason.toMessage())
                }
            }
        }
    }

    fun refresh() {
        val id = ++requestId
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = incidents.getIncidents()) {
                is AdminIncidentResult.Success -> {
                    if (id != requestId) return@launch
                    _state.update {
                        it.copy(loading = false, incidents = result.data, error = null)
                    }
                }
                is AdminIncidentResult.Failure -> {
                    if (id != requestId) return@launch
                    _state.update { it.copy(loading = false, error = result.reason.toMessage()) }
                }
            }
        }
    }
}
