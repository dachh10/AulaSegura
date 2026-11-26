package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.Student
import kotlinx.coroutines.flow.Flow

/**
 * The teacher's groups, read cache-first.
 *
 * Every read comes from Room so the screen never waits on the network and keeps
 * working in airplane mode; [refreshMyGroups] is what brings Firestore into the
 * cache. Screens subscribe to [observeMyGroups] and call refresh once.
 *
 * The signed-in teacher is resolved inside the implementation on purpose: if the
 * caller had to pass `uid` and `schoolId`, one screen would eventually filter by
 * `titularTeacherId` and another by `teacherIds`, and the teacher would see a
 * different set of groups depending on the tab.
 */
interface GroupRepository {

    /** Groups the signed-in teacher teaches, ordered by grade and section. */
    fun observeMyGroups(): Flow<List<Group>>

    /**
     * Pulls the school's groups from Firestore into Room, then refreshes the
     * roster of every group this teacher teaches.
     */
    suspend fun refreshMyGroups(): RosterRefreshResult
}

/**
 * The roster of one group, read cache-first.
 *
 * Inactive students are excluded: a roster is who is in the classroom today, and
 * the seeded data plus the admin flow only ever deactivate, never delete.
 */
interface StudentRepository {

    /** Students of [groupId], ordered by list number. */
    fun observeRoster(groupId: String): Flow<List<Student>>

    /** Pulls one group's roster from Firestore into Room. */
    suspend fun refreshRoster(groupId: String): RosterRefreshResult
}

/** Outcome of a cache refresh. Typed so the screen can say what actually failed. */
sealed interface RosterRefreshResult {

    /** [documents] were written into the cache. */
    data class Updated(val documents: Int) : RosterRefreshResult

    /** Nobody is signed in; the screen is already on its way to the login. */
    data object SignedOut : RosterRefreshResult

    data class Failed(val reason: RosterFailure) : RosterRefreshResult
}

enum class RosterFailure {

    /** No connection. The cache still answers, so this is not a screen error. */
    OFFLINE,

    /** The rules refused the query — almost always a missing `where` filter. */
    DENIED,

    /**
     * The account has no group scope yet, so there is nothing to query.
     *
     * A tutor whose child was accepted "sin grupo por ahora" has no
     * `users.groupIds`, and every family read is scoped by it. That is not a
     * permission problem and must not be shown as one: the data simply does not
     * exist until the director assigns the classroom.
     */
    NO_GROUP,

    UNKNOWN,
}
