package com.example.aulasegura.domain.model

/**
 * Dashboard metrics of the signed-in principal's school.
 *
 * Pure domain data: no Firebase types and no collection names. The four counts come
 * from four different collections, and [status] tells the UI whether the numbers
 * were never read, were read successfully, or were read and are all zero. That
 * distinction is what stops a fresh school from looking like a failed load.
 */
data class AdminDashboard(
    val studentCount: Int = 0,
    val teacherCount: Int = 0,
    val groupCount: Int = 0,
    val incidentCount: Int = 0,
    val status: AdminDashboardStatus = AdminDashboardStatus.NOT_LOADED,
) {
    /** True only after a completed load that found no records at all. */
    val empty: Boolean get() = status == AdminDashboardStatus.EMPTY

    companion object {
        /** Builds a loaded snapshot, deriving EMPTY from an all-zero result. */
        fun loaded(
            studentCount: Int,
            teacherCount: Int,
            groupCount: Int,
            incidentCount: Int,
        ): AdminDashboard {
            val total = studentCount + teacherCount + groupCount + incidentCount
            return AdminDashboard(
                studentCount = studentCount,
                teacherCount = teacherCount,
                groupCount = groupCount,
                incidentCount = incidentCount,
                status = if (total == 0) AdminDashboardStatus.EMPTY else AdminDashboardStatus.LOADED,
            )
        }
    }
}

/** Typed load flag, so the UI never has to guess what a zero means. */
enum class AdminDashboardStatus {
    NOT_LOADED,
    LOADED,
    EMPTY,
}
