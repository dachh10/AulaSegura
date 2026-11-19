package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.CreateStudentResult
import com.example.aulasegura.domain.repository.LinkingCodeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the new-student form and exposes the linking code after a successful save.
 *
 * No Firebase types here: the repositories return domain models and typed results,
 * so this class stays testable without an emulator or a Firestore instance.
 *
 * [presetGroupId] is the group a caller came from. It wins over the first group in
 * the list whenever that id is actually present in the school's loaded groups.
 */
class AdminNewStudentViewModel(
    private val students: AdminStudentRepository,
    private val groups: AdminGroupRepository,
    private val presetGroupId: String = "",
) : ViewModel() {

    private val _state = MutableStateFlow(AdminNewStudentUiState())
    val state: StateFlow<AdminNewStudentUiState> = _state

    init {
        loadGroups()
    }

    fun loadGroups() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { groups.getGroups() }
            _state.update { current ->
                result.fold(
                    onSuccess = { loaded ->
                        current.copy(
                            loading = false,
                            groups = loaded,
                            form = current.form.copy(
                                // A group card or the detail screen preselects its own
                                // group; fall back to the first only when that preset
                                // is absent from the loaded school.
                                groupId = loaded.firstOrNull { it.id == presetGroupId }?.id
                                    ?: loaded.firstOrNull()?.id.orEmpty(),
                            ),
                        )
                    },
                    onFailure = { current.copy(loading = false, error = "No se pudieron cargar los grupos.") },
                )
            }
        }
    }

    fun onFormChange(form: AdminStudentForm) {
        _state.update { it.copy(form = form, error = null) }
    }

    fun submit() {
        val form = _state.value.form
        val error = validationError(form)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }

        _state.update { it.copy(loading = true, error = null, linkingCode = null, message = null) }
        viewModelScope.launch {
            when (val result = students.createStudent(form.toInput())) {
                is CreateStudentResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.toMessage())
                }

                is CreateStudentResult.Success -> when (val code = students.createLinkingCode(result.studentId)) {
                    is LinkingCodeResult.Success -> _state.update {
                        it.copy(
                            loading = false,
                            linkingCode = code.code,
                            message = "Alumno guardado.",
                        )
                    }

                    is LinkingCodeResult.Failure -> _state.update {
                        it.copy(
                            loading = false,
                            // The student is already saved; never imply a rollback
                            // that did not happen, but make the missing code clear.
                            error = code.toMessage(),
                        )
                    }
                }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
