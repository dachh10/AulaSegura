package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminGroupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the school's groups once and applies each principal write, refreshing
 * after success.
 *
 * No Firebase types here: the repository returns domain models and a typed result,
 * so this class is testable without an emulator or a Firestore instance.
 */
class AdminGroupsViewModel(
    private val groups: AdminGroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminGroupsUiState())
    val state: StateFlow<AdminGroupsUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { groups.getGroups() }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(loading = false, groups = it, error = null) },
                    onFailure = {
                        current.copy(loading = false, error = "No se pudieron cargar los grupos.")
                    },
                )
            }
        }
    }

    fun createGroup(form: AdminGroupForm) {
        if (!isValid(form)) return
        val grade = parseGrade(form.grade) ?: return

        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = groups.createGroup(form.name, grade, form.section)) {
                AdminGroupResult.Success -> {
                    _state.update { it.copy(message = "Grupo creado.") }
                    refresh()
                }

                is AdminGroupResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.toMessage())
                }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
