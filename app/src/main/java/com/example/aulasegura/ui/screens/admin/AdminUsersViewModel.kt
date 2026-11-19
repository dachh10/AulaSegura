package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AccountCreationError
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminStudentResult
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.CreateAccountResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the school's users and students once and edits one user profile at a time.
 *
 * No Firebase types here: the repositories return domain models and typed results,
 * so this class stays testable without an emulator or a Firestore instance.
 * Students are a separate collection with a separate load state: a failed students
 * read must not blank out Docentes/Padres.
 */
class AdminUsersViewModel(
    private val users: AdminUserRepository,
    private val students: AdminStudentRepository,
    private val groups: AdminGroupRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state

    init {
        refresh()
    }

    /** Reloads the collections; used by the screen's retry action. */
    fun refresh() {
        refreshUsers()
        refreshStudents()
        refreshGroupNames()
    }

    /**
     * Resolves the real name of every group so rows and the filter show "4° D"
     * instead of a random Firestore id. Seed ids are absent from the map and fall
     * back to the `grp-4d` formatting in [groupLabel].
     */
    private fun refreshGroupNames() {
        val repository = groups ?: return
        viewModelScope.launch {
            val result = runCatching { repository.getGroups() }
            result.onSuccess { list ->
                _state.update {
                    it.copy(groupNames = list.associate { group -> group.id to group.name })
                }
            }
        }
    }

    private fun refreshUsers() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { users.getUsers() }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(loading = false, users = it, error = null) },
                    onFailure = {
                        current.copy(loading = false, error = "No se pudieron cargar los usuarios.")
                    },
                )
            }
        }
    }

    private fun refreshStudents() {
        _state.update { it.copy(studentsLoading = true, studentsError = null) }
        viewModelScope.launch {
            val result = runCatching { students.getStudents() }
            _state.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(studentsLoading = false, students = it, studentsError = null)
                    },
                    onFailure = {
                        current.copy(
                            studentsLoading = false,
                            studentsError = "No se pudieron cargar los alumnos.",
                        )
                    },
                )
            }
        }
    }

    fun save(form: AdminUserForm) {
        if (!isValid(form)) return

        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            when (val result = users.updateProfile(form.uid, buildUpdatePayload(form))) {
                AdminUserUpdateResult.Success -> {
                    _state.update {
                        it.copy(loading = false, message = "Perfil actualizado.")
                    }
                    // Only the users changed. Re-reading students here would burn
                    // Spark's read budget for nothing.
                    refreshUsers()
                }

                is AdminUserUpdateResult.Failure -> {
                    _state.update {
                        it.copy(loading = false, error = "No se pudo guardar el perfil.")
                    }
                }
            }
        }
    }

    /** Creates a docente or tutor and refreshes the list so the new row appears. */
    fun createAccount(form: CreateAccountForm, role: AccountRole) {
        val validationError = createAccountValidationError(form)
        if (validationError != null) {
            _state.update { it.copy(createError = validationError) }
            return
        }

        _state.update {
            it.copy(createLoading = true, createError = null, createSuccess = null)
        }
        viewModelScope.launch {
            when (val result = users.createAccount(
                email = form.email,
                password = form.password,
                firstName = form.firstName,
                lastName = form.lastName,
                role = role,
                phone = form.phone,
                subjects = parseCsv(form.subjects),
                groupIds = parseCsv(form.groupIds),
            )) {
                is CreateAccountResult.Success -> {
                    _state.update {
                        it.copy(
                            createLoading = false,
                            createSuccess = "Cuenta creada. Comparte la contraseña temporal por un canal privado.",
                        )
                    }
                    refreshUsers()
                }

                is CreateAccountResult.Failure -> _state.update {
                    it.copy(createLoading = false, createError = result.toMessage())
                }
            }
        }
    }

    /** Opens the detail dialog and looks up a still-valid linking code for the student. */
    fun selectStudent(studentId: String) {
        _state.update {
            it.copy(
                selectedStudentId = studentId,
                linkingCode = null,
                detailLoading = true,
                detailSaving = false,
                detailError = null,
            )
        }
        viewModelScope.launch {
            val code = runCatching { students.getLinkingCode(studentId) }.getOrNull()
            _state.update { current ->
                if (current.selectedStudentId == studentId) {
                    current.copy(linkingCode = code, detailLoading = false)
                } else {
                    current
                }
            }
        }
    }

    fun dismissStudent() {
        _state.update {
            it.copy(
                selectedStudentId = null,
                linkingCode = null,
                detailLoading = false,
                detailSaving = false,
                detailError = null,
            )
        }
    }

    /** Logical baja through the existing setActive path, then a fresh student read. */
    fun deactivateStudent() {
        setStudentActive(active = false)
    }

    /**
     * Undo of the logical baja: same path with `active = true`, so the group's
     * `studentCount` goes back up. Without it a directivo could deactivate a
     * student and never bring them back from the app.
     */
    fun reactivateStudent() {
        setStudentActive(active = true)
    }

    private fun setStudentActive(active: Boolean) {
        val studentId = _state.value.selectedStudentId ?: return
        _state.update { it.copy(detailSaving = true, detailError = null) }
        viewModelScope.launch {
            // Reuse the repository's batch: active flag + groups.studentCount ±1.
            when (val result = students.setActive(studentId, active)) {
                AdminStudentResult.Success -> {
                    dismissStudent()
                    refreshStudents()
                }

                is AdminStudentResult.Failure -> _state.update {
                    it.copy(detailSaving = false, detailError = result.toMessage())
                }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun createMessageShown() {
        _state.update { it.copy(createSuccess = null, createError = null) }
    }

    fun createErrorShown() {
        _state.update { it.copy(createError = null) }
    }
}

/** Server failures in Spanish; `orphaned` is never hidden behind a generic retry. */
private fun CreateAccountResult.Failure.toMessage(): String = when {
    orphaned -> "La cuenta se creó en Auth, pero no se pudo guardar el perfil ni revertir el alta. " +
        "Contacta al administrador del sistema."
    else -> when (reason) {
        AccountCreationError.NO_SESSION -> "No hay una sesión activa."
        AccountCreationError.DENIED -> "No tienes permiso para crear cuentas."
        AccountCreationError.INVALID_EMAIL -> "Ingresa un correo electrónico válido."
        AccountCreationError.EMAIL_IN_USE -> "Ya existe una cuenta con ese correo."
        AccountCreationError.WEAK_PASSWORD -> "La contraseña debe tener al menos 6 caracteres."
        AccountCreationError.NETWORK -> "No se pudo conectar. Revisa tu conexión e inténtalo de nuevo."
        AccountCreationError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
        AccountCreationError.UNKNOWN -> "No se pudo crear la cuenta. Inténtalo de nuevo."
    }
}
