package com.example.aulasegura.sync

import com.example.aulasegura.data.local.entity.OutboxEntity

/**
 * Reports a failed sync operation to Crashlytics without ever throwing.
 *
 * Sync failures are non-fatal by definition: the data is still in Room. The
 * reporter is an interface so the tests can assert what was reported instead of
 * standing up Firebase.
 */
fun interface SyncFailureReporter {
    fun recordFailure(
        operation: String,
        entityLocalId: String,
        message: String,
        cause: Throwable?,
    )
}

/**
 * Production reporter.
 *
 * Wrapped in `runCatching` on purpose: telemetry must never turn a recoverable
 * sync failure into a crash loop.
 */
object CrashlyticsSyncReporter : SyncFailureReporter {

    override fun recordFailure(
        operation: String,
        entityLocalId: String,
        message: String,
        cause: Throwable?,
    ) {
        runCatching {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("sync_operation", operation)
            crashlytics.setCustomKey("sync_entity", entityLocalId)
            crashlytics.recordException(SyncFailureException(operation, message, cause))
        }
    }

    /** Marker type so these entries are filterable in the Crashlytics dashboard. */
    class SyncFailureException(
        operation: String,
        message: String,
        cause: Throwable?,
    ) : Exception("Sync failed for $operation: $message", cause)
}

/** Reporter used by tests and by any run where telemetry is irrelevant. */
object NoOpSyncFailureReporter : SyncFailureReporter {
    override fun recordFailure(
        operation: String,
        entityLocalId: String,
        message: String,
        cause: Throwable?,
    ) = Unit
}

/** Convenience for the worker: the reporter signature plus the entity it came from. */
internal fun SyncFailureReporter.record(entry: OutboxEntity, message: String, cause: Throwable?) {
    recordFailure(entry.opType, entry.entityLocalId, message, cause)
}
