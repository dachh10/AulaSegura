package com.example.aulasegura.sync

import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Whether the outbox should keep this row for a later pass.
 *
 * These are the codes where the write may succeed on its own later: the network,
 * the backend, a transaction conflict. Everything else — `PERMISSION_DENIED`,
 * `INVALID_ARGUMENT`, `NOT_FOUND` — is a bug or a rules mismatch that a retry
 * cannot fix, and retrying it forever would only hide it.
 */
val FirebaseFirestoreException.isRetryable: Boolean
    get() = code in RETRYABLE_CODES

private val RETRYABLE_CODES = setOf(
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    FirebaseFirestoreException.Code.ABORTED,
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
    FirebaseFirestoreException.Code.INTERNAL,
    FirebaseFirestoreException.Code.CANCELLED,
    FirebaseFirestoreException.Code.UNKNOWN,
)
