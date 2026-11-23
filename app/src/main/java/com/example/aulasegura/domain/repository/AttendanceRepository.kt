package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.model.RollCallEntry
import com.example.aulasegura.domain.model.RollCallSync
import kotlinx.coroutines.flow.Flow

/**
 * Write side of the roll call.
 *
 * There is no read side yet: the screen takes attendance from the roster cache
 * and the history screen is a later block. Keeping the contract this small means
 * the screen cannot accidentally read a session from Firestore while it is
 * supposed to be writing one.
 *
 * Every write goes through Room plus the outbox, never straight to Firestore.
 * That is what lets a teacher close the roll call in airplane mode and still
 * have it reach the cloud once (and only once) later.
 */
interface AttendanceRepository {

    /**
     * Resolves everything a new session needs before the teacher starts marking:
     * the group, the signed-in teacher and the client-side UUID.
     *
     * Nothing is persisted here. A session only exists once it is saved, so
     * backing out of the screen leaves no half-open session in Firestore.
     */
    suspend fun startDraft(groupId: String, subjectId: String): AttendanceStart

    /**
     * Persists [entries] as the closed session [draft], locally and queued.
     *
     * The session and its records are handed to the outbox in one transaction,
     * so a `PENDING` session is always backed by a queued row.
     */
    suspend fun save(draft: RollCallDraft, entries: List<RollCallEntry>): SaveRollCallOutcome

    /**
     * Follows one saved session while it waits for the cloud.
     *
     * The screen needs the real phase, not a promise in a string: an offline save
     * that stays `PENDING` for hours and one that Firestore rejected look identical
     * from the teacher's chair unless the UI says which one happened.
     *
     * The flow is backed by the local cache, so it keeps emitting with no network.
     */
    fun observeSync(sessionId: String): Flow<RollCallSync>
}

/** Outcome of resolving a new session. */
sealed interface AttendanceStart {

    data class Ready(val draft: RollCallDraft) : AttendanceStart

    /** Nobody is signed in; the screen is already on its way to the login. */
    data object SignedOut : AttendanceStart

    /**
     * The group is not in the local cache, so its `schoolId` cannot be trusted.
     * Saving a session without it would be rejected by the rules anyway.
     */
    data class NoGroup(val groupId: String) : AttendanceStart

    /**
     * The subject does not exist in the catalog or does not belong to this
     * group's grade. Opening a session with it would put an unfilterable mark in
     * the tutor's list, so the screen must ask for a different subject.
     */
    data class InvalidSubject(val groupId: String, val subjectId: String) : AttendanceStart
}

/**
 * Outcome of saving a roll call.
 *
 * [Incomplete] is produced by the use case before the cache is touched: the
 * schema has no "unmarked" status, and a session with missing students is not a
 * session. Everything else comes from the repository.
 */
sealed interface SaveRollCallOutcome {

    /** Written to Room and queued for sync; it may reach Firestore much later. */
    data object Queued : SaveRollCallOutcome

    data object SignedOut : SaveRollCallOutcome

    /** The roster is empty, so there is no session to write. */
    data object EmptyRoster : SaveRollCallOutcome

    /** [unmarked] students still have no mark. */
    data class Incomplete(val unmarked: Int) : SaveRollCallOutcome

    /**
     * The local write failed. [groupId] travels in the outcome so the screen can
     * name the group instead of printing "no se pudo guardar" with no context.
     */
    data class Failed(val groupId: String) : SaveRollCallOutcome
}
