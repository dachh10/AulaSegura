package com.example.aulasegura.sync

import com.example.aulasegura.data.local.entity.OutboxEntity

/**
 * What a handler tells the worker to do with one outbox entry.
 *
 * The split matters because WorkManager's backoff is per worker, not per row:
 * a transient failure stops the run so the whole queue is retried later, while
 * a permanent failure only buries that row and lets the rest continue.
 */
sealed interface SyncOutcome {

    /** The write reached its destination; the row can be deleted. */
    data object Success : SyncOutcome

    /** Transient: network, timeout, unavailable service. Retry the queue later. */
    data class Retry(val message: String, val cause: Throwable? = null) : SyncOutcome

    /** The row can never succeed as written; keep it, record why, move on. */
    data class Permanent(val message: String, val cause: Throwable? = null) : SyncOutcome
}

/**
 * Executes one queued operation.
 *
 * A handler is allowed to throw: the worker converts an unexpected exception
 * into [SyncOutcome.Retry] so a bug in one write never drops the queue.
 */
fun interface SyncOperationHandler {
    suspend fun handle(entry: OutboxEntity): SyncOutcome
}

/**
 * Maps an operation to its handler.
 *
 * Registration is explicit because an operation without a handler is a design
 * decision, not an accident: Fase 0 registers attendance only, and the media
 * and incident operations arrive with their repositories (Cloudinary upload is
 * Bloque H).
 */
class SyncHandlerRegistry(
    private val handlers: Map<SyncOperation, SyncOperationHandler>,
) {
    fun handlerFor(operation: SyncOperation?): SyncOperationHandler? =
        operation?.let(handlers::get)
}
