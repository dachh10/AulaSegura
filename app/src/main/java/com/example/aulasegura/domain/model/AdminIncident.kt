package com.example.aulasegura.domain.model

/**
 * Read-side model of one `incidents/{id}` document for the principal's inbox.
 *
 * Nothing here knows Firestore: the strings on the wire are converted once, at the
 * repository boundary, and the rest of the app sees typed enums. Unknown severity
 * or status values fall back to safe defaults instead of crashing a list that is
 * evidence: an unreadable chip is better than an inaccessible incident.
 */

/** One evidence entry already uploaded to Cloudinary, as the inbox detail shows it. */
data class AdminIncidentMedia(
    val id: String = "",
    val type: String = "image",
    val publicId: String? = null,
    val url: String = "",
    val thumbnailUrl: String? = null,
    val format: String? = null,
)

/** The school's pin, kept only to render the detail row and the verified badge. */
data class AdminIncidentLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Double? = null,
    val distanceMeters: Double? = null,
    val verified: Boolean = false,
)

data class AdminIncident(
    val id: String = "",
    val schoolId: String = "",
    val groupId: String = "",
    val studentId: String? = null,
    val studentName: String = "",
    val title: String = "",
    val description: String = "",
    val severity: IncidentSeverity = IncidentSeverity.LEVE,
    val category: IncidentCategory = IncidentCategory.OTRO,
    val reportedBy: String = "",
    val reportedByName: String = "",
    val occurredAt: Long = 0L,
    val location: AdminIncidentLocation? = null,
    val media: List<AdminIncidentMedia> = emptyList(),
    val status: IncidentStatus = IncidentStatus.NUEVO,
    val seenByTutorIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
) {
    /** Numeric rank that gives the three known severities a total order. */
    val severityRank: Int
        get() = when (severity) {
            IncidentSeverity.LEVE -> 1
            IncidentSeverity.MODERADO -> 2
            IncidentSeverity.GRAVE -> 3
        }

    /** Spanish severity label, printed on the chip and reusable in a sorted label. */
    val severityLabel: String get() = severity.label

    val statusLabel: String get() = status.label

    /** "Grave · Daño al mobiliario": stable enough to double as a display label. */
    val displayLabel: String get() = "$severityLabel · $title"

    /** Only uploaded images count as photos; videos are a separate evidence kind. */
    val photoCount: Int
        get() = media.count { it.type.equals(IMAGE_TYPE, ignoreCase = true) }

    val hasMedia: Boolean get() = media.isNotEmpty()

    companion object {
        private const val IMAGE_TYPE = "image"

        /**
         * Unknown or missing wire values read as the least alarming severity and
         * the initial status. A bad value must never hide evidence from the inbox.
         */
        fun severityFromWire(wire: String?): IncidentSeverity =
            IncidentSeverity.entries.firstOrNull { it.wire == wire } ?: IncidentSeverity.LEVE

        fun statusFromWire(wire: String?): IncidentStatus = IncidentStatus.fromWire(wire)

        fun categoryFromWire(wire: String?): IncidentCategory =
            IncidentCategory.entries.firstOrNull { it.wire == wire } ?: IncidentCategory.OTRO
    }
}
