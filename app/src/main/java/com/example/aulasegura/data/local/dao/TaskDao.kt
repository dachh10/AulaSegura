package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read-only cache of `tasks/{taskId}`.
 *
 * [TaskEntity.groupIds] is a converted list and cannot be filtered in SQL; a
 * student's homework list is `observeBySchool` narrowed in memory to the groups
 * that student belongs to.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE schoolId = :schoolId AND active = 1 ORDER BY dueDate")
    fun observeActiveBySchool(schoolId: String): Flow<List<TaskEntity>>

    /**
     * What the teacher published on this device, newest first.
     *
     * No index on `createdBy`: the cache only ever holds this teacher's rows in
     * Fase 1, so the table is small enough that an index would cost a migration
     * and buy nothing.
     */
    @Query("SELECT * FROM tasks WHERE createdBy = :createdBy ORDER BY createdAt DESC")
    fun observeByCreator(createdBy: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE taskId = :taskId LIMIT 1")
    suspend fun findById(taskId: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    suspend fun deleteById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}
