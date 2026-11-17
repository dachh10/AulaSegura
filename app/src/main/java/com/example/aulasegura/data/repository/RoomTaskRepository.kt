package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.dao.TaskDao
import com.example.aulasegura.data.local.entity.TaskEntity
import com.example.aulasegura.data.mapper.StoredTaskAttachment
import com.example.aulasegura.data.mapper.encodeTaskAttachments
import com.example.aulasegura.data.mapper.groupNamesFor
import com.example.aulasegura.data.mapper.toPublishedTask
import com.example.aulasegura.domain.model.NewTask
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.model.PublishedTask
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.TaskRepository
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
 * The task write path: Room first, Firestore later.
 *
 * Nothing here talks to the network. The cached row and the outbox row are
 * written in one transaction, so a task the teacher sees as published is
 * guaranteed to be queued — and one published in airplane mode reaches the cloud
 * when the worker runs, once, under the client UUID.
 *
 * The group is resolved from the cached roster and not trusted from the caller:
 * `schoolId` and `groupIds` have to be the ones the rules accept
 * (`groupIds.hasAny(myGroups()) && hasOnly(myGroups())`), and a list with a group
 * the teacher does not teach is denied after the form is already closed.
 */
class RoomTaskRepository(
    private val taskDao: TaskDao,
    private val outboxDao: OutboxDao,
    private val outbox: OutboxRepository,
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newTaskId: () -> String = { UUID.randomUUID().toString() },
) : TaskRepository {

    override suspend fun publish(task: NewTask): PublishOutcome {
        val teacher = auth.currentUser ?: return PublishOutcome.SignedOut
        if (task.groupIds.isEmpty()) return PublishOutcome.GroupNotMine("")

        val mine = groups.observeMyGroups().first()
        val chosen = task.groupIds.map { id ->
            mine.firstOrNull { it.id == id } ?: return PublishOutcome.GroupNotMine(id)
        }

        val now = clock()
        val entity = TaskEntity(
            taskId = newTaskId(),
            schoolId = chosen.first().schoolId,
            groupIds = chosen.map { it.id },
            subject = task.subject,
            title = task.title,
            description = task.description,
            dueDate = task.dueDate,
            createdBy = teacher.uid,
            createdByName = teacher.fullName,
            active = true,
            // The PDF is already on disk; what is queued is its upload, not the
            // file itself. `TaskSyncHandler` waits for `UPLOADED` before writing
            // the document, so the family never sees an empty array.
            attachmentsJson = task.attachment?.let { attachment ->
                encodeTaskAttachments(
                    listOf(
                        StoredTaskAttachment(
                            attachmentId = UUID.randomUUID().toString(),
                            fileName = attachment.fileName,
                            bytes = attachment.bytes,
                            mimeType = attachment.mimeType,
                            localPath = attachment.localPath,
                        ),
                    ),
                )
            } ?: "[]",
            createdAt = now,
            updatedAt = now,
        )

        return try {
            outbox.enqueueTask(entity, uploadAttachment = task.attachment != null)
            // A publication the teacher completed, whether or not it has left the
            // phone: the counter exists to see how many were written, and the
            // sync side already reports its own failures.
            analytics.log(AnalyticsEvent.TaskPublished)
            PublishOutcome.Queued(entity.taskId)
        } catch (failed: Exception) {
            // Room is the local source of truth: if the transaction did not
            // commit, nothing changed and the form can be retried as it is.
            PublishOutcome.StoreFailed
        }
    }

    override fun observeMyTasks(): Flow<List<PublishedTask>> {
        val user = auth.currentUser ?: return flowOf(emptyList())
        return combine(
            taskDao.observeByCreator(user.uid),
            groups.observeMyGroups(),
            outboxDao.observeAll(),
        ) { rows, myGroups, queue ->
            val labels = myGroups.associate { it.id to it.label }
            // Pending is read from the outbox and not from the row: the queue is
            // what actually knows whether Firestore accepted the document, and it
            // is the same source the worker drains.
            val pendingIds = queue.map { it.entityLocalId }.toSet()
            rows.map { row ->
                row.toPublishedTask(
                    groupNames = groupNamesFor(row.groupIds, labels),
                    pending = row.taskId in pendingIds,
                )
            }
        }
    }
}
