package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.StudentEntity
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

    @Upsert
    suspend fun upsertAll(students: List<StudentEntity>)

    @Query("DELETE FROM students WHERE groupId = :groupId")
    suspend fun deleteByGroup(groupId: String)

    @Query("DELETE FROM students")
    suspend fun clear()
}
