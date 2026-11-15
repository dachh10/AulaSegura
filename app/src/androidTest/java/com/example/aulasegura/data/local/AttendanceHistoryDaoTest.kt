package com.example.aulasegura.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The history list is one query, and it is the one that has to be right offline:
 * `observeSessionsByTeacher` is what the teacher sees in airplane mode, ordered by
 * `date` so the day sections stay in order.
 *
 * Uses an in-memory database, same as [AulaSeguraDatabaseTest], so it never
 * touches the device's real `aulasegura.db`.
 */
@RunWith(AndroidJUnit4::class)
class AttendanceHistoryDaoTest {

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
    fun historyShowsOnlyTheTeachersSessionsNewestFirst() = runBlocking {
        val dao = database.attendanceDao()
        dao.upsertSessions(
            listOf(
                session("ses-vieja", teacherId = "uid-docente", date = "2026-11-10", startedAt = 1L),
                session("ses-nueva", teacherId = "uid-docente", date = "2026-11-15", startedAt = 2L),
                // Another teacher's session must never leak into this history.
                session("ses-ajena", teacherId = "uid-otro", date = "2026-11-14", startedAt = 3L),
            ),
        )

        val history = dao.observeSessionsByTeacher("uid-docente").first()

        assertEquals(listOf("ses-nueva", "ses-vieja"), history.map { it.sessionId })
    }

    @Test
    fun twoSessionsOfTheSameDayStayOrderedByStartTime() = runBlocking {
        val dao = database.attendanceDao()
        dao.upsertSessions(
            listOf(
                session("ses-manana", teacherId = "uid-docente", date = "2026-11-15", startedAt = 1L),
                session("ses-tarde", teacherId = "uid-docente", date = "2026-11-15", startedAt = 9L),
            ),
        )

        val history = dao.observeSessionsByTeacher("uid-docente").first()

        assertEquals(listOf("ses-tarde", "ses-manana"), history.map { it.sessionId })
    }

    private fun session(
        sessionId: String,
        teacherId: String,
        date: String,
        startedAt: Long,
    ) = AttendanceSessionEntity(
        sessionId = sessionId,
        groupId = "grp-3b",
        schoolId = "sch-benito-juarez",
        teacherId = teacherId,
        subject = "Matemáticas",
        date = date,
        startedAt = startedAt,
        closedAt = startedAt + 600_000,
        status = "closed",
        presentCount = 28,
        syncState = SyncState.SYNCED,
        createdAt = startedAt,
        updatedAt = startedAt,
    )
}
