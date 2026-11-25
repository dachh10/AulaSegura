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

    /**
     * The teacher's history, newest first.
     *
     * Ordered by `date` and not by `startedAt`: the screen groups by day, and the
     * ISO string sorts lexicographically because it is fixed to `Locale.US`
     * (`AttendanceTimes`). `startedAt` breaks ties inside a day.
     */
    @Query(
        "SELECT * FROM attendance_sessions WHERE teacherId = :teacherId " +
            "ORDER BY date DESC, startedAt DESC",
    )
    fun observeSessionsByTeacher(teacherId: String): Flow<List<AttendanceSessionEntity>>

    @Query(
        "SELECT * FROM attendance_sessions " +
            "WHERE groupId = :groupId AND date = :date ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun findSession(groupId: String, date: String): AttendanceSessionEntity?

    /**
     * The one-per-slot guard: a session already closed by this teacher for this
     * group on this date and subject. [subjectKey] is the durable subject label,
     * which legacy sessions also carry; the canonical id is matched as well.
     */
    @Query(
        "SELECT sessionId FROM attendance_sessions " +
            "WHERE groupId = :groupId AND date = :date AND teacherId = :teacherId " +
            "AND (subjectId = :subjectKey OR subject = :subjectKey) " +
            "ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun existingSessionId(
        groupId: String,
        date: String,
        teacherId: String,
        subjectKey: String,
    ): String?

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
    suspend fun upsertSessions(sessions: List<AttendanceSessionEntity>)

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
