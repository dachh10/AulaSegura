package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminIncident
import com.example.aulasegura.domain.model.AdminIncidentLocation
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.repository.AdminIncidentError
import java.util.Calendar
import java.util.Locale

/**
 * What the Incidents inbox renders, plus pure helpers for all of its filtering
 * and formatting. No Firebase, no Compose, no ViewModel here: the screen only
 * maps these values onto the existing Stitch layout.
 */
data class AdminIncidentsUiState(
    val loading: Boolean = true,
    val incidents: List<AdminIncident> = emptyList(),
    val severityFilter: IncidentSeverity? = null,
    val statusFilter: IncidentStatus? = null,
    val error: String? = null,
    val actionError: String? = null,
    val updating: Boolean = false,
) {
    /** True only after a completed load that found no incidents at all. */
    val empty: Boolean get() = !loading && error == null && incidents.isEmpty()

    /** Filters and a deterministic sort applied to the loaded list. */
    val visibleIncidents: List<AdminIncident>
        get() = sortedIncidents(
            filterByStatus(
                filterBySeverity(incidents, severityFilter),
                statusFilter,
            ),
        )
}

/** Null means "Todos": no severity clause is applied. */
fun filterBySeverity(
    incidents: List<AdminIncident>,
    severity: IncidentSeverity?,
): List<AdminIncident> =
    if (severity == null) incidents else incidents.filter { it.severity == severity }

/** Null means "Todos": no status clause is applied. */
fun filterByStatus(
    incidents: List<AdminIncident>,
    status: IncidentStatus?,
): List<AdminIncident> =
    if (status == null) incidents else incidents.filter { it.status == status }

/**
 * Newest first, then id: the id tie-break makes the order total, so a refresh
 * never reshuffles two incidents created in the same millisecond.
 */
fun sortedIncidents(incidents: List<AdminIncident>): List<AdminIncident> =
    incidents.sortedWith(
        compareByDescending<AdminIncident> { it.createdAt }.thenBy { it.id },
    )

data class IncidentStats(
    val total: Int = 0,
    val news: Int = 0,
    val grave: Int = 0,
)

/** Counters come from the loaded list, never from a design-time constant. */
fun incidentStats(incidents: List<AdminIncident>): IncidentStats = IncidentStats(
    total = incidents.size,
    news = incidents.count { it.status == IncidentStatus.NUEVO },
    grave = incidents.count { it.severity == IncidentSeverity.GRAVE },
)

private val MONTH_LABELS = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic",
)

/**
 * `"21 oct 2026 · 13:05"` from epoch millis, in the device's time zone.
 * A missing date (0) becomes an em dash instead of printing 1 Jan 1970.
 */
fun formatIncidentDate(millis: Long): String {
    if (millis <= 0L) return "—"
    val calendar = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = millis }
    val month = MONTH_LABELS.getOrNull(calendar.get(Calendar.MONTH)) ?: return "—"
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "${calendar.get(Calendar.DAY_OF_MONTH)} $month ${calendar.get(Calendar.YEAR)} · $hour:$minute"
}

/** Short caption for the status action, e.g. `"Nuevo → Revisado"`. */
fun statusChangeLabel(from: IncidentStatus, to: IncidentStatus): String =
    "${from.label} → ${to.label}"

/** One line of the expandable detail. Media rows carry the URL to open in a browser. */
data class IncidentDetailRow(
    val label: String,
    val value: String,
    val mediaUrl: String? = null,
) {
    val isMedia: Boolean get() = mediaUrl != null
}

/** Coordinates and a verified/edited note, or an explicit absence message. */
fun formatIncidentLocation(location: AdminIncidentLocation?): String {
    if (location == null) return "Sin registrar"
    val lat = location.latitude ?: return "Sin coordenadas"
    val lng = location.longitude ?: return "Sin coordenadas"
    val coordinates = String.format(Locale.US, "%.5f, %.5f", lat, lng)
    val note = if (location.verified) "verificada" else "editada"
    return "$coordinates · $note"
}

/** Every field the detail shows, including one row per evidence URL. */
fun incidentDetailRows(incident: AdminIncident): List<IncidentDetailRow> {
    val rows = mutableListOf(
        IncidentDetailRow("Estudiante", incident.studentName.ifBlank { "—" }),
        IncidentDetailRow("Reportó", incident.reportedByName.ifBlank { "—" }),
        IncidentDetailRow("Descripción", incident.description.ifBlank { "—" }),
        IncidentDetailRow("Categoría", incident.category.label),
        IncidentDetailRow("Fecha del hecho", formatIncidentDate(incident.occurredAt)),
        IncidentDetailRow("Fecha de registro", formatIncidentDate(incident.createdAt)),
        IncidentDetailRow("Ubicación", formatIncidentLocation(incident.location)),
    )
    incident.media.forEachIndexed { index, media ->
        rows += IncidentDetailRow(
            label = "Evidencia ${index + 1} · ${if (media.type == "video") "video" else "foto"}",
            value = media.url.ifBlank { "Sin URL" },
            mediaUrl = media.url.takeIf { it.isNotBlank() },
        )
    }
    return rows
}

/** User-facing copy for a typed repository failure. */
fun AdminIncidentError.toMessage(): String = when (this) {
    AdminIncidentError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminIncidentError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminIncidentError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminIncidentError.DENIED -> "No tienes permiso para ver los incidentes del plantel."
    AdminIncidentError.UNKNOWN -> "No se pudieron cargar los incidentes."
}
