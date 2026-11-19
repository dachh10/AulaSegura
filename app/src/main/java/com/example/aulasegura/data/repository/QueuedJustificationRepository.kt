package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.media.JustificationAttachmentStore
import com.example.aulasegura.data.mapper.JustificationPayload
import com.example.aulasegura.data.mapper.StoredJustificationAttachment
import com.example.aulasegura.data.mapper.encodeJustificationPayload
import com.example.aulasegura.domain.model.JustificationDraft
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.JustificationRepository
import com.example.aulasegura.domain.repository.JustificationResult
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import com.example.aulasegura.sync.SyncOperation
import com.example.aulasegura.sync.SyncScheduling
import java.util.UUID

/**
 * The tutor's justification, stored on the phone and queued for the worker.
 *
 * Same contract as the teacher's offline writes: [submit] returns once the file
 * and the queue row are on disk, and the network happens later in
 * `JustificationSyncHandler`. The screen can promise "se enviará después"
 * precisely because nothing here waits for Firestore.
 *
 * The document id is a UUID generated now, not at sync time, which is what makes
 * a replay idempotent: the same row always rewrites `justifications/{uuid}`.
 *
 * The receipt is kept **before** the row is inserted. A row pointing at a file
 * that was never written would fail forever in the worker; if the copy cannot be
 * written, the tutor is told instead of being given a silent promise.
 */
class QueuedJustificationRepository(
    private val database: AulaSeguraDatabase,
    private val attachments: JustificationAttachmentStore,
    private val auth: AuthRepository,
    private val scheduling: SyncScheduling,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) : JustificationRepository {

    override suspend fun submit(draft: JustificationDraft): JustificationResult {
        // `schoolId` and `tutorId` are not the screen's business: they come from
        // the session profile, which is exactly what the rule checks.
        val user = auth.currentUser ?: return JustificationResult.SignedOut
        val now = System.currentTimeMillis()

        val kept = draft.attachment?.let { pending ->
            attachments.keep(
                bytes = pending.bytes,
                fileName = pending.fileName,
                mimeType = pending.mimeType,
            ) ?: return JustificationResult.StorageFailed
        }

        val payload = JustificationPayload(
            studentId = draft.studentId,
            tutorId = user.uid,
            schoolId = user.schoolId,
            groupId = draft.groupId,
            dateFromMillis = draft.absenceDateMillis,
            dateToMillis = draft.absenceDateMillis,
            reason = draft.reasonCode,
            description = draft.description,
            createdAtMillis = now,
            attachment = kept?.let {
                StoredJustificationAttachment(
                    fileName = it.fileName,
                    bytes = it.bytes,
                    mimeType = it.mimeType,
                    localPath = it.localPath,
                )
            },
        )

        // One row, one insert: unlike attendance there is no cache entity to keep
        // in step, so there is nothing to make atomic with it.
        database.outboxDao().insert(
            OutboxEntity(
                opType = SyncOperation.UPSERT_JUSTIFICATION.name,
                entityLocalId = UUID.randomUUID().toString(),
                payloadJson = encodeJustificationPayload(payload),
                createdAt = now,
            ),
        )

        // Queued is not sent: the event counts the report the family filed, which
        // is what the principal's backlog should match.
        analytics.log(AnalyticsEvent.JustificationCreated)

        scheduling.schedule()
        return JustificationResult.Queued
    }
}
