package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.ParentChild
import kotlinx.coroutines.flow.Flow
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.model.ParentTaskItem

/**
 * Read side of the tutor's screen: what the school already produced for the
 * children linked to `students.tutorIds`.
 *
 * Deliberately read-only and online-first. Unlike the teacher's roster there is
 * no Room copy to fall back to: a family opens the app a few times a day, and a
 * cached "no sessions" would read as a lost month instead of a slow network.
 */
interface ParentRepository {

    /** The children linked to the open session, in school order. */
    suspend fun loadChildren(): ParentChildrenResult

/** Live count/ids of the tutor's children, to notice a newly approved sibling. */
fun observeMyChildren(): Flow<List<String>> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * Sessions of [child]'s group in the given month, each with the child's own
     * record. [month] is 1-based, the way `Calendar` and the UI read it.
     */
    suspend fun loadMonth(child: ParentChild, year: Int, month: Int): ParentMonthResult

    /**
     * Everything the home card shows: every child with today's status, the
     * month counters and the tasks still due for their group.
     */
    suspend fun loadHome(): ParentHomeResult

    /**
     * Every task published to [child]'s group, due date included.
     *
     * Unlike the home card this list is not narrowed to what is still due: the
     * tutor opens it to see the overdue ones too, and the screen's filters are
     * what decide which slice is on screen.
     */
    suspend fun loadTasks(child: ParentChild): ParentTasksResult

    /** One task by document id, for the detail route. */
    suspend fun loadTask(taskId: String): ParentTaskResult

    /**
     * The notices addressed to the families of every child's group, newest
     * first, each with the tutor's own read mark.
     */
    suspend fun loadNotices(): ParentNoticesResult

    /**
     * Idempotent, like the document id it writes: opening a notice twice keeps
     * one `noticeReads` document.
     */
    suspend fun markNoticeRead(noticeId: String): NoticeReadResult

    /**
     * Every incident the school registered about the tutor's children, newest
     * first. The tutor's own read mark travels in each row.
     */
    suspend fun loadIncidents(): ParentIncidentsResult

    /** One incident by document id, for the detail and the map. */
    suspend fun loadIncident(incidentId: String): ParentIncidentResult

    /**
     * Adds the signed-in tutor to `seenByTutorIds`.
     *
     * The rules allow this and only this write, and they cannot undo it: a tutor
     * acknowledges an incident once. That is why the mark is never optimistic on
     * the detail screen — it appears when the cloud accepted it.
     */
    suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult
}

sealed interface ParentChildrenResult {

    data class Loaded(val children: List<ParentChild>) : ParentChildrenResult

    data object SignedOut : ParentChildrenResult

    data class Failed(val reason: RosterFailure) : ParentChildrenResult
}

sealed interface ParentMonthResult {

    data class Loaded(val month: ParentMonthAttendance) : ParentMonthResult

    data object SignedOut : ParentMonthResult

    data class Failed(val reason: RosterFailure) : ParentMonthResult
}

sealed interface ParentHomeResult {

    /** Empty when the account is a tutor with no linked student yet. */
    data class Loaded(val children: List<ParentChildOverview>) : ParentHomeResult

    data object SignedOut : ParentHomeResult

    data class Failed(val reason: RosterFailure) : ParentHomeResult
}

sealed interface ParentTasksResult {

    data class Loaded(val tasks: List<ParentTaskItem>) : ParentTasksResult

    data object SignedOut : ParentTasksResult

    data class Failed(val reason: RosterFailure) : ParentTasksResult
}

sealed interface ParentTaskResult {

    data class Loaded(val task: ParentTaskItem) : ParentTaskResult

    /** The document does not exist (or was removed after the list was read). */
    data object NotFound : ParentTaskResult

    data object SignedOut : ParentTaskResult

    data class Failed(val reason: RosterFailure) : ParentTaskResult
}

sealed interface ParentNoticesResult {

    /** Children are returned with the notices so the UI does not reload them. */
    data class Loaded(
        val notices: List<ParentNoticeItem>,
        val children: List<ParentChild> = emptyList(),
    ) : ParentNoticesResult

    data object SignedOut : ParentNoticesResult

    data class Failed(val reason: RosterFailure) : ParentNoticesResult
}

sealed interface NoticeReadResult {

    data object Marked : NoticeReadResult

    data object SignedOut : NoticeReadResult

    data class Failed(val reason: RosterFailure) : NoticeReadResult
}

sealed interface ParentIncidentsResult {

    data class Loaded(val incidents: List<ParentIncident>) : ParentIncidentsResult

    data object SignedOut : ParentIncidentsResult

    data class Failed(val reason: RosterFailure) : ParentIncidentsResult
}

sealed interface ParentIncidentResult {

    data class Loaded(val incident: ParentIncident) : ParentIncidentResult

    data object NotFound : ParentIncidentResult

    data object SignedOut : ParentIncidentResult

    data class Failed(val reason: RosterFailure) : ParentIncidentResult
}

sealed interface IncidentSeenResult {

    data object Marked : IncidentSeenResult

    data object SignedOut : IncidentSeenResult

    data class Failed(val reason: RosterFailure) : IncidentSeenResult
}
