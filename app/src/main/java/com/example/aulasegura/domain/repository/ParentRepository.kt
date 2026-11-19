package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentMonthAttendance

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
