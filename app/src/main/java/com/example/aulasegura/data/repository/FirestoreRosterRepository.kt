package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.GroupDao
import com.example.aulasegura.data.local.dao.StudentDao
import com.example.aulasegura.data.local.mapper.groupEntityFrom
import com.example.aulasegura.data.local.mapper.studentEntityFrom
import com.example.aulasegura.data.local.mapper.toDomain
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.RosterRefreshResult
import com.example.aulasegura.domain.repository.StudentRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Cache-first roster reads.
 *
 * One class implements both contracts because the two share the same three
 * moving parts — the same Firestore instance, the same cache and the same
 * session. Splitting it would duplicate the query filters, and duplicated
 * filters are how one screen ends up working and the other returning
 * `PERMISSION_DENIED`.
 *
 * The queries are not free-form: Firestore does not filter by rules, so each
 * query has to carry exactly the predicate the rule checks.
 *  - `groups`: the rule reads `resource.data.schoolId == mySchool()`, so the
 *    query filters by `schoolId`. Teacher filtering happens in memory because
 *    `teacherIds` has no index and the school never has enough groups to matter.
 *  - `students`: the rule checks `resource.data.groupId in myGroups()`, so the
 *    query filters by `groupId` (plus `active`, which the composite index covers).
 *
 * Both refreshes ask for `Source.SERVER` on purpose. A cached read is not a
 * refresh, and — worse — an offline read can come back as an empty snapshot:
 * `refreshRoster` would then conclude that every student left the group and
 * delete the cached roster, which is data loss in the one scenario the cache
 * exists for. A server read either answers for real or fails, and a failure
 * leaves the cache untouched.
 */
class FirestoreRosterRepository(
    private val firestore: FirebaseFirestore,
    private val groupDao: GroupDao,
    private val studentDao: StudentDao,
    private val auth: AuthRepository,
) : GroupRepository, StudentRepository {

    override fun observeMyGroups(): Flow<List<Group>> {
        val user = auth.currentUser ?: return flowOf(emptyList())
        return groupDao.observeBySchool(user.schoolId).map { entities ->
            entities
                .filter { user.uid in it.teacherIds }
                .map { it.toDomain() }
        }
    }

    override suspend fun refreshMyGroups(): RosterRefreshResult {
        val user = auth.currentUser ?: return RosterRefreshResult.SignedOut

        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(GROUPS)
                    .whereEqualTo(FIELD_SCHOOL_ID, user.schoolId)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return RosterRefreshResult.Failed(error.toRosterFailure())
        }

        val groups = snapshot.documents.mapNotNull { document ->
            document.data?.let { groupEntityFrom(document.id, it) }
        }
        groupDao.upsertAll(groups)

        // The roster of every group this teacher teaches, so opening a group
        // works offline on the first try. A roster failure is not reported here:
        // the roster screen refreshes its own group and shows its own message,
        // and failing the whole refresh would hide the groups that did load.
        groups
            .filter { user.uid in it.teacherIds }
            .forEach { refreshRoster(it.groupId) }

        return RosterRefreshResult.Updated(groups.size)
    }

    override fun observeRoster(groupId: String): Flow<List<Student>> =
        studentDao.observeByGroup(groupId).map { entities ->
            entities.filter { it.active }.map { it.toDomain() }
        }

    override suspend fun refreshRoster(groupId: String): RosterRefreshResult {
        auth.currentUser ?: return RosterRefreshResult.SignedOut

        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(STUDENTS)
                    .whereEqualTo(FIELD_GROUP_ID, groupId)
                    .whereEqualTo(FIELD_ACTIVE, true)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return RosterRefreshResult.Failed(error.toRosterFailure())
        }

        val students = snapshot.documents.mapNotNull { document ->
            document.data?.let { studentEntityFrom(document.id, it) }
        }
        studentDao.upsertAll(students)

        // Deactivation is the normal way a student leaves a group, and the query
        // already excludes them — so the ones no longer returned must also leave
        // the cache, or the roster would keep showing them forever.
        if (students.isEmpty()) {
            studentDao.deleteByGroup(groupId)
        } else {
            studentDao.deleteMissing(groupId, students.map { it.studentId })
        }

        return RosterRefreshResult.Updated(students.size)
    }

    private companion object {
        const val GROUPS = "groups"
        const val STUDENTS = "students"
        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_ACTIVE = "active"

        /**
         * Ceiling for one server read. Long enough for a school Wi-Fi hiccup, short
         * enough that a teacher with no signal gets the offline message while still
         * in the classroom.
         */
        const val REFRESH_TIMEOUT_MILLIS = 12_000L
    }
}

/**
 * Turns a failed refresh into the reason the screen will show.
 *
 * A top-level function, not a private one, so the mapping is testable without a
 * Firebase session: this table decides whether a teacher reads "sin conexión" or
 * a generic error, and getting it wrong is invisible in code review.
 */
internal fun Exception.toRosterFailure(): RosterFailure = when {
    // A server read that never answers means the same to the teacher as no
    // connection: the cache is what they are looking at.
    this is TimeoutCancellationException -> RosterFailure.OFFLINE

    this is FirebaseNetworkException -> RosterFailure.OFFLINE

    // With `Source.SERVER` and no connection the SDK reports UNAVAILABLE, not
    // `FirebaseNetworkException`. Same meaning for the teacher.
    this is FirebaseFirestoreException &&
        code == FirebaseFirestoreException.Code.UNAVAILABLE -> RosterFailure.OFFLINE

    this is FirebaseFirestoreException &&
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED -> RosterFailure.DENIED

    else -> RosterFailure.UNKNOWN
}
