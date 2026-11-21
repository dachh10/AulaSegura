package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminStudentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UngroupedStudentsUiState(
    val students: List<AdminStudent> = emptyList(),
    val groups: List<AdminGroup> = emptyList(),
    val query: String = "",
    val selected: Set<String> = emptySet(),
    val grado: Int? = null,
    val grupo: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val ungrouped: List<AdminStudent> get() = ungroupedStudents(students, query)

    /** One row per grade available in the school's real groups. */
    val grados: List<Int> get() = groups.map { it.grade }.distinct().sorted()

    /** Sections offered for the chosen grade. */
    val secciones: List<String> get() = groups
        .filter { grado == null || it.grade == grado }
        .map { it.section }
        .distinct()
        .sorted()
}

/** Drives the director's "Alumnos sin grupo" bulk-assignment screen. No Firebase types. */
class UngroupedStudentsViewModel(
    private val students: AdminStudentRepository,
    private val groups: AdminGroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UngroupedStudentsUiState())
    val state: StateFlow<UngroupedStudentsUiState> = _state

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val studentResult = runCatching { students.getStudents() }
            val groupResult = runCatching { groups.getGroups() }
            _state.update { current ->
                current.copy(
                    loading = false,
                    students = studentResult.getOrDefault(emptyList()),
                    groups = groupResult.getOrDefault(emptyList()),
                    error = if (studentResult.isFailure || groupResult.isFailure) {
                        "No se pudieron cargar los alumnos."
                    } else null,
                )
            }
        }
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query, message = null) }

    fun toggle(id: String) = _state.update {
        it.copy(
            selected = if (id in it.selected) it.selected - id else it.selected + id,
            message = null,
        )
    }

    fun toggleAll() = _state.update {
        val visible = it.ungrouped.map { student -> student.id }.toSet()
        it.copy(
            selected = if (visible.isNotEmpty() && visible.all { id -> id in it.selected }) {
                emptySet()
            } else {
                visible
            },
            message = null,
        )
    }

    fun onGradoChange(grado: Int) = _state.update {
        it.copy(grado = grado, grupo = "", message = null)
    }

    fun onGrupoChange(grupo: String) = _state.update { it.copy(grupo = grupo, message = null) }

    fun assign() {
        val current = _state.value
        if (current.loading || current.selected.isEmpty()) return
        val target = groupForAssignment(current.groups, current.grado ?: 0, current.grupo)
        if (target == null) {
            _state.update { it.copy(error = "Elige un grado y un grupo válidos.") }
            return
        }
        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            val result = students.assignGroup(
                studentIds = current.selected.toList(),
                groupId = target.id,
                grado = target.grade,
                grupo = target.section,
            )
            _state.update {
                when (result) {
                    is AdminStudentResult.Success -> it.copy(
                        loading = false,
                        selected = emptySet(),
                        message = "${current.selected.size} alumnos asignados a ${target.name}.",
                    ).also { refresh() }
                    is AdminStudentResult.Failure -> it.copy(
                        loading = false,
                        error = "No se pudo asignar el grupo. Inténtalo de nuevo.",
                    )
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null, error = null) }
}
