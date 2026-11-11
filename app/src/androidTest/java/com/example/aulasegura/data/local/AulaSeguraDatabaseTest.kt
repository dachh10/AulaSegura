package com.example.aulasegura.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.GroupEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the Room layer, run against an in-memory database
 * so they never touch the device's real `aulasegura.db`.
 *
 * Covers the three things that can silently break the cache: a plain insert and
 * read, the `List<String>` / enum converters, and the outbox queue order.
 */
@RunWith(AndroidJUnit4::class)
class AulaSeguraDatabaseTest {

    private lateinit var database: AulaSeguraDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AulaSeguraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndReadsGroup() = runBlocking {
        val group = group(groupId = "grp-3b", name = "3° B", grade = 3, section = "B")

        database.groupDao().upsert(group)

        val stored = database.groupDao().findById("grp-3b")
        assertEquals("3° B", stored?.name)
        assertEquals(3, stored?.grade)

        val bySchool = database.groupDao().observeBySchool("sch-benito-juarez").first()
        assertEquals(1, bySchool.size)
    }

    @Test
    fun roundTripsConvertedListsAndSyncState() = runBlocking {
        val group = group(
            groupId = "grp-4a",
            name = "4° A",
            grade = 4,
            section = "A",
            teacherIds = listOf("uid-docente", "uid-titular"),
        )

        database.groupDao().upsert(group)

        val stored = database.groupDao().findById("grp-4a")
        assertEquals(listOf("uid-docente", "uid-titular"), stored?.teacherIds)

        val session = session(sessionId = "ses-1", groupId = "grp-4a")
        database.attendanceDao().upsertSession(session.copy(syncState = SyncState.PENDING))

        val pending = database.attendanceDao().sessionsWithState(SyncState.PENDING)
        assertEquals(1, pending.size)
        assertEquals(SyncState.PENDING, pending.first().syncState)

        val synced = database.attendanceDao().sessionsWithState(SyncState.SYNCED)
        assertTrue(synced.isEmpty())
    }

    @Test
    fun keepsOneRecordPerStudentAndUpdatesIt() = runBlocking {
        database.attendanceDao().upsertSession(session(sessionId = "ses-2", groupId = "grp-4a"))

        database.attendanceDao().upsertRecord(record("ses-2", "stu-1", status = "absent"))
        database.attendanceDao().upsertRecord(record("ses-2", "stu-1", status = "present"))
        database.attendanceDao().upsertRecord(record("ses-2", "stu-2", status = "late"))

        val records = database.attendanceDao().records("ses-2")
        assertEquals(2, records.size)
        assertEquals("present", records.first { it.studentId == "stu-1" }.status)
    }

    @Test
    fun outboxKeepsInsertionOrderAndRecordsAttempts() = runBlocking {
        val outbox = database.outboxDao()
        outbox.insert(outboxEntry(entityLocalId = "ses-1", createdAt = 1_000L))
        outbox.insert(outboxEntry(entityLocalId = "inc-1", createdAt = 2_000L))

        assertEquals(2, outbox.count())
        val queued = outbox.peek(10)
        assertEquals(listOf("ses-1", "inc-1"), queued.map { it.entityLocalId })

        outbox.markAttempt(queued.first().id, "network unavailable")
        val retried = outbox.peek(1).first()
        assertEquals(1, retried.attempts)
        assertEquals("network unavailable", retried.lastError)

        outbox.deleteById(retried.id)
        assertEquals(1, outbox.count())
        assertNull(outbox.peek(1).first().lastError)
        assertEquals(0, outbox.peek(1).first().attempts)
    }

    private fun group(
        groupId: String,
        name: String,
        grade: Int,
        section: String,
        teacherIds: List<String> = listOf("uid-docente"),
    ) = GroupEntity(
        groupId = groupId,
        name = name,
        grade = grade,
        section = section,
        schoolId = "sch-benito-juarez",
        titularTeacherId = "uid-docente",
        teacherIds = teacherIds,
        studentCount = 0,
        cycle = "2026-2027",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun session(sessionId: String, groupId: String) = AttendanceSessionEntity(
        sessionId = sessionId,
        groupId = groupId,
        schoolId = "sch-benito-juarez",
        teacherId = "uid-docente",
        subject = "Matemáticas",
        date = "2026-11-03",
        startedAt = 1_700_000_000_000L,
        status = "open",
        syncState = SyncState.SYNCED,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun record(sessionId: String, studentId: String, status: String) = AttendanceRecordEntity(
        sessionId = sessionId,
        studentId = studentId,
        listNumber = 1,
        status = status,
        time = "08:12",
        updatedAt = 1_700_000_000_000L,
        updatedBy = "uid-docente",
        syncState = SyncState.PENDING,
    )

    private fun outboxEntry(entityLocalId: String, createdAt: Long) = OutboxEntity(
        opType = "UPSERT_ATTENDANCE_SESSION",
        entityLocalId = entityLocalId,
        payloadJson = "{}",
        createdAt = createdAt,
    )
}
