package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Incident created on the device, possibly while offline.
 *
 * `incidents/{incidentId}` in Firestore uses this same client-generated UUID as
 * the document id, so replaying the outbox entry is idempotent. Evidence files
 * live in [IncidentMediaEntity] because SQLite cannot hold an array of maps.
 */
@Entity(
    tableName = "incident_drafts",
    indices = [Index("groupId"), Index("schoolId"), Index("syncState")],
)
data class IncidentDraftEntity(
    @PrimaryKey val incidentId: String,
    val schoolId: String,
    val groupId: String,
    val studentId: String? = null,
    val studentName: String,
    val title: String,
    val description: String,
    /** `"leve"`, `"moderado"` or `"grave"`. */
    val severity: String,
    /** `"discusion"`, `"caida"`, `"danio"`, `"desacato"` or `"otro"`. */
    val category: String,
    val reportedBy: String,
    val reportedByName: String,
    val occurredAt: Long,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAccuracy: Double? = null,
    val locationDistanceMeters: Double? = null,
    val locationVerified: Boolean? = null,
    /** `"nuevo"`, `"revisado"` or `"cerrado"`. */
    val status: String = "nuevo",
    val seenByTutorIds: List<String> = emptyList(),
    val syncState: SyncState = SyncState.PENDING,
    val createdAt: Long,
    val updatedAt: Long,
)
