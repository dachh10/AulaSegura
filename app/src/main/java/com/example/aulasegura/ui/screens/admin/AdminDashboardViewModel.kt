package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.example.aulasegura.domain.repository.pendingTeacherCount
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AdminDashboardRepository
import com.example.aulasegura.domain.repository.AdminDashboardResult
import com.example.aulasegura.domain.repository.AdminReportRepository
import com.example.aulasegura.domain.repository.AdminReportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the school's dashboard metrics and today's attendance once, and reloads
 * both on explicit refresh.
 *
 * No Firebase types here: the repositories return domain models and typed results,
 * so this class is testable without an emulator or a Firestore instance. The two
 * reads are independent: a failure of the day card must not hide the campus numbers.
 */
class AdminDashboardViewModel(
    private val dashboard: AdminDashboardRepository,
    private val reports: AdminReportRepository,
    private val users: com.example.aulasegura.domain.repository.AdminUserRepository? = null,
    private val schools: com.example.aulasegura.domain.repository.AdminSchoolRepository? = null,
    private val auth: com.example.aulasegura.domain.repository.AuthRepository? = null,
    private val students: com.example.aulasegura.domain.repository.AdminStudentRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardUiState())
    val state: StateFlow<AdminDashboardUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        val current = auth?.currentUser
        _state.update {
            it.copy(
                photoUrl = current?.photoUrl,
                fullName = current?.fullName.orEmpty(),
            )
        }
        refreshIdentity()
        refreshPendingTeachers()
        refreshUngroupedStudents()
        refreshSchoolName()
        _state.update {
            it.copy(
                loading = true,
                error = null,
                todayLoading = true,
                todayError = null,
            )
        }
        viewModelScope.launch {
            when (val result = dashboard.getMetrics()) {
                is AdminDashboardResult.Success -> _state.update {
                    it.copy(loading = false, dashboard = result.dashboard, error = null)
                }

                is AdminDashboardResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.reason.toMessage())
                }
            }
        }
        viewModelScope.launch {
            when (val result = reports.getTodaySummary()) {
                is AdminReportResult.Success -> _state.update {
                    it.copy(todayLoading = false, todaySummary = result.data, todayError = null)
                }

                is AdminReportResult.Failure -> _state.update {
                    it.copy(todayLoading = false, todayError = result.reason.toMessage())
                }
            }
        }
    }

    /**
     * Re-reads the directora's own document for the top bar.
     *
     * `auth.currentUser` is the profile resolved at sign-in, so a photo or a name
     * saved later in the profile screen stays invisible here until the next
     * sign-in. The dashboard re-runs this whenever the screen comes back, not on
     * every refresh, and only touches the greeting fields — the metrics keep their
     * own loading state.
     */
    fun refreshIdentity() {
        val repo = users ?: return
        viewModelScope.launch {
            val self = runCatching { repo.getSelf() }.getOrNull() ?: return@launch
            _state.update {
                it.copy(
                    photoUrl = self.photoUrl ?: it.photoUrl,
                    fullName = self.fullName.ifBlank { it.fullName },
                )
            }
        }
    }

    private fun refreshPendingTeachers() {
        val repo = users ?: return
        viewModelScope.launch {
            val count = runCatching { repo.getUsers() }
                .getOrDefault(emptyList())
                .let { pendingTeacherCount(it) }
            _state.update { it.copy(pendingTeachers = count) }
        }
    }

    private fun refreshSchoolName() {
        val repo = schools ?: return
        viewModelScope.launch {
            val name = runCatching { repo.getSchool() }
                .getOrNull()
                ?.let { it as? com.example.aulasegura.domain.repository.AdminSchoolResult.Success }
                ?.data?.name.orEmpty()
            _state.update { it.copy(schoolName = name) }
        }
    }

    private fun refreshUngroupedStudents() {
        val repo = students ?: return
        viewModelScope.launch {
            val count = runCatching { repo.getStudents() }
                .getOrDefault(emptyList())
                .let { ungroupedStudentCount(it) }
            _state.update { it.copy(ungroupedStudents = count) }
        }
    }
}
