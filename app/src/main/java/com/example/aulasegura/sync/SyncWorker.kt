package com.example.aulasegura.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aulasegura.data.local.DatabaseProvider
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Wires the real dependencies into [SyncRunner]. Kept thin on purpose: all the
 * policy lives in the runner, which is tested without WorkManager.
 *
 * Bloque G replaces this locator with the DI container.
 */
object SyncServiceLocator {

    fun registry(context: Context): SyncHandlerRegistry {
        val database = DatabaseProvider.get(context)

        // Fase 0 registers the attendance flow only. Incident, justification and
        // media uploads arrive with their repositories (Cloudinary is Bloque H);
        // until then an unregistered opType stays in the outbox and is reported,
        // instead of pretending to have synced.
        return SyncHandlerRegistry(
            mapOf(
                SyncOperation.UPSERT_ATTENDANCE_SESSION to AttendanceSyncHandler(
                    attendanceDao = database.attendanceDao(),
                    firestore = FirebaseFirestore.getInstance(),
                ),
            ),
        )
    }
}

/**
 * Drains the outbox whenever WorkManager decides the device is online.
 *
 * A transient failure returns `retry()`, so WorkManager applies the exponential
 * backoff configured by [SyncScheduler]; a permanent failure is recorded as a
 * Crashlytics non-fatal (Bloque F5) and does not stall the rest of the queue.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = DatabaseProvider.get(applicationContext)
        val runner = SyncRunner(
            outboxDao = database.outboxDao(),
            registry = SyncServiceLocator.registry(applicationContext),
            reporter = CrashlyticsSyncReporter,
        )

        return when (runner.run()) {
            is SyncRunResult.Done -> Result.success()
            is SyncRunResult.RetryLater -> Result.retry()
        }
    }
}
