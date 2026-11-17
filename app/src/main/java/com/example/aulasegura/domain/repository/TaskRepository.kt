package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.NewTask
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.model.PublishedTask
import kotlinx.coroutines.flow.Flow

/**
 * Write and read side of the teacher's tasks.
 *
 * Same offline-first contract as [IncidentRepository]: [publish] writes the
 * cache row and the outbox row in one transaction and never touches the network;
 * `TaskSyncHandler` is what reaches Firestore.
 *
 * [observeMyTasks] reads from Room only. Listing `tasks` from Firestore as a
 * docente needs an `array-contains-any` query over the groups, and this block
 * does not pay for that read: the segment is the teacher's own readback of what
 * this device published, which is exactly what the cache already holds.
 */
interface TaskRepository {

    /** Stores [task] locally and queues it for Firestore. */
    suspend fun publish(task: NewTask): PublishOutcome

    /** Tasks this teacher published on this device, newest first. */
    fun observeMyTasks(): Flow<List<PublishedTask>>
}
