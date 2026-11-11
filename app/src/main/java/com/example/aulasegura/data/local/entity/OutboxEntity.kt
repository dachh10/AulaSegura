package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable queue of writes that still have to reach Firestore.
 *
 * Rows are processed oldest first, so [id] is an auto-generated monotonic key
 * rather than the entity id: the same entity can legitimately be queued several
 * times and each operation has to keep its own order.
 *
 * [opType] is one of `UPSERT_ATTENDANCE_SESSION`, `UPSERT_INCIDENT`,
 * `UPLOAD_INCIDENT_MEDIA` or `UPSERT_JUSTIFICATION` (`CONTEXT.md` §8.2).
 * [payloadJson] is the serialized operation, filled by the sync layer.
 */
@Entity(
    tableName = "outbox",
    indices = [Index("entityLocalId"), Index("createdAt")],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val opType: String,
    val entityLocalId: String,
    val payloadJson: String,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
)
