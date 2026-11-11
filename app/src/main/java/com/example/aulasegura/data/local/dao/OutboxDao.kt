package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aulasegura.data.local.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Durable write queue. Oldest row first, one row per operation, so a replay
 * keeps the order the user produced.
 *
 * A row is only deleted after its operation reached Firestore (or, for media,
 * Cloudinary); on failure [markAttempt] records the error and the attempts
 * counter the worker uses for its backoff decision.
 */
@Dao
interface OutboxDao {

    @Query("SELECT * FROM outbox ORDER BY createdAt, id")
    fun observeAll(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox ORDER BY createdAt, id LIMIT :limit")
    suspend fun peek(limit: Int): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM outbox")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(entry: OutboxEntity): Long

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markAttempt(id: Long, error: String?)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM outbox WHERE entityLocalId = :entityLocalId")
    suspend fun deleteByEntity(entityLocalId: String)

    @Query("DELETE FROM outbox")
    suspend fun clear()
}
