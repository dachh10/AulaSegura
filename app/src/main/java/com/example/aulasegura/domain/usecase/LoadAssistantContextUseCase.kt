package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AssistantChild
import com.example.aulasegura.domain.model.AssistantContext
import com.example.aulasegura.domain.model.AssistantIncident
import com.example.aulasegura.domain.model.AssistantNotice
import com.example.aulasegura.domain.model.AssistantTask
import com.example.aulasegura.domain.model.AssistantToday
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.assistantAttendanceCounts
import com.example.aulasegura.domain.model.assistantIsoDate
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.RosterFailure
import java.util.Calendar
import java.util.TimeZone

/** What the assistant screen got when it asked for the children's data. */
sealed interface AssistantContextResult {

    data class Loaded(val context: AssistantContext) : AssistantContextResult

    /** A tutor account with no linked student yet: nothing to ask about. */
    data object NoChildren : AssistantContextResult

    data object SignedOut : AssistantContextResult

    data class Failed(val reason: RosterFailure) : AssistantContextResult
}

/**
 * Assembles the only data the assistant is allowed to see.
 *
 * It reuses the family reads instead of adding new queries: `loadHome()` already
 * returns the children with today's mark, the current month and the pending
 * tasks, and `loadNotices()`/`loadIncidents()` return what the tutor reads on
 * their own screens. Nothing here touches a collection the tutor could not open.
 *
 * The one deliberate extra read is the previous month. `CONTEXT.md` §9.2 asks for
 * a **30-day** summary, and on the 19th of the month that window starts in the
 * previous month: reusing the month counters would quote 19 days while claiming
 * 30, and the tutor can catch that against the calendar. It only happens when the
 * window actually crosses the month start.
 *
 * Failures are closed, not partial: a half-context would let the model answer
 * "sin avisos" when the truth is "no pude leer los avisos". The screen shows the
 * error and offers a retry instead.
 */
class LoadAssistantContextUseCase(
    private val parents: ParentRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) {

    suspend operator fun invoke(): AssistantContextResult {
        val home = when (val result = parents.loadHome()) {
            is ParentHomeResult.Loaded -> result.children
            ParentHomeResult.SignedOut -> return AssistantContextResult.SignedOut
            is ParentHomeResult.Failed -> return AssistantContextResult.Failed(result.reason)
        }
        if (home.isEmpty()) return AssistantContextResult.NoChildren

        val notices = when (val result = parents.loadNotices()) {
            is ParentNoticesResult.Loaded -> result.notices
            ParentNoticesResult.SignedOut -> return AssistantContextResult.SignedOut
            is ParentNoticesResult.Failed -> return AssistantContextResult.Failed(result.reason)
        }

        val incidents = when (val result = parents.loadIncidents()) {
            is ParentIncidentsResult.Loaded -> result.incidents
            ParentIncidentsResult.SignedOut -> return AssistantContextResult.SignedOut
            is ParentIncidentsResult.Failed -> return AssistantContextResult.Failed(result.reason)
        }

        val moment = now()
        val fromIso = assistantIsoDate(moment - WINDOW_MILLIS, zone)
        val needsPreviousMonth = fromIso < monthStartIso(moment)

        val children = mutableListOf<AssistantChild>()
        home.forEach { overview ->
            val days = overview.month.days.toMutableList()
            if (needsPreviousMonth) {
                when (val previous = parents.loadMonth(overview.child, previousYear(moment), previousMonth(moment))) {
                    is ParentMonthResult.Loaded -> days += previous.month.days
                    ParentMonthResult.SignedOut -> return AssistantContextResult.SignedOut
                    is ParentMonthResult.Failed -> return AssistantContextResult.Failed(previous.reason)
                }
            }

            children += overview.toAssistantChild(days, fromIso)
        }

        return AssistantContextResult.Loaded(
            AssistantContext(
                children = children,
                notices = notices
                    .distinctBy { it.noticeId }
                    .take(CONTEXT_NOTICES)
                    .map {
                        AssistantNotice(
                            title = it.title,
                            body = it.body,
                            urgent = it.urgent,
                            createdAtMillis = it.createdAtMillis,
                        )
                    },
                incidents = incidents
                    .distinctBy { it.incidentId }
                    .take(CONTEXT_INCIDENTS)
                    .map {
                        AssistantIncident(
                            studentName = it.studentName,
                            title = it.title,
                            severity = it.severity,
                            occurredAtMillis = it.occurredAtMillis,
                            status = it.status,
                        )
                    },
            ),
        )
    }

    private fun ParentChildOverview.toAssistantChild(
        days: List<ParentDayAttendance>,
        fromIso: String,
    ): AssistantChild = AssistantChild(
        fullName = child.fullName,
        firstName = child.firstName,
        groupLabel = child.groupLabel,
        attendance = assistantAttendanceCounts(days, fromIso),
        // A session without a record for this child is a missing record, not
        // absence, so today stays unknown instead of turning into a false "falta".
        today = today?.mark?.let { mark -> AssistantToday(mark = mark, time = today.time) },
        pendingTasks = tasks.take(CONTEXT_TASKS).map {
            AssistantTask(
                taskId = it.taskId,
                subject = it.subject,
                title = it.title,
                dueDateMillis = it.dueDateMillis,
            )
        },
    )

    private fun monthStartIso(moment: Long): String {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = moment }
        val start = Calendar.getInstance(zone).apply {
            clear()
            set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1)
        }
        return assistantIsoDate(start.timeInMillis, zone)
    }

    private fun previousYear(moment: Long): Int =
        Calendar.getInstance(zone).apply {
            timeInMillis = moment
            add(Calendar.MONTH, -1)
        }.get(Calendar.YEAR)

    private fun previousMonth(moment: Long): Int =
        // Calendar months are 0-based and the repository speaks 1..12.
        Calendar.getInstance(zone).apply {
            timeInMillis = moment
            add(Calendar.MONTH, -1)
        }.get(Calendar.MONTH) + 1

    private companion object {
        const val WINDOW_MILLIS = 30L * 86_400_000L

        /** Caps that keep one prompt (and one bill) bounded. */
        const val CONTEXT_TASKS = 8
        const val CONTEXT_NOTICES = 8
        const val CONTEXT_INCIDENTS = 8
    }
}
