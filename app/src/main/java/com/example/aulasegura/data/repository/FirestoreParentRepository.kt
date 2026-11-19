package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentTaskBrief
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
            val data = document.data ?: return@mapNotNull null
            if (data[FIELD_ACTIVE] == false) return@mapNotNull null

            val fullName = (data[FIELD_FULL_NAME] as? String)
                ?: listOfNotNull(
                    data[FIELD_FIRST_NAME] as? String,
                    data[FIELD_LAST_NAME] as? String,
                ).joinToString(" ").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            ParentChild(
                studentId = document.id,
                fullName = fullName,
                firstName = (data[FIELD_FIRST_NAME] as? String)
                    ?: fullName.substringBefore(' '),
                listNumber = (data[FIELD_LIST_NUMBER] as? Number)?.toInt() ?: 0,
                groupId = data[FIELD_GROUP_ID] as? String ?: return@mapNotNull null,
                groupLabel = "",
            )
        }

        val labels = rows.map { it.groupId }.distinct().associateWith { groupLabel(it) }

        return ParentChildrenResult.Loaded(
            rows
                .map { it.copy(groupLabel = labels[it.groupId] ?: it.groupId) }
                .sortedWith(compareBy({ it.listNumber }, { it.fullName })),
        )
    }

    override suspend fun loadMonth(
        child: ParentChild,
        year: Int,
        month: Int,
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

            SessionRow(
                sessionId = document.id,
                date = date,
                subject = data[FIELD_SUBJECT] as? String ?: "",
                startedAt = epochMillis(data[FIELD_STARTED_AT]),
            )
        }

        // One document read per session: `records/{studentId}` is what the tutor
        // is allowed to see, and the record of one child is exactly one document.
        val records = coroutineScope {
            sessions.map { session ->
                async(Dispatchers.IO) { session to readRecord(session.sessionId, child.studentId) }
            }.awaitAll()
        }

        if (sessions.isNotEmpty() && records.all { it.second == null }) {
            // Every record failed: that is a read problem, not "the month is empty".
            return ParentMonthResult.Failed(RosterFailure.UNKNOWN)
        }

        val days = records
            .mapNotNull { (session, record) -> record?.let { session to it } }
            .map { (session, record) ->
                ParentDayAttendance(
                    date = session.date,
                    subject = session.subject,
                    startedAt = session.startedAt,
                    mark = AttendanceMark.fromWire(record.status),
                    time = record.time,
                    note = record.note,
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

    override suspend fun loadHome(): ParentHomeResult {
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
                    when (val monthResult = loadMonth(child, year, month)) {
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

    private suspend fun readRecord(sessionId: String, studentId: String): RecordRow? = try {
        val document = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(SESSIONS)
                .document(sessionId)
                .collection(RECORDS)
                .document(studentId)
                .get(Source.SERVER)
                .await()
        }
        if (!document.exists()) {
            null
        } else {
            RecordRow(
                status = document.getString(FIELD_STATUS).orEmpty(),
                time = document.getString(FIELD_TIME),
                note = document.getString(FIELD_NOTE),
            )
        }
    } catch (_: Exception) {
        null
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
                val due = epochMillis(data[FIELD_DUE_DATE])
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

    private fun epochMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0L
    }

    private data class SessionRow(
        val sessionId: String,
        val date: String,
        val subject: String,
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

        const val FIELD_TUTOR_IDS = "tutorIds"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_FIRST_NAME = "firstName"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_LIST_NUMBER = "listNumber"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_GROUP_IDS = "groupIds"
        const val FIELD_ACTIVE = "active"
        const val FIELD_NAME = "name"
        const val FIELD_DATE = "date"
        const val FIELD_SUBJECT = "subject"
        const val FIELD_STARTED_AT = "startedAt"
        const val FIELD_STATUS = "status"
        const val FIELD_TIME = "time"
        const val FIELD_NOTE = "note"
        const val FIELD_TITLE = "title"
        const val FIELD_DUE_DATE = "dueDate"

        /** One cycle of one group, with room to spare. */
        const val SESSION_LIMIT = 200L

        const val TASK_LIMIT = 100L

        /** The home card shows two; a third is downloaded so a due task cannot hide. */
        const val TASKS_ON_HOME = 2

        /** Same ceiling as the roster and the teacher's history. */
        const val READ_TIMEOUT_MILLIS = 12_000L
    }
}
