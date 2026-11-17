package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.NoticeEntity
import com.example.aulasegura.data.local.entity.TaskEntity
import com.example.aulasegura.data.local.mapper.toTimestamp
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask

/**
 * The two directions of a publication.
 *
 * Room feeds the "Publicaciones" segment and Firestore receives the sync, so the
 * cached row and the document are mapped here side by side: a field added to one
 * and forgotten in the other would publish a task the teacher cannot read back.
 */

/** A cached task, as the history row shows it. */
internal fun TaskEntity.toPublishedTask(
    groupNames: List<String>,
    pending: Boolean,
): PublishedTask = PublishedTask(
    taskId = taskId,
    title = title,
    subject = subject,
    description = description,
    groupNames = groupNames,
    dueDate = dueDate,
    createdAt = createdAt,
    pending = pending,
    attachmentName = attachments().firstOrNull()?.fileName,
)

/** A cached notice, as the history row shows it. */
internal fun NoticeEntity.toPublishedNotice(
    groupNames: List<String>,
    pending: Boolean,
): PublishedNotice = PublishedNotice(
    noticeId = noticeId,
    title = title,
    body = body,
    groupNames = groupNames,
    urgent = urgent,
    createdAt = createdAt,
    pending = pending,
)

/**
 * `tasks/{taskId}`, exactly as `CONTEXT.md` §`tasks/{taskId}` documents it.
 *
 * `attachments` is always an array: empty when the task has no PDF, and with the
 * single uploaded file when it does. A file still in flight is left out on
 * purpose — `TaskSyncHandler` does not write the document until it is done.
 */
internal fun TaskEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "schoolId" to schoolId,
    "groupIds" to groupIds,
    "subject" to subject,
    "title" to title,
    "description" to description,
    "dueDate" to dueDate.toTimestamp(),
    "attachments" to attachments().filter { it.isUploaded }.map { it.toFirestoreMap() },
    "createdBy" to createdBy,
    "createdByName" to createdByName,
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
    "active" to active,
)

/**
 * `notices/{noticeId}`, exactly as `CONTEXT.md` §`notices/{noticeId}` documents
 * it.
 *
 * `expiresAt` is written as `null`: Fase 1 does not schedule a notice, and the
 * field is optional in the schema. Sending it explicitly keeps the document shape
 * stable for the tutor screens instead of leaving the key to appear later.
 */
internal fun NoticeEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "schoolId" to schoolId,
    "audience" to mapOf(
        "roles" to audienceRoles,
        "groupIds" to audienceGroupIds,
        "studentIds" to audienceStudentIds,
    ),
    "title" to title,
    "body" to body,
    "category" to category,
    "urgent" to urgent,
    "attachments" to emptyList<Map<String, Any>>(),
    "createdBy" to createdBy,
    "createdByName" to createdByName,
    "createdAt" to createdAt.toTimestamp(),
    "expiresAt" to expiresAt?.toTimestamp(),
)

/**
 * Resolves `groupIds` against the roster labels this device already has.
 *
 * A group missing from the cache is skipped instead of printed as its raw id:
 * `grp-3b` in the UI reads as a bug, and the row is still readable without it.
 */
internal fun groupNamesFor(groupIds: List<String>, labels: Map<String, String>): List<String> =
    groupIds.mapNotNull { labels[it] }
