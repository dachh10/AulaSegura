package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminGroupBreakdown
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminTopAbsenceDay

/**
 * One-shot attendance report reads for the signed-in principal's school.
 *
 * No listeners: on Spark every snapshot would be billed while the screen stays
 * open, for numbers that change only when a teacher closes a roll call.
 */
interface AdminReportRepository {
    suspend fun getRangeSummary(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<AdminReportRangeSummary>

    suspend fun getGroupBreakdown(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<List<AdminGroupBreakdown>>

    suspend fun getTopAbsenceDays(
        fromDate: String,
        toDate: String,
    ): AdminReportResult<List<AdminTopAbsenceDay>>

    suspend fun getTodaySummary(): AdminReportResult<AdminReportRangeSummary>
}

/** Typed outcome, so the ViewModel never inspects an exception. */
sealed interface AdminReportResult<out T> {
    data class Success<T>(val data: T) : AdminReportResult<T>
    data class Failure(val reason: AdminReportError) : AdminReportResult<Nothing>
}

enum class AdminReportError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    UNKNOWN,
}
