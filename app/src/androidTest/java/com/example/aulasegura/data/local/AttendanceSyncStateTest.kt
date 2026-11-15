package com.example.aulasegura.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.SyncState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The roll call screen paints the sync phase of the session it just closed, so
 * that query is now part of the offline story and not a cosmetic detail.
 *
 * Runs on the device against an in-memory database: `observeSession` is the only
 * place where a bad column name or a broken enum converter would crash at runtime
 * instead of at compile time, and the screen would show a save confirmation with
 * no way to know whether the session ever left the phone.
 */
@RunWith(AndroidJUnit4::class)
class AttendanceSyncStateTest {

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
    fun everySyncPhaseSurvivesTheObservableQuery() = runBlocking {
        val dao = database.attendanceDao()
        dao.upsertSession(session().copy(syncState = SyncState.PENDING))

        assertEquals(SyncState.PENDING, dao.observeSession(SESSION_ID).awaitState())

        // The handler writes SYNCING before the batch and SYNCED after Firestore
        // accepts it; both have to be readable, not just the endpoints.
        dao.upsertSession(session().copy(syncState = SyncState.SYNCING))
        assertEquals(SyncState.SYNCING, dao.observeSession(SESSION_ID).awaitState())

        dao.upsertSession(session().copy(syncState = SyncState.SYNCED))
        assertEquals(SyncState.SYNCED, dao.observeSession(SESSION_ID).awaitState())

        // A retryable failure leaves the row queued and the teacher informed.
        dao.upsertSession(session().copy(syncState = SyncState.FAILED))
        assertEquals(SyncState.FAILED, dao.observeSession(SESSION_ID).awaitState())
    }

    @Test
    fun aSessionThatWasNeverSavedEmitsNothing() = runBlocking {
        val state = withTimeoutOrNull(NO_EMISSION_WINDOW_MILLIS) {
            database.attendanceDao().observeSession("ses-que-no-existe").awaitState()
        }

        assertNull(state)
    }

    private suspend fun kotlinx.coroutines.flow.Flow<AttendanceSessionEntity?>.awaitState(): SyncState =
        filterNotNull().first().syncState

    private fun session() = AttendanceSessionEntity(
        sessionId = SESSION_ID,
        groupId = "grp-3b",
        schoolId = "sch-benito-juarez",
        teacherId = "uid-docente",
        subject = "Matemáticas",
        date = "2026-11-15",
        startedAt = 1_762_000_000_000L,
        closedAt = 1_762_000_600_000L,
        status = "closed",
        syncState = SyncState.PENDING,
        createdAt = 1_762_000_000_000L,
        updatedAt = 1_762_000_600_000L,
    )

    private companion object {
        const val SESSION_ID = "ses-offline-1"
        const val NO_EMISSION_WINDOW_MILLIS = 800L
    }
}
