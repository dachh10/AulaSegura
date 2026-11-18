package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AdminReportRepository
import com.example.aulasegura.domain.repository.AdminReportResult
import com.example.aulasegura.util.AttendanceTimes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the report once and reloads it whenever the date range changes.
 *
 * No Firebase types here: the repository returns domain models and typed results,
 * so the screen's state machine can be driven by a fake in a unit test.
 */
class AdminReportsViewModel(
    private val reports: AdminReportRepository,
    private val todayIso: () -> String = { AttendanceTimes.isoDate(System.currentTimeMillis()) },
) : ViewModel() {

    private val initialRange = monthRange(todayIso())

    private val _state = MutableStateFlow(
        AdminReportsUiState(fromDate = initialRange.first, toDate = initialRange.second),
    )
    val state: StateFlow<AdminReportsUiState> = _state

    /** Stale responses from an earlier range must never overwrite the new one. */
    private var requestId = 0

    init {
        refresh()
    }

    fun setRange(fromDate: String, toDate: String) {
        val current = _state.value
        if (fromDate == current.fromDate && toDate == current.toDate) return
        _state.update { it.copy(fromDate = fromDate, toDate = toDate) }
        refresh()
    }

    fun refresh() {
        val id = ++requestId
        val from = _state.value.fromDate
        val to = _state.value.toDate
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            val summary = when (val result = reports.getRangeSummary(from, to)) {
                is AdminReportResult.Success -> result.data
                is AdminReportResult.Failure -> return@launch fail(id, result.reason.toMessage())
            }
            val groups = when (val result = reports.getGroupBreakdown(from, to)) {
                is AdminReportResult.Success -> result.data
                is AdminReportResult.Failure -> return@launch fail(id, result.reason.toMessage())
            }
            val days = when (val result = reports.getTopAbsenceDays(from, to)) {
                is AdminReportResult.Success -> result.data
                is AdminReportResult.Failure -> return@launch fail(id, result.reason.toMessage())
            }
            if (id != requestId) return@launch
            _state.update {
                it.copy(
                    loading = false,
                    summary = summary,
                    groups = groups,
                    topDays = days,
                    error = null,
                )
            }
        }
    }

    private fun fail(id: Int, message: String) {
        if (id != requestId) return
        _state.update { it.copy(loading = false, error = message) }
    }
}
