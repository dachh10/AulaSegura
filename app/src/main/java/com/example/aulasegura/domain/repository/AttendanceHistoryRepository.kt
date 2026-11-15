package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import kotlinx.coroutines.flow.Flow

/**
 * Read side of the roll call: the sessions the teacher has already closed.
 *
 * Separate from [AttendanceRepository] on purpose. That contract is the write
 * path and is deliberately small so the screen taking attendance cannot
 * accidentally read a session; this one only reads and never writes a session.
 *
 * Cache-first like the roster: [observeHistory] answers from Room, so the history
 * opens in airplane mode with the sessions this device has saved, and
 * [refreshHistory] is what brings the rest from Firestore.
 *
 * The signed-in teacher is resolved inside the implementation. If the screen
 * passed `uid`, one list would filter by `teacherId` and another by `groupIds`
 * and the same teacher would see different histories depending on the tab.
 */
interface AttendanceHistoryRepository {

    /** Sessions of the signed-in teacher, newest first. */
    fun observeHistory(): Flow<List<AttendanceSessionSummary>>

    /** Pulls the teacher's sessions from Firestore into Room. */
    suspend fun refreshHistory(): HistoryRefreshResult

    /** One session with its records, read from the cache. */
    fun observeDetail(sessionId: String): Flow<AttendanceSessionDetail?>

    /**
     * Pulls one session's records from Firestore into Room.
     *
     * Called when a row is expanded, not during the list refresh: fetching 28
     * records for every session in the history would read thousands of documents
     * to paint a list that only needs the counters.
     */
    suspend fun refreshRecords(sessionId: String): HistoryRefreshResult
}

/** Outcome of a history refresh. */
sealed interface HistoryRefreshResult {

    data class Updated(val documents: Int) : HistoryRefreshResult

    data object SignedOut : HistoryRefreshResult

    /** Same reasons a roster refresh fails; the cache still answers. */
    data class Failed(val reason: RosterFailure) : HistoryRefreshResult
}
