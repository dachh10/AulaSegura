package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of `groups/{groupId}`.
 *
 * Mirrors the Firestore document one to one: same id, same field names, and
 * timestamps stored as epoch milliseconds because SQLite has no timestamp type.
 * See `CONTEXT.md` §6.2 for the authoritative schema.
 *
 * [teacherIds] is stored through a `TypeConverter`, so **it cannot appear in a
 * `WHERE` clause**. Queries that need "groups of this teacher" filter in memory
 * after reading by [schoolId]; a junction table is deliberately out of scope
 * for Fase 0.
 */
@Entity(
    tableName = "groups",
    indices = [Index("schoolId"), Index("titularTeacherId")],
)
data class GroupEntity(
    @PrimaryKey val groupId: String,
    val name: String,
    val grade: Int,
    val section: String,
    val schoolId: String,
    val titularTeacherId: String? = null,
    val teacherIds: List<String> = emptyList(),
    val studentCount: Int = 0,
    val cycle: String,
    val createdAt: Long,
    val updatedAt: Long,
)
