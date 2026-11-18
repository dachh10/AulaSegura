package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.Flow

/** The outbox count, read from the same table the sync worker drains. */
class RoomSyncQueueRepository(
    private val outboxDao: OutboxDao,
) : SyncQueueRepository {

    override fun observePendingCount(): Flow<Int> = outboxDao.observeCount()
}
