package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.displayGroupLabel
import com.example.aulasegura.domain.model.gradeSectionFromGroupId
import com.example.aulasegura.domain.model.subjectIdFromLabel
import com.example.aulasegura.domain.model.ParentAttachment
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentIncidentLocation
import com.example.aulasegura.domain.model.ParentIncidentMedia
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.model.ParentTaskBrief
import com.example.aulasegura.domain.model.ParentTaskItem
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.NoticeReadResult
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentIncidentResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTaskResult
import com.example.aulasegura.domain.repository.ParentTasksResult
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * The tutor's read path.
 *
 * Every query here is shaped so the published rules can prove it:
 *  - `students.whereArrayContains("tutorIds", uid)` is the query form of
 *    `request.auth.uid in resource.data.tutorIds`.
 *  - `attendanceSessions.whereEqualTo("groupId", g)` is the query form of
 *    `resource.data.groupId in myGroups()`.
 *  - `tasks.whereArrayContains("groupIds", g)` is the query form of
 *    `resource.data.groupIds.hasAny(myGroups())`.
 *  - `incidents.whereEqualTo("studentId", s)` is the query form of
 *    `resource.data.studentId in myStudents()`.
 *  - `notices.whereArrayContains("audience.groupIds", g)` is the query form of
 *    `resource.data.audience.groupIds.hasAny(myGroups())`. The tutor branch of
 *    the notice rule deliberately does not check `audience.roles`: Firestore
 *    proves a list query from its own constraints, allows a single
 *    `array-contains`, and an extra unprovable condition denies the whole read.
 *    [parentNoticeFrom] keeps that audience check on the client.
 * A query that does not carry the filter the rule checks is denied, so these
 * three shapes are not an optimisation — they are what makes the read possible.
 *
 * The month filter runs in the client on purpose. Adding a `date` range next to
 * the group filter needs a composite index, and this repository has no
 * `firestore.indexes.json` to declare one without risking indexes that only
 * exist in the console. One group and one school cycle keep the download small.
 */
class FirestoreParentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ParentRepository {


    override fun observeMyChildren(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }
        // whereArrayContains("tutorIds", uid) is the query form the rules can
        // prove for a tutor read; no extra filter that would deny the listener.
        val registration = firestore.collection(STUDENTS)
            .whereArrayContains(FIELD_TUTOR_IDS, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                trySend(snapshot.documents.map { it.id })
            }
        awaitClose { registration.remove() }
    }

    override suspend fun loadChildren(): ParentChildrenResult {
        val user = auth.currentUser ?: return ParentChildrenResult.SignedOut

        val snapshot = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(STUDENTS)
                    .whereArrayContains(FIELD_TUTOR_IDS, user.uid)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return ParentChildrenResult.Failed(error.toRosterFailure())
        }

        val rows = snapshot.documents.mapNotNull { document ->
            document.data?.let { parentChildFrom(document.id, it) }
        }

        val labels = rows.map { it.groupId }.distinct().associateWith { groupLabel(it) }

        return ParentChildrenResult.Loaded(
            rows
                .map { it.copy(groupLabel = displayGroupLabel(it.groupId, labels[it.groupId])) }
                .sortedWith(compareBy({ it.listNumber }, { it.fullName })),
        )
    }

    override suspend fun loadMonth(
        child: ParentChild,
        year: Int,
        month: Int,
        subjectId: String?,
    ): ParentMonthResult {
        auth.currentUser ?: return ParentMonthResult.SignedOut

        val prefix = monthPrefix(year, month)

        // Single equality on purpose: it is the filter the read rule checks and
        // the only one Firestore can serve without a composite index.
        val snapshot = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(SESSIONS)
                    .whereEqualTo(FIELD_GROUP_ID, child.groupId)
                    .limit(SESSION_LIMIT)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return ParentMonthResult.Failed(error.toRosterFailure())
        }

        val sessions = snapshot.documents.mapNotNull { document ->
            val data = document.data ?: return@mapNotNull null
            val date = data[FIELD_DATE] as? String ?: return@mapNotNull null
            if (!date.startsWith(prefix)) return@mapNotNull null

            val label = data[FIELD_SUBJECT] as? String ?: ""
            val resolvedSubjectId = parentSubjectId(
                storedId = data[FIELD_SUBJECT_ID] as? String ?: "",
                label = label,
                grade = gradeSectionFromGroupId(child.groupId)?.first,
            )
            // Unknown legacy labels resolve to "": they belong to "Todas" only.
            if (subjectId != null && resolvedSubjectId != subjectId) return@mapNotNull null

            SessionRow(
                sessionId = document.id,
                date = date,
                subject = label,
                subjectId = resolvedSubjectId,
                startedAt = parentEpochMillis(data[FIELD_STARTED_AT]),
            )
        }

        // One document read per session: `records/{studentId}` is what the tutor
        // is allowed to see, and the record of one child is exactly one document.
        val records = coroutineScope {
            sessions.map { session ->
                async(Dispatchers.IO) { session to readRecord(session.sessionId, child.studentId) }
            }.awaitAll()
        }

        val failed = firstRecordFailure(records.map { it.second })
        if (failed != null) return ParentMonthResult.Failed(failed)

        val days = records
            .mapNotNull { (session, record) ->
                (record as? RecordRead.Found)?.let { session to it.row }
            }
            .map { (session, record) ->
                ParentDayAttendance(
                    date = session.date,
                    subject = session.subject,
                    startedAt = session.startedAt,
                    mark = AttendanceMark.fromWire(record.status),
                    time = record.time,
                    note = record.note,
                    subjectId = session.subjectId,
                    sessionId = session.sessionId,
                )
            }
            .sortedWith(compareByDescending<ParentDayAttendance> { it.date }.thenByDescending { it.startedAt })

        return ParentMonthResult.Loaded(
            ParentMonthAttendance(
                year = year,
                month = month,
                counts = countOf(days),
                days = days,
            ),
        )
    }

    override suspend fun loadHome(subjectId: String?): ParentHomeResult {
        auth.currentUser ?: return ParentHomeResult.SignedOut

        val children = when (val result = loadChildren()) {
            is ParentChildrenResult.Loaded -> result.children
            ParentChildrenResult.SignedOut -> return ParentHomeResult.SignedOut
            is ParentChildrenResult.Failed -> return ParentHomeResult.Failed(result.reason)
        }
        if (children.isEmpty()) return ParentHomeResult.Loaded(emptyList())

        val today = AttendanceTimes.isoDate(now(), zone)
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = now() }
        val year = calendar.get(Calendar.YEAR)
        // Calendar is 0-based; the rest of the app talks about months 1-12.
        val month = calendar.get(Calendar.MONTH) + 1
        val todayStart = AttendanceTimes.isoDateToMillis(today, zone) ?: 0L

        val results = coroutineScope {
            children.map { child ->
                async {
                    when (val monthResult = loadMonth(child, year, month, subjectId)) {
                        is ParentMonthResult.Loaded -> {
                            val tasks = loadTasks(child.groupId, todayStart)
                            if (tasks == null) {
                                HomeLoad.Failed(RosterFailure.UNKNOWN)
                            } else {
                                HomeLoad.Loaded(
                                    ParentChildOverview(
                                        child = child,
                                        today = monthResult.month.days
                                            .firstOrNull { it.date == today },
                                        month = monthResult.month,
                                        tasks = tasks,
                                    ),
                                )
                            }
                        }
                        ParentMonthResult.SignedOut -> HomeLoad.SignedOut
                        is ParentMonthResult.Failed -> HomeLoad.Failed(monthResult.reason)
                    }
                }
            }.awaitAll()
        }

        results.firstOrNull { it is HomeLoad.Failed }?.let { failed ->
            return ParentHomeResult.Failed((failed as HomeLoad.Failed).reason)
        }
        if (results.any { it is HomeLoad.SignedOut }) return ParentHomeResult.SignedOut

        return ParentHomeResult.Loaded(
            results.filterIsInstance<HomeLoad.Loaded>().map { it.overview },
        )
    }

    override suspend fun loadTasks(child: ParentChild): ParentTasksResult {
        auth.currentUser ?: return ParentTasksResult.SignedOut

        val snapshot = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(TASKS)
                    .whereArrayContains(FIELD_GROUP_IDS, child.groupId)
                    .limit(TASK_LIMIT)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return ParentTasksResult.Failed(error.toRosterFailure())
        }

        // A deactivated task (`active = false`) is one the teacher withdrew: it
        // stays in the cloud for the record but is not homework any more.
        return ParentTasksResult.Loaded(
            snapshot.documents
                .mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    if (data[FIELD_ACTIVE] == false) return@mapNotNull null
                    parentTaskFrom(document.id, data)
                }
                .sortedWith(compareBy({ it.dueDateMillis }, { it.title })),
        )
    }

    /**
     * One task by document id.
     *
     * A single-document read, not a list: the rules evaluate it against the real
     * document, so the detail keeps working even for a task the tutor reached
     * from a notification before the list was ever opened.
     */
    override suspend fun loadTask(taskId: String): ParentTaskResult {
        auth.currentUser ?: return ParentTaskResult.SignedOut

        val document = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(TASKS).document(taskId).get(Source.SERVER).await()
            }
        } catch (error: Exception) {
            return ParentTaskResult.Failed(error.toRosterFailure())
        }

        if (!document.exists()) return ParentTaskResult.NotFound
        val data = document.data ?: return ParentTaskResult.NotFound
        val task = parentTaskFrom(document.id, data) ?: return ParentTaskResult.NotFound
        return ParentTaskResult.Loaded(task)
    }

    override suspend fun loadNotices(): ParentNoticesResult {
        val user = auth.currentUser ?: return ParentNoticesResult.SignedOut

        val children = when (val result = loadChildren()) {
            is ParentChildrenResult.Loaded -> result.children
            ParentChildrenResult.SignedOut -> return ParentNoticesResult.SignedOut
            is ParentChildrenResult.Failed -> return ParentNoticesResult.Failed(result.reason)
        }
        if (children.isEmpty()) return ParentNoticesResult.Loaded(emptyList(), children)

        // The read mark is fetched once and joined in memory: one query for the
        // whole inbox instead of one document read per notice.
        val readIds = noticeReadIds(user.uid)
        val moment = now()

        val byGroup = coroutineScope {
            children.map { it.groupId }.distinct().map { groupId ->
                async(Dispatchers.IO) { noticesOf(groupId, readIds, moment) }
            }.awaitAll()
        }
        if (byGroup.any { it == null }) {
            // One group failing is a read problem, not "this group has no notices".
            return ParentNoticesResult.Failed(RosterFailure.UNKNOWN)
        }

        // Siblings in the same group are one query; siblings in different groups
        // cannot publish the same notice twice, and `distinctBy` keeps it that way
        // if the school ever addresses one notice to both groups.
        return ParentNoticesResult.Loaded(
            notices = byGroup.filterNotNull().flatten()
                .distinctBy { it.noticeId }
                .sortedWith(compareByDescending<ParentNoticeItem> { it.createdAtMillis }.thenBy { it.title }),
            children = children,
        )
    }

    override suspend fun markNoticeRead(noticeId: String): NoticeReadResult {
        val user = auth.currentUser ?: return NoticeReadResult.SignedOut

        return try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(USERS)
                    .document(user.uid)
                    .collection(NOTICE_READS)
                    .document(noticeId)
                    .set(mapOf(FIELD_READ_AT to FieldValue.serverTimestamp()))
                    .await()
            }
            NoticeReadResult.Marked
        } catch (error: Exception) {
            NoticeReadResult.Failed(error.toRosterFailure())
        }
    }

    override suspend fun loadIncidents(): ParentIncidentsResult {
        val user = auth.currentUser ?: return ParentIncidentsResult.SignedOut

        val children = when (val result = loadChildren()) {
            is ParentChildrenResult.Loaded -> result.children
            ParentChildrenResult.SignedOut -> return ParentIncidentsResult.SignedOut
            is ParentChildrenResult.Failed -> return ParentIncidentsResult.Failed(result.reason)
        }
        if (children.isEmpty()) return ParentIncidentsResult.Loaded(emptyList())

        // One equality query per child: `incidents` is filtered by `studentId`,
        // which is the filter the tutor rule checks. A single `in` with the whole
        // list would also work, but per-child keeps the shape identical to the
        // attendance reads and a failure maps to one child, not the screen.
        val byStudent = coroutineScope {
            children.map { it.studentId }.distinct().map { studentId ->
                async(Dispatchers.IO) { incidentsOf(studentId, user.uid) }
            }.awaitAll()
        }
        if (byStudent.any { it == null }) {
            return ParentIncidentsResult.Failed(RosterFailure.UNKNOWN)
        }

        return ParentIncidentsResult.Loaded(
            byStudent.filterNotNull().flatten()
                .distinctBy { it.incidentId }
                .sortedWith(
                    compareByDescending<ParentIncident> { it.occurredAtMillis }.thenBy { it.title },
                ),
        )
    }

    override suspend fun loadIncident(incidentId: String): ParentIncidentResult {
        val user = auth.currentUser ?: return ParentIncidentResult.SignedOut

        val document = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(INCIDENTS)
                    .document(incidentId)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return ParentIncidentResult.Failed(error.toRosterFailure())
        }

        if (!document.exists()) return ParentIncidentResult.NotFound
        val incident = document.data?.let { parentIncidentFrom(document.id, it, user.uid) }
            ?: return ParentIncidentResult.NotFound
        return ParentIncidentResult.Loaded(incident)
    }

    override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult {
        val user = auth.currentUser ?: return IncidentSeenResult.SignedOut

        return try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(INCIDENTS)
                    .document(incidentId)
                    .update(FIELD_SEEN_BY_TUTOR_IDS, FieldValue.arrayUnion(user.uid))
                    .await()
            }
            IncidentSeenResult.Marked
        } catch (error: Exception) {
            IncidentSeenResult.Failed(error.toRosterFailure())
        }
    }

    /** `null` when the child's incidents could not be read at all. */
    private suspend fun incidentsOf(studentId: String, uid: String): List<ParentIncident>? = try {
        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(INCIDENTS)
                .whereEqualTo(FIELD_STUDENT_ID, studentId)
                .limit(INCIDENT_LIMIT)
                .get(Source.SERVER)
                .await()
        }
        snapshot.documents.mapNotNull { document ->
            document.data?.let { parentIncidentFrom(document.id, it, uid) }
        }
    } catch (_: Exception) {
        null
    }

    /**
     * The ids the tutor already opened, or an empty set when they cannot be read.
     *
     * Failing the whole screen because the mark is missing would hide the school's
     * notices over a decoration; painting them unread is recoverable and visible.
     */
    private suspend fun noticeReadIds(uid: String): Set<String> = try {
        withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(USERS)
                .document(uid)
                .collection(NOTICE_READS)
                .get(Source.SERVER)
                .await()
        }.documents.map { it.id }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    /** `null` when the group's notices could not be read at all. */
    private suspend fun noticesOf(
        groupId: String,
        readIds: Set<String>,
        moment: Long,
    ): List<ParentNoticeItem>? = try {
        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(NOTICES)
                .whereArrayContains(FIELD_AUDIENCE_GROUP_IDS, groupId)
                .limit(NOTICE_LIMIT)
                .get(Source.SERVER)
                .await()
        }
        snapshot.documents.mapNotNull { document ->
            document.data?.let { parentNoticeFrom(document.id, it, readIds, moment, groupId) }
        }
    } catch (_: Exception) {
        null
    }

    private suspend fun groupLabel(groupId: String): String? = try {
        val document = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(GROUPS).document(groupId).get(Source.SERVER).await()
        }
        document.getString(FIELD_NAME)
    } catch (_: Exception) {
        // A missing label costs the group name, not the child: the UI falls back
        // to the raw id instead of hiding a linked student.
        null
    }

    private suspend fun readRecord(sessionId: String, studentId: String): RecordRead<RecordRow> =
        try {
            val document = withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(SESSIONS)
                    .document(sessionId)
                    .collection(RECORDS)
                    .document(studentId)
                    .get(Source.SERVER)
                    .await()
            }
            if (!document.exists()) {
                // A newly linked student has no note in older sessions. That is
                // an empty month, not a failed read.
                RecordRead.Missing
            } else {
                RecordRead.Found(
                    RecordRow(
                        status = document.getString(FIELD_STATUS).orEmpty(),
                        time = document.getString(FIELD_TIME),
                        note = document.getString(FIELD_NOTE),
                    ),
                )
            }
        } catch (error: Exception) {
            RecordRead.Failed(error.toRosterFailure())
        }

    /**
     * Tasks still due for [groupId], soonest first.
     *
     * No `orderBy` on purpose: `array-contains` plus a range needs a composite
     * index. The group publishes a handful of tasks per week, so ordering in the
     * client is cheaper than a new index for data this small.
     */
    private suspend fun loadTasks(groupId: String, todayStart: Long): List<ParentTaskBrief>? = try {
        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(TASKS)
                .whereArrayContains(FIELD_GROUP_IDS, groupId)
                .limit(TASK_LIMIT)
                .get(Source.SERVER)
                .await()
        }
        snapshot.documents
            .mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                if (data[FIELD_ACTIVE] == false) return@mapNotNull null
                val due = parentEpochMillis(data[FIELD_DUE_DATE])
                if (due < todayStart) return@mapNotNull null

                ParentTaskBrief(
                    taskId = document.id,
                    subject = data[FIELD_SUBJECT] as? String ?: "",
                    title = data[FIELD_TITLE] as? String ?: "",
                    dueDateMillis = due,
                )
            }
            .sortedBy { it.dueDateMillis }
            .take(TASKS_ON_HOME)
    } catch (_: Exception) {
        null
    }

    private fun countOf(days: List<ParentDayAttendance>): ParentAttendanceCounts {
        var present = 0
        var late = 0
        var absent = 0
        var justified = 0
        days.forEach { day ->
            when (day.mark) {
                AttendanceMark.PRESENT -> present++
                AttendanceMark.LATE -> late++
                AttendanceMark.ABSENT -> absent++
                AttendanceMark.JUSTIFIED -> justified++
                null -> Unit
            }
        }
        return ParentAttendanceCounts(
            present = present,
            late = late,
            absent = absent,
            justified = justified,
        )
    }

    private fun monthPrefix(year: Int, month: Int): String =
        String.format(Locale.US, "%04d-%02d", year, month)

    private data class SessionRow(
        val sessionId: String,
        val date: String,
        val subject: String,
        val subjectId: String,
        val startedAt: Long,
    )

    private data class RecordRow(
        val status: String,
        val time: String?,
        val note: String?,
    )

    /** Per-child outcome of the home load, folded after every child answered. */
    private sealed interface HomeLoad {
        data class Loaded(val overview: ParentChildOverview) : HomeLoad
        data object SignedOut : HomeLoad
        data class Failed(val reason: RosterFailure) : HomeLoad
    }

    private companion object {
        const val STUDENTS = "students"
        const val GROUPS = "groups"
        const val SESSIONS = "attendanceSessions"
        const val RECORDS = "records"
        const val TASKS = "tasks"
        const val NOTICES = "notices"
        const val INCIDENTS = "incidents"
        const val USERS = "users"
        const val NOTICE_READS = "noticeReads"

        /** One cycle of one group, with room to spare. */
        const val SESSION_LIMIT = 200L

        const val TASK_LIMIT = 100L

        /** One group publishes a handful of notices a month; a cycle fits twice. */
        const val NOTICE_LIMIT = 50L

        /** A cycle of incidents for one child; the tutor reads the newest first. */
        const val INCIDENT_LIMIT = 100L

        /** The home card shows two; a third is downloaded so a due task cannot hide. */
        const val TASKS_ON_HOME = 2

        /** Same ceiling as the roster and the teacher's history. */
        const val READ_TIMEOUT_MILLIS = 12_000L
    }
}

/**
 * `students/{id}` as the tutor's list needs it, or `null` when the document does
 * not carry enough to render a child.
 *
 * `active` is deliberately **not** read. The link in `tutorIds` is what makes the
 * child theirs; a student deactivated in the school's roster (`active = false`,
 * logical baja) would otherwise vanish from the family screen and read as "no
 * child linked", which is the opposite of the truth. The teacher's roster keeps
 * filtering by `active` because there it means "enrolled today".
 */
internal fun parentIncidentFrom(
    id: String,
    data: Map<String, Any?>,
    uid: String,
): ParentIncident? {
    val title = (data[FIELD_TITLE] as? String)?.takeIf { it.isNotBlank() } ?: return null

    return ParentIncident(
        incidentId = id,
        studentId = data[FIELD_STUDENT_ID] as? String,
        studentName = data[FIELD_STUDENT_NAME] as? String ?: "",
        groupId = data[FIELD_GROUP_ID] as? String ?: "",
        title = title,
        description = data[FIELD_DESCRIPTION] as? String ?: "",
        severity = data[FIELD_SEVERITY] as? String ?: "",
        category = data[FIELD_CATEGORY] as? String ?: "",
        reportedByName = data[FIELD_REPORTED_BY_NAME] as? String ?: "",
        occurredAtMillis = parentEpochMillis(data[FIELD_OCCURRED_AT]),
        status = data[FIELD_STATUS] as? String ?: "",
        location = parentIncidentLocationFrom(data[FIELD_LOCATION]),
        media = parentIncidentMediaFrom(data[FIELD_MEDIA]),
        seenByMe = uid in parentStringList(data[FIELD_SEEN_BY_TUTOR_IDS]),
    )
}

/**
 * The reported place, or `null` when the document carries no usable coordinate.
 *
 * A location without both numbers is dropped instead of defaulted to the school:
 * a pin on the wrong building is worse than no pin.
 */
internal fun parentIncidentLocationFrom(value: Any?): ParentIncidentLocation? {
    val map = value as? Map<*, *> ?: return null
    val latitude = (map[FIELD_LAT] as? Number)?.toDouble() ?: return null
    val longitude = (map[FIELD_LNG] as? Number)?.toDouble() ?: return null

    return ParentIncidentLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = (map[FIELD_ACCURACY] as? Number)?.toDouble(),
        distanceMeters = (map[FIELD_DISTANCE] as? Number)?.toDouble(),
        verified = map[FIELD_VERIFIED] == true,
    )
}

/** Evidence in the order the teacher uploaded it; a row without a URL is dropped. */
internal fun parentIncidentMediaFrom(value: Any?): List<ParentIncidentMedia> =
    (value as? List<*>)
        ?.mapNotNull { row ->
            val map = row as? Map<*, *> ?: return@mapNotNull null
            val url = (map[FIELD_MEDIA_URL] as? String)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            ParentIncidentMedia(
                mediaId = map[FIELD_MEDIA_ID] as? String,
                type = map[FIELD_MEDIA_TYPE] as? String ?: "",
                url = url,
                thumbnailUrl = (map[FIELD_MEDIA_THUMBNAIL] as? String)?.takeIf { it.isNotBlank() },
                durationSec = (map[FIELD_MEDIA_DURATION] as? Number)?.toDouble(),
            )
        }
        ?: emptyList()

internal fun parentTaskFrom(id: String, data: Map<String, Any?>): ParentTaskItem? {
    val title = (data[FIELD_TITLE] as? String)?.takeIf { it.isNotBlank() } ?: return null

    return ParentTaskItem(
        taskId = id,
        groupIds = parentStringList(data[FIELD_GROUP_IDS]),
        subject = data[FIELD_SUBJECT] as? String ?: "",
        title = title,
        description = data[FIELD_DESCRIPTION] as? String ?: "",
        dueDateMillis = parentEpochMillis(data[FIELD_DUE_DATE]),
        createdAtMillis = parentEpochMillis(data[FIELD_CREATED_AT]),
        createdByName = data[FIELD_CREATED_BY_NAME] as? String ?: "",
        attachments = parentAttachmentsFrom(data[FIELD_ATTACHMENTS]),
    )
}

/**
 * `notices/{noticeId}` as the tutor may see it, or `null` when it is not theirs.
 *
 * Three documents are rejected even though the query returned them: one that is
 * not addressed to `tutor`, one already expired, and one without a title. The
 * role check lives here because the rules cannot require it *and* the group in a
 * single list query (see `docs/firestore.rules` §notices), so the client keeps the
 * audience contract it would otherwise lose.
 */
internal fun parentNoticeFrom(
    id: String,
    data: Map<String, Any?>,
    readIds: Set<String>,
    now: Long,
    groupId: String = "",
): ParentNoticeItem? {
    val audience = data[FIELD_AUDIENCE] as? Map<*, *> ?: return null
    if (ROLE_TUTOR !in parentStringList(audience[FIELD_ROLES])) return null

    val expiresAt = parentEpochMillis(data[FIELD_EXPIRES_AT])
    if (expiresAt > 0 && expiresAt <= now) return null

    val title = (data[FIELD_TITLE] as? String)?.takeIf { it.isNotBlank() } ?: return null

    return ParentNoticeItem(
        noticeId = id,
        title = title,
        body = data[FIELD_BODY] as? String ?: "",
        category = data[FIELD_CATEGORY] as? String ?: "",
        urgent = data[FIELD_URGENT] == true,
        createdByName = data[FIELD_CREATED_BY_NAME] as? String ?: "",
        createdAtMillis = parentEpochMillis(data[FIELD_CREATED_AT]),
        attachments = parentAttachmentsFrom(data[FIELD_ATTACHMENTS]),
        read = id in readIds,
        groupId = groupId,
    )
}

/**
 * The `attachments` array of `CONTEXT.md`: maps of `name`, `url` and `type`.
 *
 * A row without a URL is dropped: it is a file that never reached Cloudinary, and
 * a download button that opens nothing is worse than no button.
 */
internal fun parentAttachmentsFrom(value: Any?): List<ParentAttachment> =
    (value as? List<*>)
        ?.mapNotNull { row ->
            val map = row as? Map<*, *> ?: return@mapNotNull null
            val url = (map[FIELD_ATTACHMENT_URL] as? String)?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            ParentAttachment(
                name = (map[FIELD_ATTACHMENT_NAME] as? String).orEmpty(),
                url = url,
                type = (map[FIELD_ATTACHMENT_TYPE] as? String).orEmpty(),
            )
        }
        ?: emptyList()

/** Firestore hands timestamps back as [Timestamp]; a seeded document may not. */
internal fun parentEpochMillis(value: Any?): Long = when (value) {
    is Timestamp -> value.toDate().time
    is Number -> value.toLong()
    else -> 0L
}

private fun parentStringList(value: Any?): List<String> =
    (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

/** One session's record read: absent, found, or failed for a specific reason. */
internal sealed interface RecordRead<out T> {
    data object Missing : RecordRead<Nothing>
    data class Found<T>(val row: T) : RecordRead<T>
    data class Failed(val reason: RosterFailure) : RecordRead<Nothing>
}

/** The first real read failure, ignoring sessions with no note for this child. */
internal fun <T> firstRecordFailure(results: List<RecordRead<T>>): RosterFailure? =
    results.firstNotNullOfOrNull { (it as? RecordRead.Failed)?.reason }

internal fun parentChildFrom(id: String, data: Map<String, Any?>): ParentChild? {
    val groupId = data[FIELD_GROUP_ID] as? String ?: return null

    val fullName = (data[FIELD_FULL_NAME] as? String)?.takeIf { it.isNotBlank() }
        ?: listOfNotNull(
            data[FIELD_FIRST_NAME] as? String,
            data[FIELD_LAST_NAME] as? String,
        ).joinToString(" ").takeIf { it.isNotBlank() }
        ?: return null

    return ParentChild(
        studentId = id,
        fullName = fullName,
        firstName = (data[FIELD_FIRST_NAME] as? String)?.takeIf { it.isNotBlank() }
            ?: fullName.substringBefore(' '),
        listNumber = (data[FIELD_LIST_NUMBER] as? Number)?.toInt() ?: 0,
        groupId = groupId,
        groupLabel = "",
    )
}

private const val FIELD_TUTOR_IDS = "tutorIds"
private const val FIELD_FULL_NAME = "fullName"
/**
 * Sessions written before the subject filter only carry a display label. Derive
 * the canonical id on read, without rewriting any Firestore document. A label
 * that is not in the catalog keeps an empty id: the tutor still sees it under
 * "Todas", but it never answers a concrete subject filter.
 */
internal fun parentSubjectId(storedId: String, label: String, grade: Int?): String =
    storedId.takeIf { it.isNotBlank() } ?: subjectIdFromLabel(label, grade).orEmpty()

private const val FIELD_FIRST_NAME = "firstName"
private const val FIELD_LAST_NAME = "lastName"
private const val FIELD_LIST_NUMBER = "listNumber"
private const val FIELD_GROUP_ID = "groupId"
private const val FIELD_GROUP_IDS = "groupIds"
private const val FIELD_ACTIVE = "active"
private const val FIELD_NAME = "name"
private const val FIELD_DATE = "date"
private const val FIELD_SUBJECT = "subject"
private const val FIELD_SUBJECT_ID = "subjectId"
private const val FIELD_STARTED_AT = "startedAt"
private const val FIELD_STATUS = "status"
private const val FIELD_TIME = "time"
private const val FIELD_NOTE = "note"
private const val FIELD_TITLE = "title"
private const val FIELD_DESCRIPTION = "description"
private const val FIELD_DUE_DATE = "dueDate"
private const val FIELD_CREATED_AT = "createdAt"
private const val FIELD_CREATED_BY_NAME = "createdByName"
private const val FIELD_ATTACHMENTS = "attachments"
private const val FIELD_ATTACHMENT_NAME = "name"
private const val FIELD_ATTACHMENT_URL = "url"
private const val FIELD_ATTACHMENT_TYPE = "type"
private const val FIELD_AUDIENCE = "audience"
private const val FIELD_AUDIENCE_GROUP_IDS = "audience.groupIds"
private const val FIELD_ROLES = "roles"
private const val FIELD_BODY = "body"
private const val FIELD_CATEGORY = "category"
private const val FIELD_URGENT = "urgent"
private const val FIELD_EXPIRES_AT = "expiresAt"
private const val FIELD_READ_AT = "readAt"
private const val FIELD_STUDENT_ID = "studentId"
private const val FIELD_STUDENT_NAME = "studentName"
private const val FIELD_SEVERITY = "severity"
private const val FIELD_REPORTED_BY_NAME = "reportedByName"
private const val FIELD_OCCURRED_AT = "occurredAt"
private const val FIELD_SEEN_BY_TUTOR_IDS = "seenByTutorIds"
private const val FIELD_LOCATION = "location"
private const val FIELD_LAT = "lat"
private const val FIELD_LNG = "lng"
private const val FIELD_ACCURACY = "accuracy"
private const val FIELD_DISTANCE = "distanceMeters"
private const val FIELD_VERIFIED = "verified"
private const val FIELD_MEDIA = "media"
private const val FIELD_MEDIA_ID = "id"
private const val FIELD_MEDIA_TYPE = "type"
private const val FIELD_MEDIA_URL = "url"
private const val FIELD_MEDIA_THUMBNAIL = "thumbnailUrl"
private const val FIELD_MEDIA_DURATION = "durationSec"
private const val ROLE_TUTOR = "tutor"
