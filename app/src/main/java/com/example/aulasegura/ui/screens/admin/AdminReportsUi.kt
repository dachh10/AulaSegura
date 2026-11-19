package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroupBreakdown
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminTopAbsenceDay
import com.example.aulasegura.domain.model.attendancePercent
import com.example.aulasegura.domain.repository.AdminReportError
import com.example.aulasegura.util.AttendanceTimes
import java.util.Calendar
import java.util.Locale

/**
 * What the Reportes screen renders, plus pure helpers for all of its formatting.
 *
 * No Firebase, no Compose, no ViewModel here. Every number the screen prints comes
 * from these functions applied to the repository's domain models.
 */
data class AdminReportsUiState(
    val loading: Boolean = true,
    val fromDate: String = "",
    val toDate: String = "",
    val summary: AdminReportRangeSummary? = null,
    val groups: List<AdminGroupBreakdown> = emptyList(),
    val topDays: List<AdminTopAbsenceDay> = emptyList(),
    val error: String? = null,
) {
    /** True only after a completed load that found no sessions in the range. */
    val empty: Boolean get() = !loading && error == null && summary?.empty == true

    val hasData: Boolean
        get() = !loading && error == null && summary != null && summary.sessionCount > 0
}

private val MONTH_LABELS = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic",
)

/**
 * `"2026-11-17"` to `"17 nov 2026"` without parsing through a device timezone.
 * An unreadable value is returned unchanged, never replaced by a guessed date.
 */
fun formatIsoDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val day = parts[2].toIntOrNull() ?: return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val label = MONTH_LABELS.getOrNull(month - 1) ?: return iso
    return "$day $label ${parts[0]}"
}

/** `"1 nov 2026 — 17 nov 2026"`, or a single date when both ends are the same. */
fun rangeLabel(fromDate: String, toDate: String): String =
    if (fromDate == toDate) formatIsoDate(toDate)
    else "${formatIsoDate(fromDate)} — ${formatIsoDate(toDate)}"

/** Percentage formatting with the zero-marks guard, so no range shows a NaN. */
fun formatAttendancePercent(present: Int, total: Int): String =
    "${attendancePercent(present, total)}%"

/** The first of the month through today, in the ISO format Firestore stores. */
fun monthRange(todayIso: String): Pair<String, String> {
    if (todayIso.length < 7) return todayIso to todayIso
    return "${todayIso.take(7)}-01" to todayIso
}

/** Today and the [days] previous calendar days, using the device's calendar. */
fun lastDaysRange(todayIso: String, days: Int): Pair<String, String> {
    val todayMillis = AttendanceTimes.isoDateToMillis(todayIso) ?: return todayIso to todayIso
    val calendar = Calendar.getInstance(Locale.getDefault()).apply {
        timeInMillis = todayMillis
        add(Calendar.DAY_OF_MONTH, -(days - 1).coerceAtLeast(0))
    }
    return AttendanceTimes.isoDate(calendar.timeInMillis) to todayIso
}

/** Most absences first, then the earlier day, so the ranking is deterministic. */
fun sortedTopDays(days: List<AdminTopAbsenceDay>): List<AdminTopAbsenceDay> =
    days.sortedWith(
        compareByDescending<AdminTopAbsenceDay> { it.absentCount }
            .thenBy { it.date },
    )

/** Most absences first; ties fall back to the best attendance, then the label. */
fun sortedGroups(groups: List<AdminGroupBreakdown>): List<AdminGroupBreakdown> =
    groups.sortedWith(
        compareByDescending<AdminGroupBreakdown> { it.absentCount }
            .thenByDescending { it.attendancePercentage }
            .thenBy { it.groupLabel },
    )

/**
 * The four bar values of the status chart, derived from the range summary.
 * Callers render them only when [AdminReportsUiState.hasData] is true.
 */
data class ReportChartValues(
    val present: Int,
    val absent: Int,
    val late: Int,
    val justified: Int,
) {
    val total: Int get() = present + absent + late + justified
}

fun chartValues(summary: AdminReportRangeSummary): ReportChartValues =
    ReportChartValues(
        present = summary.presentCount,
        absent = summary.absentCount,
        late = summary.lateCount,
        justified = summary.justifiedCount,
    )

/** User-facing copy for a typed repository failure. */
fun AdminReportError.toMessage(): String = when (this) {
    AdminReportError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminReportError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminReportError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminReportError.DENIED -> "No tienes permiso para ver los datos del plantel."
    AdminReportError.UNKNOWN -> "No se pudieron cargar los reportes de asistencia."
}
