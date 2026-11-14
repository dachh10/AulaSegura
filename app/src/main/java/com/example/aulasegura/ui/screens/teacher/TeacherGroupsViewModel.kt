package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.RosterRefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Mis grupos".
 *
 * The list itself is a `Flow` from the cache, so the screen keeps working with
 * no connection and redraws by itself when the sync worker writes. The refresh
 * runs once on open; its failure never clears the list, because stale groups are
 * still better than an empty screen for a teacher standing in a classroom.
 */
class TeacherGroupsViewModel(
    private val groups: GroupRepository,
    auth: AuthRepository,
) : ViewModel() {

    data class UiState(
        val groups: List<TeacherGroupUi> = emptyList(),
        val refreshing: Boolean = true,
        val notice: String? = null,
    )

    // Resolved once: the subject comes from the profile, and the profile does not
    // change while this screen is alive.
    private val subject: String? = auth.currentUser?.subjects?.firstOrNull()

    private val refreshing = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)

    val state: StateFlow<UiState> = combine(
        groups.observeMyGroups(),
        refreshing,
        notice,
    ) { cached, isRefreshing, message ->
        UiState(
            groups = cached.map { it.toUi(subject) },
            refreshing = isRefreshing,
            notice = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            notice.value = when (val result = groups.refreshMyGroups()) {
                is RosterRefreshResult.Updated -> null
                RosterRefreshResult.SignedOut -> null
                is RosterRefreshResult.Failed -> result.reason.userMessage()
            }
            refreshing.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun RosterFailure.userMessage(): String = when (this) {
    RosterFailure.OFFLINE -> "Sin conexión. Estás viendo los grupos guardados."
    RosterFailure.DENIED -> "No tienes permiso para ver los grupos de este plantel."
    RosterFailure.UNKNOWN -> "No se pudieron actualizar los grupos."
}
