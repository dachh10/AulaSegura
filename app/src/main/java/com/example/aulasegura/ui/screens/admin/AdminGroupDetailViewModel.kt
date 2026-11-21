package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AdminScheduleSlot
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminGroupResult
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminUserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the detail screen: loads one group with its schedule, lists the school's
 * teachers for assignment, and edits the schedule rows.
 *
 * No Firebase types here. The two-sided assignment is delegated to the repository;
 * this class only decides which direction to request from the UI toggle.
 */
class AdminGroupDetailViewModel(
    private val groups: AdminGroupRepository,
    private val users: AdminUserRepository,
    private val students: AdminStudentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminGroupDetailUiState())
    val state: StateFlow<AdminGroupDetailUiState> = _state

    /** Called from the screen once the route's groupId is known, and on retry. */
    fun load(groupId: String) {
        _state.update {
            it.copy(
                groupId = groupId,
                loading = true,
                deleted = false,
                saving = false,
                error = null,
                message = null,
                studentsLoading = true,
                students = emptyList(),
            )
        }
        viewModelScope.launch {
            val groupResult = runCatching { groups.getGroup(groupId) }
            val userResult = runCatching { users.getUsers() }
            val groupsResult = runCatching { groups.getGroups() }
            val studentsResult = runCatching { students.getStudents() }
            val group = groupResult.getOrNull()
            if (group == null) {
                _state.update {
                    it.copy(loading = false, studentsLoading = false, error = "No se pudo cargar el grupo.")
                }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    group = group,
                    users = userResult.getOrNull().orEmpty(),
                    // The destination must be another group of this school; the
                    // repository validates schoolId again before writing.
                    destinationOptions = groupsResult.getOrNull().orEmpty()
                        .filter { it.id != groupId },
                    // The student repository is school-wide; the brief's filter is
                    // applied here so the tab only ever shows this group's students.
                    students = studentsForGroup(studentsResult.getOrNull().orEmpty(), groupId),
                    studentsLoading = false,
                    slots = group.schedule.toMutableList(),
                    error = null,
                )
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun toggleTeacher(teacherUid: String) {
        val current = _state.value.group ?: return
        val assign = teacherUid !in current.teacherIds
        viewModelScope.launch {
            when (val result = groups.assignTeacher(current.id, teacherUid, assign)) {
                AdminGroupResult.Success -> {
                    reloadGroup()
                    _state.update {
                        it.copy(
                            message = if (assign) "Docente asignado al grupo."
                            else "Docente desasignado del grupo.",
                        )
                    }
                }

                is AdminGroupResult.Failure -> _state.update {
                    it.copy(error = result.toMessage())
                }
            }
        }
    }

    /** Applies the edit dialog's fields, after the same pure guard the dialog uses. */
    fun updateGroup(groupId: String, name: String, grade: Int, section: String) {
        val invalid = validationError(
            AdminGroupForm(name = name, grade = grade.toString(), section = section),
        )
        if (invalid != null) {
            _state.update { it.copy(error = invalid) }
            return
        }

        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = groups.updateGroup(groupId, name, grade, section)) {
                AdminGroupResult.Success -> {
                    reloadGroup()
                    _state.update {
                        it.copy(saving = false, message = "Grupo actualizado.")
                    }
                }

                is AdminGroupResult.Failure -> _state.update {
                    it.copy(saving = false, error = result.toMessage())
                }
            }
        }
    }

    /**
     * Deletes a group, moving its students to [destinationGroupId] when it has any.
     * On success [AdminGroupDetailUiState.deleted] flips and the screen navigates
     * back; a typed destination failure stays in the error banner.
     */
    fun deleteGroup(groupId: String, destinationGroupId: String?) {
        _state.update { it.copy(saving = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = groups.deleteGroup(groupId, destinationGroupId)) {
                AdminGroupResult.Success -> _state.update {
                    it.copy(saving = false, deleted = true)
                }

                is AdminGroupResult.Failure -> _state.update {
                    it.copy(saving = false, error = result.toMessage())
                }
            }
        }
    }

    /** Renumbers this group's roster 1..N by paternal surname. */
    fun renumberStudents() {
        val id = _state.value.groupId
        viewModelScope.launch {
            when (val result = students.renumberRoster(id)) {
                is com.example.aulasegura.domain.repository.AdminStudentResult.Success -> load(id)
                is com.example.aulasegura.domain.repository.AdminStudentResult.Failure -> _state.update {
                    it.copy(error = "No se pudo asignar el número de lista.")
                }
            }
        }
    }

    fun addSlot() {
        _state.update {
            it.copy(
                slots = it.slots + AdminScheduleSlot(
                    weekday = 1,
                    startTime = "08:00",
                    endTime = "08:45",
                    subject = "",
                    teacherId = "",
                ),
            )
        }
    }

    fun setSlot(index: Int, slot: AdminScheduleSlot) {
        _state.update { current ->
            if (index !in current.slots.indices) return@update current
            val previous = current.slots[index]
            // A different teacher brings a different subject catalogue. Clear a
            // subject that no longer belongs to them, but keep free text when the
            // new teacher has no subjects so the principal is never blocked.
            val adjusted = if (previous.teacherId != slot.teacherId) {
                val options = subjectOptionsForTeacher(slot.teacherId, current.users)
                if (options.isNotEmpty() && slot.subject !in options) slot.copy(subject = "") else slot
            } else {
                slot
            }
            current.copy(slots = current.slots.toMutableList().also { it[index] = adjusted })
        }
    }

    fun removeSlot(index: Int) {
        _state.update { current ->
            if (index !in current.slots.indices) return@update current
            current.copy(slots = current.slots.toMutableList().also { it.removeAt(index) })
        }
    }

    fun saveSchedule() {
        val groupId = _state.value.group?.id ?: return
        val slots = _state.value.slots
        if (slots.any { it.subject.isBlank() || it.startTime.isBlank() || it.endTime.isBlank() }) {
            _state.update {
                it.copy(error = "Completa la materia y el horario de cada bloque.")
            }
            return
        }
        val timeError = scheduleValidationError(slots)
        if (timeError != null) {
            _state.update { it.copy(error = timeError) }
            return
        }

        viewModelScope.launch {
            when (val result = groups.saveSchedule(groupId, slots)) {
                AdminGroupResult.Success -> {
                    reloadGroup()
                    _state.update { it.copy(message = "Horario guardado.") }
                }

                is AdminGroupResult.Failure -> _state.update {
                    it.copy(error = result.toMessage())
                }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun errorShown() {
        _state.update { it.copy(error = null) }
    }

    /** Refreshes only the group after a write, without a full-screen loading state. */
    private suspend fun reloadGroup() {
        val groupId = _state.value.groupId
        val result = runCatching { groups.getGroup(groupId) }
        val group = result.getOrNull() ?: return
        _state.update {
            it.copy(group = group, slots = group.schedule.toMutableList())
        }
    }
}
