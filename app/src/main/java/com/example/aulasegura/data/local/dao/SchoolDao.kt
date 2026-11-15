package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.SchoolEntity
import kotlinx.coroutines.flow.Flow

/** Cache reads of the school document; writes are "make Room match Firestore". */
@Dao
interface SchoolDao {

    @Query("SELECT * FROM schools WHERE schoolId = :schoolId LIMIT 1")
    fun observeById(schoolId: String): Flow<SchoolEntity?>

    @Query("SELECT * FROM schools WHERE schoolId = :schoolId LIMIT 1")
    suspend fun findById(schoolId: String): SchoolEntity?

    @Upsert
    suspend fun upsert(school: SchoolEntity)
}
