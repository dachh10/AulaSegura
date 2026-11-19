package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentIncidentMedia
import java.util.TimeZone

/**
 * One incident, as the detail and the map screens render it.
 *
 * Both screens read the same state: the map is the same incident with the
 * coordinate drawn, not a second document. [seen] is cloud state — the tutor
 * cannot un-acknowledge — so it only flips after `markIncidentSeen` succeeded.
 */
sealed interface ParentIncidentDetailUiState {

    data object Loading : ParentIncidentDetailUiState

    data class Loaded(
        val incidentId: String,
        val title: String,
        val description: String,
        val severity: ParentIncidentSeverity,
        val dateLabel: String,
        val locationLabel: String,
        val coordinatesLabel: String,
        /** Precision and distance, when the document measured them. */
        val locationDetail: String?,
        val locationVerified: Boolean,
        val reporterName: String,
        val reporterInitials: String,
        val media: List<ParentIncidentMediaUi>,
        val seen: Boolean,
        /** The acknowledgement write is in flight. */
        val marking: Boolean,
        val error: String?,
        /** Real coordinate from `location`; `null` keeps the map off. */
        val latitude: Double?,
        val longitude: Double?,
    ) : ParentIncidentDetailUiState

    data object NotFound : ParentIncidentDetailUiState

    data class Failed(val message: String) : ParentIncidentDetailUiState
}

data class ParentIncidentMediaUi(
    val url: String,
    /** What the thumbnail should load; falls back to [url] for an image. */
    val previewUrl: String?,
    val isVideo: Boolean,
    val durationLabel: String?,
    val label: String,
)

internal fun ParentIncident.toDetailUi(
    marking: Boolean,
    error: String?,
    zone: TimeZone = TimeZone.getDefault(),
): ParentIncidentDetailUiState.Loaded = ParentIncidentDetailUiState.Loaded(
    incidentId = incidentId,
    title = title,
    description = description,
    severity = incidentSeverityOf(severity),
    dateLabel = incidentLongDateLabel(occurredAtMillis, zone),
    locationLabel = incidentLocationLabel(location),
    coordinatesLabel = incidentCoordinatesLabel(location),
    locationDetail = incidentLocationDetail(location),
    locationVerified = location?.verified == true,
    reporterName = reportedByName,
    reporterInitials = initialsOf(reportedByName),
    media = media.mapIndexed { index, item -> item.toUi(index) },
    seen = seenByMe,
    marking = marking,
    error = error,
    latitude = location?.latitude,
    longitude = location?.longitude,
)

internal fun ParentIncidentMedia.toUi(index: Int): ParentIncidentMediaUi = ParentIncidentMediaUi(
    url = url,
    previewUrl = thumbnailUrl ?: url.takeIf { !isVideo },
    isVideo = isVideo,
    durationLabel = mediaDurationLabel(durationSec),
    label = "Evidencia ${index + 1}",
)

/** `FOLIO #INC-…` from the document id: the only folio the app actually has. */
internal fun incidentFolio(incidentId: String): String =
    "#INC-" + incidentId.take(8).uppercase()

internal fun acknowledgeFailureMessage(): String =
    "No se pudo registrar que ya lo viste. Intenta de nuevo."
