package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.util.AttendanceTimes
import com.example.aulasegura.util.SchoolCalendar
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The tutor's attendance calendar.
 *
 * The month is the unit of work because it is the unit of the screen: changing
 * month reloads one month, and the chevron cannot move past the current month
 * because there is nothing there to show.
 */
class ParentAttendanceViewModel(
    private val parents: ParentRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentAttendanceUiState>(ParentAttendanceUiState.Loading)
    val state: StateFlow<ParentAttendanceUiState> = _state.asStateFlow()

    private var children: List<ParentChild> = emptyList()
    private var selected = 0
    private var year = 0
    private var month = 0

    init {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now() }
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH) + 1
        loadChildren()
    }

    fun refresh() {
        if (children.isEmpty()) loadChildren() else reload()
    }

    fun previousMonth() = moveMonth(-1)

    fun nextMonth() {
        if (isCurrentMonth()) return
        moveMonth(1)
    }

    /**
     * The attendance screen has one child pill and no selector, exactly like the
     * design. A tutor with more than one child taps the pill to change it.
     */
    fun cycleChild() {
        if (children.size <= 1) return
        selected = (selected + 1) % children.size
        reload()
    }

    private fun loadChildren() {
        viewModelScope.launch {
            _state.value = ParentAttendanceUiState.Loading
            when (val result = parents.loadChildren()) {
                is ParentChildrenResult.Loaded -> {
                    children = result.children
                    if (children.isEmpty()) {
                        _state.value = ParentAttendanceUiState.NoChildren
                    } else {
                        if (selected !in children.indices) selected = 0
                        loadMonth()
                    }
                }
                ParentChildrenResult.SignedOut ->
                    _state.value = ParentAttendanceUiState.Failed("Vuelve a iniciar sesión.")
                is ParentChildrenResult.Failed ->
                    _state.value = ParentAttendanceUiState.Failed(
                        attendanceFailureMessage(result.reason),
                    )
            }
        }
    }

    private fun reload() {
        if (children.isEmpty()) {
            loadChildren()
            return
        }
        viewModelScope.launch {
            _state.value = ParentAttendanceUiState.Loading
            loadMonth()
        }
    }

    private suspend fun loadMonth() {
        val child = children[selected]
        _state.value = when (val result = parents.loadMonth(child, year, month)) {
            is ParentMonthResult.Loaded -> loaded(child, result.month)
            ParentMonthResult.SignedOut -> ParentAttendanceUiState.Failed("Vuelve a iniciar sesión.")
            is ParentMonthResult.Failed -> ParentAttendanceUiState.Failed(
                attendanceFailureMessage(result.reason),
            )
        }
    }

    private fun moveMonth(delta: Int) {
        val calendar = Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, 1)
            add(Calendar.MONTH, delta)
        }
        year = calendar.get(Calendar.YEAR)
        month = calendar.get(Calendar.MONTH) + 1
        reload()
    }

    private fun loaded(
        child: ParentChild,
        monthData: ParentMonthAttendance,
    ): ParentAttendanceUiState.Loaded {
        val moment = now()
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = moment }

        return ParentAttendanceUiState.Loaded(
            childName = child.firstName.ifBlank { child.fullName },
            groupLabel = child.groupLabel,
            childCount = children.size,
            selected = selected,
            monthLabel = AttendanceTimes.monthLabel(monthData.year, monthData.month, zone),
            percent = monthData.counts.percent,
            counts = monthData.counts,
            classDays = SchoolCalendar.classDaysInMonth(monthData.year, monthData.month, zone),
            weeks = monthWeeks(
                year = monthData.year,
                month = monthData.month,
                marks = marksByDay(monthData.days),
                todayYear = calendar.get(Calendar.YEAR),
                todayMonth = calendar.get(Calendar.MONTH) + 1,
                todayDay = calendar.get(Calendar.DAY_OF_MONTH),
                zone = zone,
            ),
            recent = monthData.days.take(RECENT_LIMIT).map { it.toRecentUi(zone) },
            canGoNext = !isCurrentMonth(),
        )
    }

    private fun isCurrentMonth(): Boolean {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now() }
        return year == calendar.get(Calendar.YEAR) && month == calendar.get(Calendar.MONTH) + 1
    }

    private companion object {
        /** Enough to fill the card without turning the screen into a list. */
        const val RECENT_LIMIT = 4
    }
}
