package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.JustificationDraft

/**
 * Write side of the tutor's justification: the one flow where a family creates
 * something instead of reading (`CONTEXT.md` §8.4).
 *
 * Deliberately offline-first and queue-backed: the screen promises that a report
 * sent without signal is kept on the phone, and [submit] returns as soon as the
 * file and the queue row are on disk. Nothing here talks to Firestore; the sync
 * worker owns the network.
 */
interface JustificationRepository {

    suspend fun submit(draft: JustificationDraft): JustificationResult
}

sealed interface JustificationResult {

    /** Stored locally and queued; it reaches the school even without a network. */
    data object Queued : JustificationResult

    data object SignedOut : JustificationResult

    /** The receipt could not be written to private storage, so nothing was queued. */
    data object StorageFailed : JustificationResult
}
