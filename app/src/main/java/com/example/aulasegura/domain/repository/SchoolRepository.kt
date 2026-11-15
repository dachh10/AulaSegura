package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.SchoolSite
import kotlinx.coroutines.flow.Flow

/**
 * The school of the signed-in user, read cache-first.
 *
 * The geofence has to work in the scenario the phase exists for — a classroom
 * with no signal — so the centre and radius come from Room and Firestore is only
 * the source that fills the cache. Reading `schools/{schoolId}` live at roll
 * call time would make attendance depend on the network at the worst possible
 * moment.
 *
 * The signed-in user is resolved inside the implementation, exactly like
 * `GroupRepository`: if every caller passed `schoolId`, one screen would
 * eventually use the teacher's profile field and another a hardcoded id.
 */
interface SchoolRepository {

    /**
     * Last known school site of the signed-in user.
     *
     * Emits `null` when nothing is cached yet or when the school document has no
     * usable `location`. A null site is not an error: it means "cannot verify",
     * which leaves the roll call unverified instead of blocking it.
     */
    fun observeSchool(): Flow<SchoolSite?>

    /**
     * Pulls the school document into Room.
     *
     * Best effort by design: a failed refresh leaves the cache untouched and the
     * caller keeps whatever it already had. There is no error result because
     * there is no screen message to show — a teacher with no signal does not need
     * to be told that the school document could not be refreshed.
     */
    suspend fun refreshSchool()
}
