package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentIncidentLocation
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.Locale
import java.util.TimeZone

/**
 * What the tutor's incident list renders for the child on screen.
 *
 * The list is per child because the design has one student pill, exactly like
 * attendance and tasks. The counters are the child's, not the family's.
 */
sealed interface ParentIncidentsUiState {

    data object Loading : ParentIncidentsUiState

    data class Loaded(
        val childName: String,
        val groupLabel: String,
        /** More than one child makes the pill tappable. */
        val childCount: Int,
        val total: Int,
        /** Reported and not yet acknowledged by this tutor. */
        val newCount: Int,
        val graveCount: Int,
        val incidents: List<ParentIncidentRowUi>,
    ) : ParentIncidentsUiState

    data object NoChildren : ParentIncidentsUiState

    data class Failed(val message: String) : ParentIncidentsUiState
}

data class ParentIncidentRowUi(
    val incidentId: String,
    val title: String,
    val dateLabel: String,
    val severity: ParentIncidentSeverity,
    val locationLabel: String,
    /** The tutor already pressed "Enterado" for this one. */
    val seen: Boolean,
    /** Drives the thumbnail icon when there is no evidence. */
    val category: String,
    val thumbnailUrl: String?,
    val isVideo: Boolean,
    val videoDuration: String?,
)

/** `leve`, `moderado` or `grave`; anything else paints as a neutral incident. */
enum class ParentIncidentSeverity(val label: String) {
    Leve("Leve"),
    Moderado("Moderado"),
    Grave("Grave"),
    Other("Incidente"),
}

internal fun incidentSeverityOf(wire: String): ParentIncidentSeverity = when (wire.lowercase()) {
    "leve" -> ParentIncidentSeverity.Leve
    "moderado" -> ParentIncidentSeverity.Moderado
    "grave" -> ParentIncidentSeverity.Grave
    else -> ParentIncidentSeverity.Other
}

/** `"Jue 30 oct · 11:20"`: the day the school reported it, with the clock. */
internal fun incidentDateLabel(
    millis: Long,
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (millis <= 0) return "Sin fecha"
    return "${AttendanceTimes.dayLabel(millis, zone)} · ${AttendanceTimes.clockTime(millis, zone)}"
}

/** Long form for the detail and the map: "Martes 21 de octubre · 13:05". */
internal fun incidentLongDateLabel(
    millis: Long,
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (millis <= 0) return "Sin fecha registrada"
    val day = AttendanceTimes.longDateLabel(AttendanceTimes.isoDate(millis, zone), zone)
    return "$day · ${AttendanceTimes.clockTime(millis, zone)}"
}

/**
 * The place, in the only terms the document supports.
 *
 * There is no place name in `incidents`, so the label never says "Patio Central":
 * it says whether the coordinate was verified inside the geofence, or that there
 * is no coordinate at all.
 */
internal fun incidentLocationLabel(location: ParentIncidentLocation?): String = when {
    location == null -> "Sin ubicación"
    location.verified -> "Ubicación verificada"
    else -> "Sin verificar"
}

internal fun incidentCoordinatesLabel(location: ParentIncidentLocation?): String =
    location?.let { String.format(Locale.US, "%.6f, %.6f", it.latitude, it.longitude) }
        ?: "Sin ubicación"

/**
 * Precision and distance, when the document carries them.
 *
 * Both are measurements the teacher's phone made; neither is ever estimated from
 * the map drawing.
 */
internal fun incidentLocationDetail(location: ParentIncidentLocation?): String? {
    if (location == null) return null
    val parts = buildList {
        location.accuracyMeters?.let { add("± ${it.toInt()} m") }
        location.distanceMeters?.let { add("a ${it.toInt()} m del plantel") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** `"0:24"`; `null` when the document has no duration to print. */
internal fun mediaDurationLabel(seconds: Double?): String? {
    if (seconds == null || seconds <= 0) return null
    val total = seconds.toInt()
    return "${total / 60}:${String.format(Locale.US, "%02d", total % 60)}"
}

internal fun ParentIncident.toRowUi(zone: TimeZone = TimeZone.getDefault()): ParentIncidentRowUi {
    val evidence = media.firstOrNull()

    return ParentIncidentRowUi(
        incidentId = incidentId,
        title = title,
        dateLabel = incidentDateLabel(occurredAtMillis, zone),
        severity = incidentSeverityOf(severity),
        locationLabel = incidentLocationLabel(location),
        seen = seenByMe,
        category = category,
        thumbnailUrl = evidence?.thumbnailUrl ?: evidence?.takeIf { !it.isVideo }?.url,
        isVideo = evidence?.isVideo == true,
        videoDuration = mediaDurationLabel(evidence?.durationSec),
    )
}

/** The incidents of one child, newest first as the repository already returns them. */
internal fun incidentsOf(
    incidents: List<ParentIncident>,
    studentId: String,
): List<ParentIncident> = incidents.filter { it.studentId == studentId }

internal fun newIncidents(incidents: List<ParentIncident>): Int =
    incidents.count { !it.seenByMe }

internal fun graveIncidents(incidents: List<ParentIncident>): Int =
    incidents.count { incidentSeverityOf(it.severity) == ParentIncidentSeverity.Grave }

internal fun incidentsFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver los incidentes."
    RosterFailure.UNKNOWN -> "No se pudieron cargar los incidentes."
}

internal fun incidentDetailFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver este incidente."
    RosterFailure.UNKNOWN -> "No se pudo cargar el incidente."
}
