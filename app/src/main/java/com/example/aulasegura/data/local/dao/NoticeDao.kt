package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.NoticeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read-only cache of `notices/{noticeId}`.
 *
 * The audience arrays are converted lists, so who sees a notice is decided in
 * memory against the signed-in user; `expiresAt` is filtered by the caller
 * because "still valid" depends on the device clock, not on the query.
 */
@Dao
interface NoticeDao {

    @Query("SELECT * FROM notices WHERE schoolId = :schoolId ORDER BY createdAt DESC")
    fun observeBySchool(schoolId: String): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices WHERE noticeId = :noticeId LIMIT 1")
    suspend fun findById(noticeId: String): NoticeEntity?

    @Upsert
    suspend fun upsert(notice: NoticeEntity)

    @Upsert
    suspend fun upsertAll(notices: List<NoticeEntity>)

    @Query("DELETE FROM notices WHERE noticeId = :noticeId")
    suspend fun deleteById(noticeId: String)

    @Query("DELETE FROM notices")
    suspend fun clear()
}
