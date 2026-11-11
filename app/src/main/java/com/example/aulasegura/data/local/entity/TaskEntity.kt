package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of `tasks/{taskId}`.
 *
 * [groupIds] goes through a `TypeConverter` and cannot be filtered in SQL, so
 * "homework for my groups" is resolved by reading the school's tasks and
 * matching in memory. [attachmentsJson] stores the `attachments` array of maps
 * as JSON for the same reason as `NoticeEntity`.
 */
@Entity(
    tableName = "tasks",
    indices = [Index("schoolId"), Index("dueDate"), Index("active")],
)
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val schoolId: String,
    val groupIds: List<String> = emptyList(),
    val subject: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val attachmentsJson: String = "[]",
    val createdBy: String,
    val createdByName: String,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
