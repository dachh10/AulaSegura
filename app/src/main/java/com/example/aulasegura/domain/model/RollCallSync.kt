package com.example.aulasegura.domain.model

/**
 * Whether the session the teacher just closed has already reached Firestore.
 *
 * This is not the same question as "did the save work". The save always works —
 * it is a local transaction — so a teacher in an airplane gets the exact same
 * confirmation as one online. What this enum answers is the second question:
 * has the cloud seen it yet?
 *
 * Kept in `domain` instead of reusing `data.local.entity.SyncState` so the UI
 * never imports Room (`CONTEXT.md` §6, regla de dependencia). The mapping happens
 * once, in the repository that owns the cache.
 *
 * `SYNCING` is written by the sync handler before the batch commits, not derived
 * from a timer: a phase the app never sets would be a lie painted in green.
 */
enum class RollCallSync {
    /** Written locally and queued in the outbox; the worker has not picked it up. */
    PENDING,

    /** The worker is committing the batch to Firestore right now. */
    SYNCING,

    /** Firestore accepted the session and all of its records. */
    SYNCED,

    /** The last attempt failed; the row stays queued for the next pass. */
    FAILED,
    ;

    /** True only when the cloud has the session: pending is not delivered. */
    val delivered: Boolean get() = this == SYNCED
}
