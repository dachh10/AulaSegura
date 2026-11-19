package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The tutor's home, built from what the school already produced.
 *
 * It keeps the loaded overviews so switching between children re-renders the
 * tasks and the summary without a second round trip: the tutor with two children
 * changes the selector constantly and every tap would otherwise redownload a
 * month of attendance.
 */
class ParentHomeViewModel(
    private val parents: ParentRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentHomeUiState>(ParentHomeUiState.Loading)
    val state: StateFlow<ParentHomeUiState> = _state.asStateFlow()

    private var overviews: List<ParentChildOverview> = emptyList()
    private var selected = 0

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ParentHomeUiState.Loading
            _state.value = when (val result = parents.loadHome()) {
                is ParentHomeResult.Loaded -> show(result.children)
                ParentHomeResult.SignedOut -> ParentHomeUiState.Failed("Vuelve a iniciar sesión.")
                is ParentHomeResult.Failed -> ParentHomeUiState.Failed(
                    homeFailureMessage(result.reason),
                )
            }
        }
    }

    fun select(index: Int) {
        if (index !in overviews.indices || index == selected) return
        selected = index
        _state.value = loaded()
    }

    private fun show(children: List<ParentChildOverview>): ParentHomeUiState {
        overviews = children
        if (children.isEmpty()) return ParentHomeUiState.NoChildren
        if (selected !in children.indices) selected = 0
        return loaded()
    }

    private fun loaded(): ParentHomeUiState.Loaded {
        val moment = now()
        return ParentHomeUiState.Loaded(
            todayLabel = todayHeader(moment, zone),
            children = overviews.map { it.toChildUi() },
            selected = selected,
            tasks = overviews[selected].tasks.map { it.toUi(moment, zone) },
        )
    }
}
