package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminDashboard

/**
 * One-shot read of the school's dashboard metrics.
 *
 * Like the other principal reads, this is deliberately not a listener: a refresh is
 * a handful of aggregate reads, and leaving a snapshot open on the dashboard would
 * burn the daily read budget for numbers that change a few times a day.
 */
interface AdminDashboardRepository {
    suspend fun getMetrics(): AdminDashboardResult
}

/** Typed outcome, so the ViewModel never has to inspect an exception. */
sealed interface AdminDashboardResult {
    data class Success(val dashboard: AdminDashboard) : AdminDashboardResult
    data class Failure(val reason: AdminDashboardError) : AdminDashboardResult
}

enum class AdminDashboardError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    UNKNOWN,
}
