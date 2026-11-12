package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.entity.OutboxEntity

/**
 * Drains the outbox once, in order, and reports whether the queue needs another
 * pass.
 *
 * This is the whole sync policy, kept out of [SyncWorker] so it can be tested
 * against an in-memory database without WorkManager and without the network.
 *
 * Policy:
 * - One row at a time, oldest first, so two attendance sessions of the same
 *   group never arrive out of order.
 * - [SyncOutcome.Success] deletes the row.
 * - [SyncOutcome.Retry] stops the pass and asks WorkManager for its exponential
 *   backoff; the rows stay queued.
 * - [SyncOutcome.Permanent], an unknown `opType` or an unregistered operation
 *   keeps the row, records why, and continues with the next one, so one broken
 *   entry cannot block a teacher's whole queue forever.
 * - A pass reads at most [maxEntriesPerPass] rows; hitting the cap asks for
 *   another pass instead of looping inside a single worker run.
 */
class SyncRunner(
    private val outboxDao: OutboxDao,
    private val registry: SyncHandlerRegistry,
    private val reporter: SyncFailureReporter,
    private val maxEntriesPerPass: Int = DEFAULT_MAX_ENTRIES_PER_PASS,
) {

    suspend fun run(): SyncRunResult {
        val entries = outboxDao.peek(maxEntriesPerPass)
        if (entries.isEmpty()) return SyncRunResult.Done(processed = 0)

        var processed = 0
        for (entry in entries) {
            when (val outcome = execute(entry)) {
                is SyncOutcome.Success -> {
                    outboxDao.deleteById(entry.id)
                    processed++
                }

                is SyncOutcome.Retry -> {
                    outboxDao.markAttempt(entry.id, outcome.message)
                    reporter.record(entry, outcome.message, outcome.cause)
                    return SyncRunResult.RetryLater(processed, outcome.message)
                }

                is SyncOutcome.Permanent -> {
                    reporter.record(entry, outcome.message, outcome.cause)
                    // A poison row is retried, but not forever: after
                    // MAX_ATTEMPTS_BEFORE_DROP passes it leaves the queue and the
                    // entity stays in Room marked FAILED by its handler. Keeping
                    // it would block the queue behind an operation that can never
                    // succeed.
                    if (entry.attempts + 1 >= MAX_ATTEMPTS_BEFORE_DROP) {
                        outboxDao.deleteById(entry.id)
                    } else {
                        outboxDao.markAttempt(entry.id, outcome.message)
                    }
                    processed++
                }
            }
        }

        return if (entries.size >= maxEntriesPerPass) {
            SyncRunResult.RetryLater(processed, "pass cap reached")
        } else {
            SyncRunResult.Done(processed)
        }
    }

    private suspend fun execute(entry: OutboxEntity): SyncOutcome {
        val operation = SyncOperation.fromName(entry.opType)
            ?: return SyncOutcome.Permanent("Unknown outbox opType '${entry.opType}'")

        val handler = registry.handlerFor(operation)
            ?: return SyncOutcome.Permanent("No handler registered for $operation")

        return try {
            handler.handle(entry)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (unexpected: Exception) {
            // A handler bug is a bug, not a reason to lose the teacher's session:
            // retry later and surface it as a non-fatal.
            SyncOutcome.Retry(
                message = unexpected.message ?: unexpected::class.java.simpleName,
                cause = unexpected,
            )
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES_PER_PASS = 100

        /** After this many permanent failures the outbox row stops blocking the queue. */
        const val MAX_ATTEMPTS_BEFORE_DROP = 10
    }
}

/** Result of one outbox pass. */
sealed interface SyncRunResult {

    /** Nothing left (or nothing actionable) in this pass. */
    data class Done(val processed: Int) : SyncRunResult

    /** Rows remain; the caller should schedule another pass with backoff. */
    data class RetryLater(val processed: Int, val reason: String) : SyncRunResult
}
