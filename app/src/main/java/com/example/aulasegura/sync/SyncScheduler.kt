package com.example.aulasegura.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Who gets told that there is work to do. Injected so the queue can be tested. */
fun interface SyncScheduling {
    fun schedule()
}

/**
 * Enqueues the single sync worker.
 *
 * `enqueueUniqueWork(KEEP)` is what makes "no duplicates" hold: ten taps while
 * offline enqueue ten outbox rows but only one pending worker, so the queue is
 * drained once, in order. The work also requires `NetworkType.CONNECTED`, which
 * is exactly the offline behaviour the block asks for: nothing runs in airplane
 * mode, and WorkManager fires it as soon as connectivity returns.
 */
class SyncScheduler(private val context: Context) : SyncScheduling {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                MIN_BACKOFF_SECONDS,
                TimeUnit.SECONDS,
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "aulasegura-sync"
        const val TAG = "aulasegura-sync"

        /**
         * WorkManager's floor is 10 s. 30 s keeps a flaky school connection from
         * burning battery with a retry storm while still recovering quickly.
         */
        const val MIN_BACKOFF_SECONDS = 30L
    }
}
