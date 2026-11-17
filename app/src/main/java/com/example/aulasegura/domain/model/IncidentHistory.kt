package com.example.aulasegura.domain.model

/**
 * One incident the teacher reported, as the history list needs it.
 *
 * The list is the teacher's own history, so it is read under the rule that
 * already allows it (`reportedBy == uid`) and not through the admin inbox, which
 * belongs to another phase.
 *
 * [severity] and [category] are nullable because a document written by an older
 * build — or edited by hand — may carry a wire value this enum no longer knows.
 * A chip that disappears is better than a list that crashes on one bad row.
 */
data class IncidentSummary(
    val incidentId: String,
    /**
     * Group the incident belongs to, so the group detail can filter its own
     * incidents out of the teacher's history. Empty only for a summary built
     * without a cached draft, which the write path never produces.
     */
    val groupId: String = "",
    val studentName: String,
    val title: String,
    val description: String,
    val severity: IncidentSeverity?,
    val category: IncidentCategory?,
    val status: IncidentStatus,
    val occurredAt: Long,
    val locationVerified: Boolean?,
    val locationDistanceMeters: Double?,
    val sync: SyncPhase,
    val mediaCount: Int,
)

/**
 * The incident's own lifecycle, which is the admin's to change and the teacher's
 * to read.
 *
 * `nuevo` is the only value the rules accept on create; the other two arrive from
 * the admin screens in a later phase, so the teacher's history has to render
 * them even though this block never writes them.
 */
enum class IncidentStatus(val wire: String, val label: String) {
    NUEVO("nuevo", "Nuevo"),
    REVISADO("revisado", "Revisado"),
    CERRADO("cerrado", "Cerrado"),
    ;

    companion object {
        /** Unknown or missing wire values read as `nuevo`, never as a crash. */
        fun fromWire(wire: String?): IncidentStatus =
            entries.firstOrNull { it.wire == wire } ?: NUEVO
    }
}

/**
 * One evidence file of an incident, as the detail shows it.
 *
 * [url] is the Cloudinary delivery URL, which is the only way to see the photo
 * again: the local copy was deleted once the upload finished. [localPath] stays
 * only for a file that is still on this phone.
 */
data class IncidentEvidence(
    val mediaId: String,
    val url: String?,
    val thumbnailUrl: String?,
    val format: String?,
    val bytes: Long,
    val phase: MediaUploadPhase,
    val localPath: String?,
)
