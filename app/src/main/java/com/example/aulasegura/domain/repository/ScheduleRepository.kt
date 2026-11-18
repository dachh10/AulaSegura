package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.ClassSlot

/**
 * The timetable of the groups the signed-in teacher teaches.
 *
 * Read on demand and not cached in Room: the documents belong to the groups and
 * are written by the `directivo` role, which does not exist yet. Caching them
 * would need a migration for data that may never arrive, so the home refreshes
 * once per open and degrades to an honest message when Firestore cannot answer.
 */
interface ScheduleRepository {

    /** Every slot of every group of the teacher, or why it could not be read. */
    suspend fun loadSchedule(): ScheduleLoadResult
}

sealed interface ScheduleLoadResult {

    data class Loaded(val slots: List<ClassSlot>) : ScheduleLoadResult

    data object SignedOut : ScheduleLoadResult

    /** Same reasons a roster refresh fails; the home prints a different sentence. */
    data class Failed(val reason: RosterFailure) : ScheduleLoadResult
}
