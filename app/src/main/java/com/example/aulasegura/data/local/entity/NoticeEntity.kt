package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of `notices/{noticeId}`.
 *
 * The Firestore `audience` map is flattened into three columns. [attachmentsJson]
 * holds the `attachments` array of maps as JSON: SQLite cannot store a list of
 * maps, and a junction table would be overkill for a read-only cache entry.
 */
@Entity(
    tableName = "notices",
    indices = [Index("schoolId"), Index("createdAt")],
)
data class NoticeEntity(
    @PrimaryKey val noticeId: String,
    val schoolId: String,
    val audienceRoles: List<String> = emptyList(),
    val audienceGroupIds: List<String> = emptyList(),
    val audienceStudentIds: List<String> = emptyList(),
    val title: String,
    val body: String,
    /** `"escuela"`, `"grupo"` or `"urgente"`. */
    val category: String,
    val urgent: Boolean = false,
    val attachmentsJson: String = "[]",
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
)
