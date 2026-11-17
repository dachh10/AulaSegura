package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.NewNotice
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.model.PublishedNotice
import kotlinx.coroutines.flow.Flow

/**
 * Write and read side of the teacher's notices.
 *
 * The read is cache-only for the same reason as [TaskRepository]: a docente
 * listing `notices` would have to prove two array conditions
 * (`audience.roles` and `audience.groupIds`) in one query, and Firestore allows
 * only one array operator per query. The rules are right; the list is what is
 * not buildable without a rules change, so the history reads what this device
 * published.
 */
interface NoticeRepository {

    /** Stores [notice] locally and queues it for Firestore. */
    suspend fun publish(notice: NewNotice): PublishOutcome

    /** Notices this teacher published on this device, newest first. */
    fun observeMyNotices(): Flow<List<PublishedNotice>>
}
