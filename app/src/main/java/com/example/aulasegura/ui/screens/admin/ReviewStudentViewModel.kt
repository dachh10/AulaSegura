package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.local.entity.StudentEntity
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.ApproveStudentResult
import com.example.aulasegura.domain.repository.ApprovedStudentInput
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.sync.PendingStudentApproval
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewStudentUiState(
    val alumno: String = "",
    val tutorUid: String = "",
    val form: ReviewStudentForm = ReviewStudentForm(),
    val existing: List<ExistingStudent> = emptyList(),
    val loading: Boolean = false,
    val done: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val duplicate: ExistingStudent?
        get() = existing.firstOrNull {
            it.name.trim().equals(alumno.trim(), ignoreCase = true)
        }
}

/**
 * Drives the approval of a scanned QR. No Firebase types: the repositories
 * return domain results, so the decision is testable without a device.
 */
class ReviewStudentViewModel(
    private val students: AdminStudentRepository,
    private val auth: AuthRepository,
    private val currentUser: () -> com.example.aulasegura.domain.model.AppUser? = { auth.currentUser },
    private val tutorUid: String,
    private val alumno: String,
    private val isOnline: () -> Boolean = { true },
    private val enqueueApproval: suspend (StudentEntity, PendingStudentApproval) -> Unit = { _, _ -> },
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ReviewStudentUiState(alumno = alumno, tutorUid = tutorUid),
    )
    val state: StateFlow<ReviewStudentUiState> = _state

    init {
        viewModelScope.launch {
            val all = runCatching { students.getStudents() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    existing = all.map { student ->
                        ExistingStudent(
                            name = student.displayName,
                            studentId = student.id,
                            groupId = student.groupId,
                        )
                    },
                )
            }
        }
    }

    fun onFormChange(form: ReviewStudentForm) = _state.update { it.copy(form = form, error = null) }

    fun approve() {
        val current = _state.value
        if (current.loading || current.done) return
        val plan = approvalPlan(
            tutorUid = tutorUid,
            alumno = alumno,
            form = current.form,
            schoolId = currentUser()?.schoolId.orEmpty(),
            createdBy = currentUser()?.uid.orEmpty(),
            existing = current.existing,
        )
        if (plan.error != null) {
            _state.update { it.copy(error = plan.error) }
            return
        }
        val student = plan.student ?: return
        val input = ApprovedStudentInput(
            alumno = student.alumno,
            tutorUid = student.tutorUid,
            schoolId = student.schoolId,
            createdBy = student.createdBy,
            grado = student.grado,
            grupo = student.grupo,
            groupId = student.groupId,
            duplicateStudentId = plan.duplicateStudentId
                .takeIf { current.form.linkAsSecondTutor },
        )
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (!isOnline()) {
                queueOffline(input)
                return@launch
            }
            approveOnline(input)
        }
    }

    private suspend fun approveOnline(input: ApprovedStudentInput) {
        val result = students.approveStudent(input)
        _state.update {
            when (result) {
                is ApproveStudentResult.Success -> it.copy(
                    loading = false,
                    done = true,
                    message = "Alumno aprobado y tutor vinculado.",
                )
                is ApproveStudentResult.Failure -> it.copy(
                    loading = false,
                    error = "No se pudo aprobar al alumno. Inténtalo de nuevo.",
                )
            }
        }
    }

    /**
     * With no signal the student stays local as PENDING: the tutor will not see
     * them linked until WorkManager replays the two-document batch. The message
     * says so explicitly instead of claiming success.
     */
    private suspend fun queueOffline(input: ApprovedStudentInput) {
        val studentId = "stu-qr-${UUID.randomUUID()}"
        val entity = StudentEntity.fromApproval(
            studentId = studentId,
            alumno = input.alumno,
            tutorUid = input.tutorUid,
            groupId = input.groupId,
            schoolId = input.schoolId,
            grado = input.grado,
            grupo = input.grupo,
            creadoPor = input.createdBy,
            now = now(),
        )
        val approval = PendingStudentApproval.from(input, studentId)
        runCatching { enqueueApproval(entity, approval) }.fold(
            onSuccess = {
                _state.update {
                    it.copy(
                        loading = false,
                        done = true,
                        message = "Guardado en el dispositivo. Se vinculará al volver la señal.",
                    )
                }
            },
            onFailure = {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "No se pudo guardar sin conexión. Inténtalo de nuevo.",
                    )
                }
            },
        )
    }

    fun reject() {
        if (_state.value.loading || _state.value.done) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val ok = auth.setTutorLinking(tutorUid, "rechazada").isSuccess
            _state.update {
                if (ok) {
                    it.copy(loading = false, done = true, message = "Solicitud rechazada.")
                } else {
                    it.copy(loading = false, error = "No se pudo rechazar. Inténtalo de nuevo.")
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null, error = null) }
}
