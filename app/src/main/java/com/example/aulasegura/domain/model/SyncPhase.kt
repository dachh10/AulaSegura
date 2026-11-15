package com.example.aulasegura.domain.model

/**
 * Where a locally written row stands on its way to Firestore.
 *
 * A domain enum, not `data.local.entity.SyncState`: the UI must not import Room
 * (`CONTEXT.md` §6). The mapping happens once, in the mappers.
 *
 * `SYNCING` is written by the sync handler before the network call, never derived
 * from a timer — a phase the app never sets would be a lie painted in green.
 */
enum class SyncPhase {
    /** Written locally and queued in the outbox; the worker has not picked it up. */
    PENDING,

    /** The worker is committing the write to Firestore right now. */
    SYNCING,

    /** Firestore accepted the write. */
    SYNCED,

    /** The last attempt failed; the row stays queued for the next pass. */
    FAILED,
    ;

    /** True only when the cloud has it: pending is not delivered. */
    val delivered: Boolean get() = this == SYNCED
}
