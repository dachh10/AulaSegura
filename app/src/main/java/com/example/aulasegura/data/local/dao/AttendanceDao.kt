package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Attendance cache: one session per (group, date) and one record per student.
 *
 * Records are merged per student, never replaced as a block, which is the
 * conflict rule in `CONTEXT.md` §8.4.
 */
@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId ORDER BY startedAt DESC")
    fun observeSessionsByGroup(groupId: String): Flow<List<AttendanceSessionEntity>>

    @Query(
        "SELECT * FROM attendance_sessions " +
            "WHERE groupId = :groupId AND date = :date ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun findSession(groupId: String, date: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun findSessionById(sessionId: String): AttendanceSessionEntity?

    /**
     * The one session the screen is watching, so it can paint its real
     * `syncState` instead of a fixed "se sincroniza en segundo plano".
     *
     * With no row it simply emits nothing; that only happens in the window before
     * the local save commits, and the screen already shows its saving state there.
     */
    @Query("SELECT * FROM attendance_sessions WHERE sessionId = :sessionId LIMIT 1")
    fun observeSession(sessionId: String): Flow<AttendanceSessionEntity?>

    @Query("SELECT * FROM attendance_sessions WHERE syncState = :state")
    suspend fun sessionsWithState(state: SyncState): List<AttendanceSessionEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY listNumber")
    fun observeRecords(sessionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY listNumber")
    suspend fun records(sessionId: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE syncState = :state")
    suspend fun recordsWithState(state: SyncState): List<AttendanceRecordEntity>

    @Upsert
    suspend fun upsertSession(session: AttendanceSessionEntity)

    @Upsert
    suspend fun upsertRecords(records: List<AttendanceRecordEntity>)

    @Upsert
    suspend fun upsertRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecords(sessionId: String)

    @Query("DELETE FROM attendance_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM attendance_records")
    suspend fun clearRecords()

    @Query("DELETE FROM attendance_sessions")
    suspend fun clearSessions()
}
