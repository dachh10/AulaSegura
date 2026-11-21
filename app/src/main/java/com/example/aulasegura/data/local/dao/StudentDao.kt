package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.StudentEntity
import com.example.aulasegura.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Student cache reads. [StudentEntity.tutorIds] is a converted list, so a
 * tutor's roster is `observeBySchool` plus an in-memory filter, never a SQL
 * predicate.
 */
@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY listNumber")
    fun observeByGroup(groupId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE schoolId = :schoolId ORDER BY fullName")
    fun observeBySchool(schoolId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE studentId = :studentId LIMIT 1")
    suspend fun findById(studentId: String): StudentEntity?

    @Upsert
    suspend fun upsert(student: StudentEntity)

    /**
     * Bookkeeping for an offline approval: PENDING while queued, SYNCING while
     * the batch is in flight, then SYNCED/FAILED. A single-column update so a
     * refresh that happens mid-flight cannot overwrite the whole row.
     */
    @Query("UPDATE students SET syncStatus = :state WHERE studentId = :studentId")
    suspend fun updateSyncStatus(studentId: String, state: SyncState)

    @Upsert
    suspend fun upsertAll(students: List<StudentEntity>)

    @Query("DELETE FROM students WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    /**
     * Drops students that left the group.
     *
     * A refresh must not leave a student who was removed from the roster sitting
     * in the cache forever. Callers pass every id still present; an empty [keep]
     * is handled by [deleteByGroup] because `NOT IN ()` is invalid SQL.
     */
    @Query("DELETE FROM students WHERE groupId = :groupId AND studentId NOT IN (:keep)")
    suspend fun deleteMissing(groupId: String, keep: List<String>)

    @Query("DELETE FROM students")
    suspend fun clear()
}
