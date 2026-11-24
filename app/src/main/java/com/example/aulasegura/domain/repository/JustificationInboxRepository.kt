package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationReview

/**
 * Read side of the justifications, for the roles that decide and observe them.
 *
 * The tutor's write side stays in [JustificationRepository]; this contract is
 * deliberately separate because reading the inbox is an online operation with
 * its own permissions, not part of the offline queue.
 */
interface JustificationInboxRepository {

    /**
     * The justifications filed for [groupIds], newest absence first.
     *
     * Each group is queried on its own: a single `whereIn` would fail as a whole
     * when one stale group id is no longer in the teacher's profile, while a
     * per-group query keeps the readable groups working.
     */
    suspend fun loadForGroups(groupIds: List<String>): JustificationInboxResult

    /**
     * Every justification of the signed-in principal's school.
     *
     * The rule authorizes the read with `schoolId == mySchool()`, so this is the
     * only filter the query may carry.
     */
    suspend fun loadForSchool(): JustificationInboxResult

    /**
     * Records the school's decision. Only the principal may call this: the rules
     * restrict the update to the four review fields.
     */
    suspend fun review(request: JustificationReview): JustificationReviewResult
}

sealed interface JustificationReviewResult {

    data object Success : JustificationReviewResult

    data object SignedOut : JustificationReviewResult

    data class Failed(val reason: JustificationReviewFailure) : JustificationReviewResult
}

enum class JustificationReviewFailure {
    OFFLINE,
    DENIED,

    /** The report disappeared between the read and the decision. */
    NOT_FOUND,
    UNKNOWN,
}

sealed interface JustificationInboxResult {

    data class Loaded(val justifications: List<JustificationRecord>) : JustificationInboxResult

    data object SignedOut : JustificationInboxResult

    data class Failed(val reason: RosterFailure) : JustificationInboxResult
}
