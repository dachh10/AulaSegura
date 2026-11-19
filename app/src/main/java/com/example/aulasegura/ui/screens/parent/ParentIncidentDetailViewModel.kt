package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.ParentIncidentResult
import com.example.aulasegura.domain.repository.ParentRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One incident, read by document id, shared by the detail and the map.
 *
 * "Enterado" is not optimistic: the tutor cannot remove themselves from
 * `seenByTutorIds` (the rules only allow adding), so showing the state before the
 * cloud accepted it would leave a mark on screen that can never come back.
 */
class ParentIncidentDetailViewModel(
    private val parents: ParentRepository,
    private val incidentId: String,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state =
        MutableStateFlow<ParentIncidentDetailUiState>(ParentIncidentDetailUiState.Loading)
    val state: StateFlow<ParentIncidentDetailUiState> = _state.asStateFlow()

    private var incident: ParentIncident? = null
    private var marking = false
    private var error: String? = null

    init {
        load()
    }

    fun refresh() = load()

    fun acknowledge() {
        val current = incident ?: return
        if (current.seenByMe || marking) return

        marking = true
        error = null
        publish()

        viewModelScope.launch {
            when (val result = parents.markIncidentSeen(current.incidentId)) {
                IncidentSeenResult.Marked -> incident = current.copy(seenByMe = true)
                IncidentSeenResult.SignedOut -> error = "Vuelve a iniciar sesión."
                is IncidentSeenResult.Failed -> error = acknowledgeFailureMessage()
            }
            marking = false
            publish()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ParentIncidentDetailUiState.Loading
            error = null

            when (val result = parents.loadIncident(incidentId)) {
                is ParentIncidentResult.Loaded -> {
                    incident = result.incident
                    marking = false
                    publish()
                }

                ParentIncidentResult.NotFound -> {
                    incident = null
                    _state.value = ParentIncidentDetailUiState.NotFound
                }

                ParentIncidentResult.SignedOut ->
                    _state.value = ParentIncidentDetailUiState.Failed("Vuelve a iniciar sesión.")

                is ParentIncidentResult.Failed ->
                    _state.value = ParentIncidentDetailUiState.Failed(
                        incidentDetailFailureMessage(result.reason),
                    )
            }
        }
    }

    private fun publish() {
        _state.value = incident?.toDetailUi(marking = marking, error = error, zone = zone)
            ?: ParentIncidentDetailUiState.Loading
    }
}
