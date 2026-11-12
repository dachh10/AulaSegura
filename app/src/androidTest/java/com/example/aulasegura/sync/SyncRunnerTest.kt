package com.example.aulasegura.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.entity.OutboxEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the queue policy against a real (in-memory) database and a fake
 * handler set: order, deletion on success, backoff on retry, and a poison row
 * that must not block the rest of a teacher's queue.
 */
@RunWith(AndroidJUnit4::class)
class SyncRunnerTest {

    private lateinit var database: AulaSeguraDatabase
    private lateinit var reporter: RecordingReporter
    private lateinit var handled: MutableList<String>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AulaSeguraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reporter = RecordingReporter()
        handled = mutableListOf()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun drainsTheQueueOldestFirstAndDeletesEveryRow() = runBlocking {
        queue("ses-1", createdAt = 1_000L)
        queue("ses-2", createdAt = 2_000L)
        queue("ses-3", createdAt = 3_000L)

        val result = runner { SyncOutcome.Success }.run()

        assertEquals(SyncRunResult.Done(processed = 3), result)
        assertEquals(listOf("ses-1", "ses-2", "ses-3"), handled)
        assertEquals(0, database.outboxDao().count())
    }

    @Test
    fun retryStopsThePassAndKeepsTheRowsQueued() = runBlocking {
        queue("ses-1", createdAt = 1_000L)
        queue("ses-2", createdAt = 2_000L)

        val result = runner { SyncOutcome.Retry("offline") }.run()

        assertEquals(SyncRunResult.RetryLater(processed = 0, reason = "offline"), result)
        assertEquals(listOf("ses-1"), handled)
        assertEquals(2, database.outboxDao().count())

        val first = database.outboxDao().peek(1).first()
        assertEquals(1, first.attempts)
        assertEquals("offline", first.lastError)
        assertEquals(listOf("ses-1"), reporter.reported.map { it.entityLocalId })
    }

    @Test
    fun handlerCrashBecomesARetryInsteadOfLosingTheRow() = runBlocking {
        queue("ses-1", createdAt = 1_000L)
        val registry = SyncHandlerRegistry(
            mapOf(
                SyncOperation.UPSERT_ATTENDANCE_SESSION to SyncOperationHandler {
                    error("boom")
                },
            ),
        )

        val result = SyncRunner(database.outboxDao(), registry, reporter).run()

        assertTrue(result is SyncRunResult.RetryLater)
        assertEquals(1, database.outboxDao().count())
        assertEquals(1, database.outboxDao().peek(1).first().attempts)
    }

    @Test
    fun unknownOperationIsReportedAndDoesNotBlockTheNextEntry() = runBlocking {
        database.outboxDao().insert(
            OutboxEntity(opType = "MYSTERY_OP", entityLocalId = "weird", payloadJson = "{}", createdAt = 1_000L),
        )
        queue("ses-1", createdAt = 2_000L)

        val result = runner { SyncOutcome.Success }.run()

        assertEquals(SyncRunResult.Done(processed = 2), result)
        assertEquals(listOf("ses-1"), handled)
        assertEquals(1, database.outboxDao().count())
        assertTrue(reporter.reported.any { it.message.contains("Unknown outbox opType") })
    }

    @Test
    fun poisonRowLeavesTheQueueAfterMaxAttempts() = runBlocking {
        database.outboxDao().insert(
            OutboxEntity(opType = "MYSTERY_OP", entityLocalId = "weird", payloadJson = "{}", createdAt = 1_000L),
        )
        val runner = runner { SyncOutcome.Success }

        repeat(SyncRunner.MAX_ATTEMPTS_BEFORE_DROP - 1) {
            runner.run()
            assertEquals("row must survive until the drop threshold", 1, database.outboxDao().count())
        }

        runner.run()

        assertEquals(0, database.outboxDao().count())
        assertEquals(
            SyncRunner.MAX_ATTEMPTS_BEFORE_DROP,
            reporter.reported.count { it.entityLocalId == "weird" },
        )
    }

    private suspend fun queue(entityLocalId: String, createdAt: Long) {
        database.outboxDao().insert(
            OutboxEntity(
                opType = SyncOperation.UPSERT_ATTENDANCE_SESSION.name,
                entityLocalId = entityLocalId,
                payloadJson = "{}",
                createdAt = createdAt,
            ),
        )
    }

    private fun runner(outcome: (OutboxEntity) -> SyncOutcome): SyncRunner {
        val handler = SyncOperationHandler { entry ->
            handled += entry.entityLocalId
            outcome(entry)
        }
        return SyncRunner(
            outboxDao = database.outboxDao(),
            registry = SyncHandlerRegistry(
                mapOf(SyncOperation.UPSERT_ATTENDANCE_SESSION to handler),
            ),
            reporter = reporter,
        )
    }

    private class RecordingReporter : SyncFailureReporter {
        data class Report(val operation: String, val entityLocalId: String, val message: String)

        val reported = mutableListOf<Report>()

        override fun recordFailure(
            operation: String,
            entityLocalId: String,
            message: String,
            cause: Throwable?,
        ) {
            reported += Report(operation, entityLocalId, message)
        }
    }
}
