package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AttendanceStart
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger

/**
 * Opens a roll call for [groupId] and logs that it started.
 *
 * The event is fired here, not in the screen, for two reasons: the screen is
 * recomposed constantly and would log a start per rotation, and "started" has a
 * business meaning (a teacher opened a group and began marking) that only this
 * seam knows about.
 *
 * Nothing is written to Firestore yet. The session exists once the teacher
 * saves it, so abandoning the screen leaves no open session behind — which is
 * also why the event does not need a session id.
 */
class StartRollCallUseCase(
    private val attendance: AttendanceRepository,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) {

    suspend operator fun invoke(groupId: String, subjectId: String): AttendanceStart {
        val result = attendance.startDraft(groupId, subjectId)
        if (result is AttendanceStart.Ready) {
            analytics.log(AnalyticsEvent.RollCallStarted)
        }
        return result
    }
}
