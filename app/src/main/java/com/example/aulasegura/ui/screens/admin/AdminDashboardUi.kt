package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminDashboard
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.repository.AdminDashboardError

/**
 * What the Panel screen renders.
 *
 * Everything here is pure: no Firebase, no ViewModel, no Compose. Formatting, the
 * summary line and the card mapping are plain functions so a unit test can prove the
 * dashboard shows the numbers it was given, never a design-time constant.
 */
data class AdminDashboardUiState(
    val pendingTeachers: Int = 0,
    val ungroupedStudents: Int = 0,
    val loading: Boolean = true,
    val dashboard: AdminDashboard = AdminDashboard(),
    val error: String? = null,
    val todaySummary: AdminReportRangeSummary? = null,
    val todayLoading: Boolean = true,
    val todayError: String? = null,
) {
    val empty: Boolean get() = !loading && error == null && dashboard.empty
}

/** Count of students with no group; the same rows the assignment screen lists. */
fun ungroupedStudentCount(students: List<AdminStudent>): Int =
    students.count { it.groupId.isBlank() }

/**
 * Copy for the "Asistencia de hoy" card. A day with no sessions says so explicitly;
 * a missing or failed read never becomes a 0% attendance figure.
 */
fun todayAttendanceMessage(
    summary: AdminReportRangeSummary?,
    loading: Boolean,
    error: String?,
): String = when {
    error != null -> error
    loading || summary == null -> "Cargando asistencia del día…"
    summary.sessionCount == 0 -> "Aún no hay sesiones registradas hoy."
    else -> {
        val sessions = if (summary.sessionCount == 1) "sesión" else "sesiones"
        "${summary.sessionCount} $sessions · ${summary.attendancePercentage}% de asistencia"
    }
}

/** The four tiles of "Plantel en números", with their stable identities. */
enum class DashboardCardKey(val label: String) {
    STUDENTS("Alumnos"),
    TEACHERS("Docentes"),
    GROUPS("Grupos"),
    INCIDENTS("Incidentes nuevos"),
}

data class DashboardCardUi(
    val key: DashboardCardKey,
    val value: String,
    val label: String,
)

/** Plain decimal rendering: no locale separators, so labels are deterministic. */
fun formatCount(count: Int): String = count.toString()

/** One-line summary of every real count, in the same order as the cards. */
fun summaryLabel(dashboard: AdminDashboard): String = listOf(
    "${formatCount(dashboard.studentCount)} alumnos",
    "${formatCount(dashboard.teacherCount)} docentes",
    "${formatCount(dashboard.groupCount)} grupos",
    "${formatCount(dashboard.incidentCount)} incidentes",
).joinToString(" · ")

/** Maps the loaded metrics to exactly the four cards the screen renders. */
fun dashboardCards(dashboard: AdminDashboard): List<DashboardCardUi> = listOf(
    DashboardCardKey.STUDENTS to dashboard.studentCount,
    DashboardCardKey.TEACHERS to dashboard.teacherCount,
    DashboardCardKey.GROUPS to dashboard.groupCount,
    DashboardCardKey.INCIDENTS to dashboard.incidentCount,
).map { (key, count) ->
    DashboardCardUi(key = key, value = formatCount(count), label = key.label)
}

/** Loading placeholders carry no number, so a refresh never flashes a fake count. */
fun loadingCards(): List<DashboardCardUi> = DashboardCardKey.entries.map { key ->
    DashboardCardUi(key = key, value = "…", label = key.label)
}

/** User-facing copy for a typed repository failure. */
fun AdminDashboardError.toMessage(): String = when (this) {
    AdminDashboardError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminDashboardError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminDashboardError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminDashboardError.DENIED -> "No tienes permiso para ver los datos del plantel."
    AdminDashboardError.UNKNOWN -> "No se pudieron cargar los datos del plantel."
}
