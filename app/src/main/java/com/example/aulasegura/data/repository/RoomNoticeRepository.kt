package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.NoticeDao
import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.entity.NoticeEntity
import com.example.aulasegura.data.mapper.groupNamesFor
import com.example.aulasegura.data.mapper.toPublishedNotice
import com.example.aulasegura.domain.model.NewNotice
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import com.example.aulasegura.sync.OutboxRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

/**
 * The notice write path: Room first, Firestore later.
 *
 * Same contract and same transaction rule as [RoomTaskRepository]. The audience
 * is flattened into three columns in the cache and rebuilt as the `audience` map
 * by `PublicationMapper`; what the rules check on create is exactly the two
 * arrays this repository resolves from the roster.
 *
 * `category` is derived here and not in the form: `urgente` when [NewNotice.urgent]
 * is set, `grupo` otherwise, so the chip the teacher picks and the value the
 * tutor filters by can never disagree.
 */
class RoomNoticeRepository(
    private val noticeDao: NoticeDao,
    private val outboxDao: OutboxDao,
    private val outbox: OutboxRepository,
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newNoticeId: () -> String = { UUID.randomUUID().toString() },
) : NoticeRepository {

    override suspend fun publish(notice: NewNotice): PublishOutcome {
        val teacher = auth.currentUser ?: return PublishOutcome.SignedOut
        if (notice.groupIds.isEmpty()) return PublishOutcome.GroupNotMine("")

        val mine = groups.observeMyGroups().first()
        val chosen = notice.groupIds.map { id ->
            mine.firstOrNull { it.id == id } ?: return PublishOutcome.GroupNotMine(id)
        }

        val now = clock()
        val entity = NoticeEntity(
            noticeId = newNoticeId(),
            schoolId = chosen.first().schoolId,
            audienceRoles = notice.audienceRoles,
            audienceGroupIds = chosen.map { it.id },
            audienceStudentIds = emptyList(),
            title = notice.title,
            body = notice.body,
            category = if (notice.urgent) CATEGORY_URGENT else CATEGORY_GROUP,
            urgent = notice.urgent,
            createdBy = teacher.uid,
            createdByName = teacher.fullName,
            createdAt = now,
            // Fase 1 does not schedule a notice: it is visible until the tutor's
            // list stops showing it, which is a later phase's decision.
            expiresAt = null,
        )

        return try {
            outbox.enqueueNotice(entity)
            analytics.log(AnalyticsEvent.NoticePublished)
            PublishOutcome.Queued(entity.noticeId)
        } catch (failed: Exception) {
            PublishOutcome.StoreFailed
        }
    }

    override fun observeMyNotices(): Flow<List<PublishedNotice>> {
        val user = auth.currentUser ?: return flowOf(emptyList())
        return combine(
            noticeDao.observeByCreator(user.uid),
            groups.observeMyGroups(),
            outboxDao.observeAll(),
        ) { rows, myGroups, queue ->
            val labels = myGroups.associate { it.id to it.label }
            val pendingIds = queue.map { it.entityLocalId }.toSet()
            rows.map { row ->
                row.toPublishedNotice(
                    groupNames = groupNamesFor(row.audienceGroupIds, labels),
                    pending = row.noticeId in pendingIds,
                )
            }
        }
    }

    private companion object {
        const val CATEGORY_URGENT = "urgente"
        const val CATEGORY_GROUP = "grupo"
    }
}
