package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of `students/{studentId}`.
 *
 * [tutorIds] goes through a `TypeConverter` and therefore cannot be filtered in
 * SQL. A tutor's roster is read by [groupId]/[schoolId] and narrowed in memory.
 */
@Entity(
    tableName = "students",
    indices = [Index("groupId"), Index("schoolId")],
)
data class StudentEntity(
    @PrimaryKey val studentId: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val curp: String? = null,
    val listNumber: Int,
    val groupId: String,
    val schoolId: String,
    val tutorIds: List<String> = emptyList(),
    val photoUrl: String? = null,
    val photoPublicId: String? = null,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    /** Linking-flow fields cached alongside the roster fields. */
    val grado: String = "",
    val grupo: String = "",
    val creadoPor: String = "",
    /** `PENDIENTE` in the requirement doc maps to [SyncState.PENDING]. */
    val syncStatus: SyncState = SyncState.SYNCED,
)
