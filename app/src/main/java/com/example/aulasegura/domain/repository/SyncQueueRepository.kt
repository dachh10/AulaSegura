package com.example.aulasegura.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * How much work is still waiting in the outbox.
 *
 * The home prints it as "Por sincronizar". Exposed as a contract so the screen
 * never needs to know that the queue lives in Room.
 */
interface SyncQueueRepository {

    /** Rows waiting to reach Firestore, live. */
    fun observePendingCount(): Flow<Int>
}
