package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads are `Flow` so a screen redraws when the sync worker rewrites the cache;
 * writes are `@Upsert` because every cache write is "make Room match Firestore".
 *
 * `teacherIds` is a converted list and cannot be used in `WHERE`; filter it in
 * memory after one of the indexed reads below.
 */
@Dao
interface GroupDao {

    @Query("SELECT * FROM groups ORDER BY grade, section")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE schoolId = :schoolId ORDER BY grade, section")
    fun observeBySchool(schoolId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun findById(groupId: String): GroupEntity?

    @Upsert
    suspend fun upsert(group: GroupEntity)

    @Upsert
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM groups WHERE groupId = :groupId")
    suspend fun deleteById(groupId: String)

    @Query("DELETE FROM groups")
    suspend fun clear()
}
