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
) {
    companion object {
        /**
         * Local copy of a QR approval awaiting sync. The tutor's roster will not
         * show it as linked until the handler writes the two Firestore documents.
         */
        fun fromApproval(
            studentId: String,
            alumno: String,
            tutorUid: String,
            groupId: String,
            schoolId: String,
            grado: String,
            grupo: String,
            creadoPor: String,
            now: Long,
        ): StudentEntity {
            val parts = alumno.trim().split(Regex("\\s+"), limit = 2)
            return StudentEntity(
                studentId = studentId,
                firstName = parts.first(),
                lastName = parts.getOrNull(1).orEmpty(),
                fullName = alumno.trim(),
                listNumber = 0,
                groupId = groupId,
                schoolId = schoolId,
                tutorIds = listOf(tutorUid),
                active = true,
                createdAt = now,
                updatedAt = now,
                grado = grado,
                grupo = grupo,
                creadoPor = creadoPor,
                syncStatus = SyncState.PENDING,
            )
        }
    }
}
