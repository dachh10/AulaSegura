package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.subjectsForGrade
import com.example.aulasegura.domain.model.AdminScheduleSlot
import com.example.aulasegura.domain.repository.AdminGroupError
import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminGroupResult
import com.example.aulasegura.domain.repository.AdminStudentFields
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.canonicalGroupId
import com.example.aulasegura.domain.repository.manualGroupDocument
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * The principal's groups, straight from Firestore.
 *
 * Reads are one-shot `get(Source.SERVER)` with a timeout: the rules do not filter
 * results, so the `schoolId` equality filter is mandatory, and a live listener would
 * burn Spark's read budget while the screen is open.
 *
 * [assignTeacher] is the reason this block exists. It writes
 * `groups/{id}.teacherIds` and `users/{uid}.groupIds` in ONE batch. The rules read
 * the user side to decide what a teacher may see, so a one-sided write would either
 * desynchronize the link or fail the rule; the batch is the only correct path.
 */
class FirestoreAdminGroupRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : AdminGroupRepository {

    override suspend fun getGroups(): List<AdminGroup> {
        val schoolId = auth.currentUser?.schoolId ?: return emptyList()

        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(AdminGroupFields.COLLECTION)
                .whereEqualTo(AdminGroupFields.SCHOOL_ID, schoolId)
                .get(Source.SERVER)
                .await()
        }
        // Schedules are a subcollection: reading them for every card would be an
        // N+1 query. The detail screen loads them only for the group it opens.
        return snapshot.documents.map { AdminGroup.fromDocument(it.id, it.safeData()) }
    }

    override suspend fun getGroup(groupId: String): AdminGroup? {
        if (auth.currentUser == null) return null

        val groupRef = firestore.collection(AdminGroupFields.COLLECTION).document(groupId)
        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) { groupRef.get(Source.SERVER).await() }
        if (!snapshot.exists()) return null

        val schedule = withTimeout(READ_TIMEOUT_MILLIS) {
            groupRef.collection(AdminGroupFields.SCHEDULE_COLLECTION)
                .get(Source.SERVER)
                .await()
        }.documents.map { AdminScheduleSlot.fromDocument(it.id, it.safeData()) }

        return AdminGroup.fromDocument(snapshot.id, snapshot.safeData(), schedule)
    }

    override suspend fun createGroup(
        name: String,
        grade: Int,
        section: String,
    ): AdminGroupResult {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminGroupResult.Failure(AdminGroupError.NO_SESSION)

        // The same grade+section is always the same classroom, whether it was
        // created here, by a QR approval or by assigning students. A random id
        // made "5° C" exist twice and split teachers from students. The merge
        // payload leaves calculated fields (teacherIds, studentCount) alone.
        val document = manualGroupDocument(grade, section, name, schoolId).toMutableMap()
        document[AdminGroupFields.UPDATED_AT] = FieldValue.serverTimestamp()

        return try {
            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(AdminGroupFields.COLLECTION)
                    .document(canonicalGroupId(grade, section))
                    .set(document, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }
            AdminGroupResult.Success
        } catch (error: Exception) {
            mapError(error)
        }
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        grade: Int,
        section: String,
    ): AdminGroupResult {
        if (auth.currentUser == null) {
            return AdminGroupResult.Failure(AdminGroupError.NO_SESSION)
        }

        val changes = AdminGroupFields.groupUpdateDocument(name, grade, section).toMutableMap()
        // The exact whitelist: the editable fields plus a server timestamp. No
        // schoolId, no counters, no teacher links.
        changes[AdminGroupFields.UPDATED_AT] = FieldValue.serverTimestamp()

        return try {
            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(AdminGroupFields.COLLECTION)
                    .document(groupId)
                    .update(changes)
                    .await()
            }
            AdminGroupResult.Success
        } catch (error: Exception) {
            mapError(error)
        }
    }

    override suspend fun deleteGroup(
        groupId: String,
        destinationGroupId: String?,
    ): AdminGroupResult {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminGroupResult.Failure(AdminGroupError.NO_SESSION)

        return try {
            val groupRef = firestore.collection(AdminGroupFields.COLLECTION).document(groupId)
            val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
                groupRef.get(Source.SERVER).await()
            }
            if (!snapshot.exists()) {
                return AdminGroupResult.Failure(AdminGroupError.UNKNOWN)
            }

            val studentCount = (snapshot.get(AdminGroupFields.STUDENT_COUNT) as? Number)?.toInt() ?: 0
            val movingStudents = studentCount > 0

            // Validate the destination BEFORE building the batch: a bad choice must
            // never leave a half-written move. The query keeps the schoolId clause so
            // the students read rule (resource.data.schoolId) is satisfied.
            val sourceStudents = if (movingStudents) {
                val destination = destinationGroupId?.trim()
                    ?: return AdminGroupResult.Failure(AdminGroupError.DESTINATION_REQUIRED)
                if (destination.isBlank()) {
                    return AdminGroupResult.Failure(AdminGroupError.DESTINATION_REQUIRED)
                }
                if (destination == groupId) {
                    return AdminGroupResult.Failure(AdminGroupError.DESTINATION_SAME_AS_SOURCE)
                }
                val destinationSnapshot = withTimeout(READ_TIMEOUT_MILLIS) {
                    firestore.collection(AdminGroupFields.COLLECTION)
                        .document(destination)
                        .get(Source.SERVER)
                        .await()
                }
                if (!destinationSnapshot.exists() ||
                    destinationSnapshot.getString(AdminGroupFields.SCHOOL_ID) != schoolId
                ) {
                    return AdminGroupResult.Failure(AdminGroupError.DESTINATION_NOT_FOUND)
                }

                withTimeout(READ_TIMEOUT_MILLIS) {
                    firestore.collection(AdminStudentFields.COLLECTION)
                        .whereEqualTo(AdminStudentFields.SCHOOL_ID, schoolId)
                        .whereEqualTo(AdminStudentFields.GROUP_ID, groupId)
                        .get(Source.SERVER)
                        .await()
                }
            } else {
                null
            }

            val schedule = withTimeout(READ_TIMEOUT_MILLIS) {
                groupRef.collection(AdminGroupFields.SCHEDULE_COLLECTION)
                    .get(Source.SERVER)
                    .await()
            }

            val batch = firestore.batch()
            if (sourceStudents != null) {
                val destination = destinationGroupId!!.trim()
                val moved = sourceStudents.documents
                moved.forEach { student ->
                    // Only groupId changes: `active` and every tutor link survive.
                    batch.update(
                        student.reference,
                        mapOf(
                            AdminStudentFields.GROUP_ID to destination,
                            AdminStudentFields.UPDATED_AT to FieldValue.serverTimestamp(),
                        ),
                    )
                }
                val delta = moved.size.toLong()
                batch.update(
                    groupRef,
                    mapOf(
                        AdminGroupFields.STUDENT_COUNT to FieldValue.increment(-delta),
                        AdminGroupFields.UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                batch.update(
                    firestore.collection(AdminGroupFields.COLLECTION).document(destination),
                    mapOf(
                        AdminGroupFields.STUDENT_COUNT to FieldValue.increment(delta),
                        AdminGroupFields.UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
            }
            schedule.documents.forEach { batch.delete(it.reference) }
            batch.delete(groupRef)
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminGroupResult.Success
        } catch (error: Exception) {
            mapError(error)
        }
    }

    override suspend fun assignTeacher(
        groupId: String,
        teacherUid: String,
        assign: Boolean,
    ): AdminGroupResult {
        if (auth.currentUser == null) {
            return AdminGroupResult.Failure(AdminGroupError.NO_SESSION)
        }

        return try {
            val groupRef = firestore.collection(AdminGroupFields.COLLECTION).document(groupId)
            val userRef = firestore.collection(AdminGroupFields.USER_COLLECTION).document(teacherUid)

            val link = if (assign) FieldValue.arrayUnion(teacherUid) else FieldValue.arrayRemove(teacherUid)
            val reverse = if (assign) FieldValue.arrayUnion(groupId) else FieldValue.arrayRemove(groupId)

            // A manual assignment must leave the same two-sided links as the
            // approval flow: the teacher's subject for this group's grade and a
            // subjectTeachers slot that no other teacher can take.
            val groupSnapshot = withTimeout(WRITE_TIMEOUT_MILLIS) { groupRef.get().await() }
            val grade = groupSnapshot.getLong(AdminGroupFields.GRADE)?.toInt()
            @Suppress("UNCHECKED_CAST")
            val subjectTeachers = (groupSnapshot.get("subjectTeachers") as? Map<String, Any?>)
                ?.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
                ?.toMap() ?: emptyMap()
            val freeSubject = if (assign && grade != null) {
                subjectsForGrade(grade).firstOrNull { it.id !in subjectTeachers }
            } else null
            val ownedSubject = if (!assign) {
                subjectTeachers.entries.firstOrNull { it.value == teacherUid }?.key
            } else null

            val groupPayload = mutableMapOf<String, Any>(
                AdminGroupFields.TEACHER_IDS to link,
                AdminGroupFields.UPDATED_AT to FieldValue.serverTimestamp(),
            )
            if (freeSubject != null) {
                groupPayload["subjectTeachers.${freeSubject.id}"] = teacherUid
            }
            if (ownedSubject != null) {
                groupPayload["subjectTeachers.$ownedSubject"] = FieldValue.delete()
            }
            val userPayload = mutableMapOf<String, Any>(
                AdminUserFields.GROUP_IDS to reverse,
                AdminUserFields.UPDATED_AT to FieldValue.serverTimestamp(),
            )
            if (freeSubject != null) {
                userPayload[AdminUserFields.SUBJECTS] = listOf(freeSubject.label)
            }

            val batch = firestore.batch()
            // Both sides commit atomically: there is no observable state in which
            // only one side has changed. No titular is written: a group can have
            // several teachers and no single one is singled out.
            batch.update(groupRef, groupPayload)
            batch.update(userRef, userPayload)

            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminGroupResult.Success
        } catch (error: Exception) {
            mapError(error)
        }
    }

    override suspend fun saveSchedule(
        groupId: String,
        slots: List<AdminScheduleSlot>,
    ): AdminGroupResult {
        if (auth.currentUser == null) {
            return AdminGroupResult.Failure(AdminGroupError.NO_SESSION)
        }

        return try {
            val scheduleRef = firestore.collection(AdminGroupFields.COLLECTION)
                .document(groupId)
                .collection(AdminGroupFields.SCHEDULE_COLLECTION)

            val desiredIds = slots.map { AdminGroupFields.slotId(it.weekday, it.startTime) }.toSet()
            val existing = withTimeout(READ_TIMEOUT_MILLIS) {
                scheduleRef.get(Source.SERVER).await()
            }

            val stale = existing.documents.filter { it.id !in desiredIds }
            if (stale.isEmpty() && slots.isEmpty()) return AdminGroupResult.Success

            val batch = firestore.batch()
            stale.forEach { batch.delete(it.reference) }
            slots.forEach { slot ->
                val slotId = AdminGroupFields.slotId(slot.weekday, slot.startTime)
                batch.set(scheduleRef.document(slotId), AdminGroupFields.slotDocument(slot))
            }
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminGroupResult.Success
        } catch (error: Exception) {
            mapError(error)
        }
    }

    private fun mapError(error: Exception): AdminGroupResult.Failure {
        if (error is TimeoutCancellationException) {
            return AdminGroupResult.Failure(AdminGroupError.TIMEOUT)
        }
        if (error is FirebaseFirestoreException) {
            val reason = when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
                -> AdminGroupError.DENIED

                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> AdminGroupError.NETWORK

                else -> AdminGroupError.UNKNOWN
            }
            // Diagnostics only: the real-device code/message tells us which write
            // the deployed rules reject. NETWORK keeps its honest copy with no detail.
            val detail = when (reason) {
                AdminGroupError.DENIED, AdminGroupError.UNKNOWN ->
                    firestoreDetail(error.code.name, error.message)

                else -> null
            }
            return AdminGroupResult.Failure(reason, detail)
        }
        return AdminGroupResult.Failure(AdminGroupError.UNKNOWN)
    }

    private companion object {
        const val READ_TIMEOUT_MILLIS = 12_000L
        const val WRITE_TIMEOUT_MILLIS = 12_000L
    }
}

/** Upper bound for the whole diagnostic string, code included. */
private const val FIRESTORE_DETAIL_MAX_LENGTH = 160

/**
 * Builds a short, single-line diagnostic from a Firestore code and message.
 *
 * Only the first line is kept (no stack traces), common secret patterns are
 * redacted, whitespace is collapsed and the result is bounded. No document
 * contents are read here — just the code and the platform message.
 */
private fun firestoreDetail(code: String, message: String?): String {
    val canonicalCode = code.lowercase().replace('_', '-')
    val safeMessage = message
        ?.lineSequence()
        ?.firstOrNull()
        .orEmpty()
        .replace(Regex("(?i)(bearer\\s+)\\S+"), "$1[redacted]")
        .replace(
            Regex("(?i)(token|secret|password|authorization|api[_-]?key)\\s*[=:]\\s*\\S+"),
            "[redacted]",
        )
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(FIRESTORE_DETAIL_MAX_LENGTH)
    val detail = if (safeMessage.isEmpty()) canonicalCode else "$canonicalCode: $safeMessage"
    return detail.take(FIRESTORE_DETAIL_MAX_LENGTH)
}

/**
 * The Firestore platform type is `Map<String, Any>!`, which is not assignable to
 * `Map<String, Any?>` because `Map` is invariant in its value type. One cast at the
 * data boundary keeps the domain mapper free of Firebase types.
 */
@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.safeData(): Map<String, Any?> =
    data as? Map<String, Any?> ?: emptyMap()
