package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.ScheduleLoadResult
import com.example.aulasegura.domain.repository.ScheduleRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * `groups/{groupId}/schedule/{slotId}` for every group the teacher teaches.
 *
 * One read per group instead of a collection-group query on purpose: the rules
 * grant `schedule` through `get(groups/{groupId}).schoolId == mySchool()`, and a
 * collection-group query cannot be authorized that way. A teacher has a handful
 * of groups, so the reads are few and run in sequence inside one timeout.
 *
 * The groups come from the roster cache, so the home does not wait on a second
 * network call just to know which collections to read.
 */
class FirestoreScheduleRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val groups: GroupRepository,
) : ScheduleRepository {

    override suspend fun loadSchedule(): ScheduleLoadResult {
        auth.currentUser ?: return ScheduleLoadResult.SignedOut

        val myGroups = runCatching { groups.observeMyGroups().first() }.getOrDefault(emptyList())
        if (myGroups.isEmpty()) return ScheduleLoadResult.Loaded(emptyList())

        return try {
            val slots = withTimeout(TIMEOUT_MILLIS) {
                myGroups.flatMap { group ->
                    firestore.collection(GROUPS).document(group.id)
                        .collection(SCHEDULE)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { document -> document.toSlot(group.id) }
                }
            }
            ScheduleLoadResult.Loaded(slots)
        } catch (error: Exception) {
            ScheduleLoadResult.Failed(error.toRosterFailure())
        }
    }

    /**
     * A slot without a readable time is dropped: a class the card cannot place in
     * the day would either hide the real next class or print a blank hour.
     */
    private fun DocumentSnapshot.toSlot(groupId: String): ClassSlot? {
        val weekday = (getLong(FIELD_WEEKDAY) ?: return null).toInt()
        if (weekday !in 1..7) return null

        val slot = ClassSlot(
            slotId = id,
            groupId = groupId,
            weekday = weekday,
            startTime = getString(FIELD_START_TIME).orEmpty(),
            endTime = getString(FIELD_END_TIME).orEmpty(),
            subject = getString(FIELD_SUBJECT).orEmpty(),
            teacherId = getString(FIELD_TEACHER_ID),
        )
        return slot.takeIf { it.startMinutes >= 0 && it.endMinutes > it.startMinutes }
    }

    private companion object {
        const val GROUPS = "groups"
        const val SCHEDULE = "schedule"
        const val FIELD_WEEKDAY = "weekday"
        const val FIELD_START_TIME = "startTime"
        const val FIELD_END_TIME = "endTime"
        const val FIELD_SUBJECT = "subject"
        const val FIELD_TEACHER_ID = "teacherId"
        const val TIMEOUT_MILLIS = 8_000L
    }
}
