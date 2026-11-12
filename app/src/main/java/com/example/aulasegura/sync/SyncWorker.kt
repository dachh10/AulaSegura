package com.example.aulasegura.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aulasegura.AulaSeguraApp

/**
 * Drains the outbox whenever WorkManager decides the device is online.
 *
 * Dependencies come from the application container, which is what keeps the
 * worker free of `DatabaseProvider` and Firebase singletons. A transient failure
 * returns `retry()`, so WorkManager applies the exponential backoff configured by
 * [SyncScheduler]; a permanent failure is recorded as a Crashlytics non-fatal and
 * does not stall the rest of the queue.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AulaSeguraApp).container
        val runner = SyncRunner(
            outboxDao = container.database.outboxDao(),
            registry = container.syncHandlers,
            reporter = container.syncFailureReporter,
        )

        return when (runner.run()) {
            is SyncRunResult.Done -> Result.success()
            is SyncRunResult.RetryLater -> Result.retry()
        }
    }
}
