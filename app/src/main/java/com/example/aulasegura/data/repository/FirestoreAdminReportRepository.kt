package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminGroupBreakdown
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminTopAbsenceDay
import com.example.aulasegura.domain.repository.AdminReportError
import com.example.aulasegura.domain.repository.AdminReportRepository
import com.example.aulasegura.domain.repository.AdminReportResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.util.AttendanceTimes
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Attendance reports, aggregated in code from `attendanceSessions` documents.
 *
 * Firestore rules do not filter results: every query carries the same
 * `schoolId == mySchool()` equality the rules check. The range queries also carry
 * `date >= from`, `date <= to` and an explicit ascending `orderBy(date)`, covered by
 * the deployed `schoolId ASC + date ASC` composite index; the day card uses a
 * `date` equality.
 * All reads are one-shot `Source.SERVER` snapshots with a single 12 s deadline, so
 * an offline phone reports an error instead of an empty (but plausible) report.
 *
 * Totals are the sum of every session's `counts` map. A malformed map contributes
 * zeros for its unreadable keys rather than crashing the whole report.
 */
class FirestoreAdminReportRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val todayIso: () -> String = { AttendanceTimes.isoDate(System.currentTimeMillis()) },
) : AdminReportRepository {

    override suspend fun getRangeSummary(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<AdminReportRangeSummary> = reportCall { schoolId ->
        val sessions = loadSessions(schoolId, fromDate, toDate)
        AdminReportRangeSummary(
            sessionCount = sessions.size,
            presentCount = sessions.sumOf { it.present },
            absentCount = sessions.sumOf { it.absent },
            lateCount = sessions.sumOf { it.late },
            justifiedCount = sessions.sumOf { it.justified },
        )
    }

    override suspend fun getGroupBreakdown(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<List<AdminGroupBreakdown>> = reportCall { schoolId ->
        val sessions = loadSessions(schoolId, fromDate, toDate)
        val labels = loadGroupLabels(schoolId)
        sessions
            .groupBy { it.groupId }
            .map { (groupId, rows) ->
                AdminGroupBreakdown(
                    groupId = groupId,
                    groupLabel = labels[groupId] ?: groupId,
                    sessionCount = rows.size,
                    presentCount = rows.sumOf { it.present },
                    absentCount = rows.sumOf { it.absent },
                    lateCount = rows.sumOf { it.late },
                    justifiedCount = rows.sumOf { it.justified },
                )
            }
    }

    override suspend fun getTopAbsenceDays(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<List<AdminTopAbsenceDay>> = reportCall { schoolId ->
        loadSessions(schoolId, fromDate, toDate)
            .groupBy { it.date }
            .map { (date, rows) ->
                AdminTopAbsenceDay(
                    date = date,
                    sessionCount = rows.size,
                    absentCount = rows.sumOf { it.absent },
                    lateCount = rows.sumOf { it.late },
                )
            }
    }

    override suspend fun getTodaySummary(): AdminReportResult<AdminReportRangeSummary> =
        reportCall { schoolId ->
            val today = todayIso()
            val sessions = loadSnapshot(
                baseQuery(schoolId).whereEqualTo(FIELD_DATE, today),
            )
            AdminReportRangeSummary(
                sessionCount = sessions.size,
                presentCount = sessions.sumOf { it.present },
                absentCount = sessions.sumOf { it.absent },
                lateCount = sessions.sumOf { it.late },
                justifiedCount = sessions.sumOf { it.justified },
            )
        }

    /** One deadline and one error mapping for every public read. */
    private suspend fun <T> reportCall(
        block: suspend (schoolId: String) -> T,
    ): AdminReportResult<T> {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminReportResult.Failure(AdminReportError.NO_SESSION)

        return try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                AdminReportResult.Success(block(schoolId))
            }
        } catch (error: TimeoutCancellationException) {
            AdminReportResult.Failure(AdminReportError.TIMEOUT)
        } catch (error: FirebaseFirestoreException) {
            AdminReportResult.Failure(error.toDomainError())
        } catch (error: Exception) {
            AdminReportResult.Failure(AdminReportError.UNKNOWN)
        }
    }

    private suspend fun loadSessions(
        schoolId: String,
        fromDate: String,
        toDate: String,
    ): List<SessionTally> = loadSnapshot(
        baseQuery(schoolId)
            .whereGreaterThanOrEqualTo(FIELD_DATE, fromDate)
            .whereLessThanOrEqualTo(FIELD_DATE, toDate)
            // Firestore infers ASC for a range without orderBy. Stating it removes
            // the ambiguity and matches the direction of the deployed index.
            .orderBy(FIELD_DATE, Query.Direction.ASCENDING),
    )

    /** The mandatory school clause: rules allow the read, Firestore does not filter. */
    private fun baseQuery(schoolId: String): Query =
        firestore.collection(COLLECTION_SESSIONS).whereEqualTo(FIELD_SCHOOL_ID, schoolId)

    private suspend fun loadSnapshot(query: Query): List<SessionTally> =
        query.get(Source.SERVER).await().documents.map { document ->
            val data = document.data.orEmpty()
            SessionTally(
                groupId = data[FIELD_GROUP_ID] as? String ?: "",
                date = data[FIELD_DATE] as? String ?: "",
                present = count(data[FIELD_COUNTS], KEY_PRESENT),
                absent = count(data[FIELD_COUNTS], KEY_ABSENT),
                late = count(data[FIELD_COUNTS], KEY_LATE),
                justified = count(data[FIELD_COUNTS], KEY_JUSTIFIED),
            )
        }

    /** Group names make the per-group bars readable; one school-filtered read. */
    private suspend fun loadGroupLabels(schoolId: String): Map<String, String> =
        firestore.collection(COLLECTION_GROUPS)
            .whereEqualTo(FIELD_SCHOOL_ID, schoolId)
            .get(Source.SERVER)
            .await()
            .documents
            .associate { document ->
                val name = document.data?.get(FIELD_NAME) as? String
                document.id to (name?.takeIf { it.isNotBlank() } ?: document.id)
            }

    /** Unreadable or negative counts become zero instead of skewing the report. */
    private fun count(counts: Any?, key: String): Int {
        val map = counts as? Map<*, *> ?: return 0
        return (map[key] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    }

    private fun FirebaseFirestoreException.toDomainError(): AdminReportError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        -> AdminReportError.DENIED

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> AdminReportError.NETWORK

        else -> AdminReportError.UNKNOWN
    }

    /** Flattened tallies of one session document. */
    private data class SessionTally(
        val groupId: String,
        val date: String,
        val present: Int,
        val absent: Int,
        val late: Int,
        val justified: Int,
    )

    private companion object {
        const val COLLECTION_SESSIONS = "attendanceSessions"
        const val COLLECTION_GROUPS = "groups"

        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_DATE = "date"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_COUNTS = "counts"
        const val FIELD_NAME = "name"

        const val KEY_PRESENT = "present"
        const val KEY_ABSENT = "absent"
        const val KEY_LATE = "late"
        const val KEY_JUSTIFIED = "justified"

        const val READ_TIMEOUT_MILLIS = 12_000L
    }
}
