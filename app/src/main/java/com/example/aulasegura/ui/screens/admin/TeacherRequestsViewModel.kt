package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateError
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.BatchResult
import com.example.aulasegura.domain.repository.TeacherRequestFilter
import com.example.aulasegura.domain.repository.acceptAllTeacherUids
import com.example.aulasegura.domain.repository.pendingTeacherCount
import com.example.aulasegura.domain.repository.teacherRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherRequestsUiState(
    val users: List<AdminUser> = emptyList(),
    val filter: TeacherRequestFilter = TeacherRequestFilter.PENDIENTES,
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val visible: List<AdminUser> get() = teacherRequests(users, filter)
    val pendingCount: Int get() = pendingTeacherCount(users)
}

/** Drives the director's teacher-approval queue. No Firebase types. */
class TeacherRequestsViewModel(
    private val users: AdminUserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherRequestsUiState())
    val state: StateFlow<TeacherRequestsUiState> = _state

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { users.getUsers() }.fold(
                onSuccess = { loaded -> _state.update { it.copy(loading = false, users = loaded) } },
                onFailure = { _state.update { it.copy(loading = false, error = "No se pudieron cargar las solicitudes.") } },
            )
        }
    }

    fun onFilterChange(filter: TeacherRequestFilter) = _state.update { it.copy(filter = filter) }

    fun decide(uid: String, approve: Boolean) {
        val estado = if (approve) "aprobado" else "rechazado"
        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = users.setTeacherStatus(uid, estado)) {
                is AdminUserUpdateResult.Success -> {
                    refresh()
                    _state.update {
                        it.copy(
                            loading = false,
                            message = if (approve) "Docente aprobado." else "Solicitud rechazada.",
                        )
                    }
                }
                is AdminUserUpdateResult.Failure -> _state.update {
                    it.copy(
                        loading = false,
                        error = when (result.reason) {
                            AdminUserUpdateError.SUBJECT_TAKEN ->
                                "La materia que eligió ya tiene docente en ese grupo. La solicitud sigue pendiente."
                            AdminUserUpdateError.DENIED ->
                                "La escuela no pudo asignar el grupo. Revisa las reglas de Firestore o el grupo elegido."
                            AdminUserUpdateError.NETWORK, AdminUserUpdateError.TIMEOUT ->
                                "Sin conexión. La solicitud sigue pendiente; inténtalo de nuevo."
                            else -> "No se pudo actualizar la solicitud."
                        },
                    )
                }
            }
        }
    }

    fun acceptAll() {
        val count = acceptAllTeacherUids(_state.value.users).size
        if (count == 0) {
            _state.update { it.copy(message = "No hay solicitudes pendientes.", error = null) }
            return
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = users.acceptAllPendingTeachers()) {
                is BatchResult.Success -> {
                    refresh()
                    _state.update {
                        it.copy(loading = false, message = "${result.changed} docentes aprobados.")
                    }
                }
                is BatchResult.Failure -> _state.update {
                    it.copy(loading = false, error = "No se pudieron aprobar las solicitudes.")
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null, error = null) }
}
