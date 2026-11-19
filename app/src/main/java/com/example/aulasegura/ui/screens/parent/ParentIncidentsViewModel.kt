package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The incidents of the child the pill shows.
 *
 * Every child's incidents are loaded once and filtered in memory, so switching
 * child costs no reads and a sibling cannot hide the other's reports.
 */
class ParentIncidentsViewModel(
    private val parents: ParentRepository,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentIncidentsUiState>(ParentIncidentsUiState.Loading)
    val state: StateFlow<ParentIncidentsUiState> = _state.asStateFlow()

    private var children: List<ParentChild> = emptyList()
    private var selected = 0
    private var incidents: List<ParentIncident> = emptyList()

    init {
        loadChildren()
    }

    fun refresh() {
        if (children.isEmpty()) loadChildren() else loadIncidents()
    }

    /** One pill and no selector, exactly like attendance and tasks. */
    fun cycleChild() {
        if (children.size <= 1) return
        selected = (selected + 1) % children.size
        publish()
    }

    private fun loadChildren() {
        viewModelScope.launch {
            _state.value = ParentIncidentsUiState.Loading
            when (val result = parents.loadChildren()) {
                is ParentChildrenResult.Loaded -> {
                    children = result.children
                    if (children.isEmpty()) {
                        _state.value = ParentIncidentsUiState.NoChildren
                    } else {
                        if (selected !in children.indices) selected = 0
                        loadIncidents()
                    }
                }

                ParentChildrenResult.SignedOut ->
                    _state.value = ParentIncidentsUiState.Failed("Vuelve a iniciar sesión.")

                is ParentChildrenResult.Failed ->
                    _state.value = ParentIncidentsUiState.Failed(
                        incidentsFailureMessage(result.reason),
                    )
            }
        }
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            _state.value = ParentIncidentsUiState.Loading
            when (val result = parents.loadIncidents()) {
                is ParentIncidentsResult.Loaded -> {
                    incidents = result.incidents
                    publish()
                }

                ParentIncidentsResult.SignedOut ->
                    _state.value = ParentIncidentsUiState.Failed("Vuelve a iniciar sesión.")

                is ParentIncidentsResult.Failed ->
                    _state.value = ParentIncidentsUiState.Failed(
                        incidentsFailureMessage(result.reason),
                    )
            }
        }
    }

    private fun publish() {
        val child = children.getOrNull(selected) ?: return
        val mine = incidentsOf(incidents, child.studentId)

        _state.value = ParentIncidentsUiState.Loaded(
            childName = child.firstName.ifBlank { child.fullName },
            groupLabel = child.groupLabel,
            childCount = children.size,
            total = mine.size,
            newCount = newIncidents(mine),
            graveCount = graveIncidents(mine),
            incidents = mine.map { it.toRowUi(zone) },
        )
    }
}
