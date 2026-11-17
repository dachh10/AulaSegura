package com.example.aulasegura.domain.model

/**
 * What the teacher publishes from the class panel, in the schema of
 * `CONTEXT.md` §`tasks/{taskId}`.
 *
 * [groupIds] is a list because the schema is one, even though the Fase 1 form
 * publishes to a single group: the tutor's list groups by `groupIds`, and a
 * later multi-group task must not need a different document shape.
 *
 * `subject` is not derived from the group: a group has no subject, the teacher
 * does (`users/{uid}.subjects`), and the same group receives Math and Science
 * from the same person.
 */
data class NewTask(
    val groupIds: List<String>,
    val subject: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    /** The single PDF of this task, already copied into private storage. */
    val attachment: NewTaskAttachment? = null,
)

/**
 * One PDF the teacher attached to a task.
 *
 * One and only one, and PDF only: the family gets a document it can open on any
 * phone, and the form does not turn into a file manager. [localPath] is the copy
 * the worker uploads; it is deleted once Cloudinary accepts it.
 */
data class NewTaskAttachment(
    val fileName: String,
    val localPath: String,
    val bytes: Long,
    val mimeType: String = "application/pdf",
)

/**
 * One notice, in the schema of `CONTEXT.md` §`notices/{noticeId}`.
 *
 * [audienceRoles] is `["tutor"]` for a notice written from this panel: the
 * preview says "ASÍ LO VERÁN LAS FAMILIAS", and a notice that also addressed
 * teachers would need a teacher inbox that is not part of Fase 1.
 *
 * [urgent] is the only priority input; `category` is derived from it (`urgente`
 * when urgent, `grupo` otherwise) instead of being a second switch that could
 * disagree with the first.
 */
data class NewNotice(
    val groupIds: List<String>,
    val audienceRoles: List<String>,
    val title: String,
    val body: String,
    val urgent: Boolean,
)

/**
 * Result of queueing a publication.
 *
 * Same shape as [IncidentSaveOutcome]: "published" on this device means "queued
 * and guaranteed to leave the phone", not "already in Firestore". The sync worker
 * is what reaches the cloud, and the history segment reads the outbox to tell the
 * teacher whether it already did.
 */
sealed interface PublishOutcome {

    /** Stored and queued under [publicationId], which is also the document id. */
    data class Queued(val publicationId: String) : PublishOutcome

    /**
     * One of the requested groups is not in this device's roster.
     *
     * The rules only accept groups the teacher belongs to, so publishing anyway
     * would be denied in the cloud after the teacher closed the screen.
     */
    data class GroupNotMine(val groupId: String) : PublishOutcome

    data object SignedOut : PublishOutcome

    /** Room did not commit, so nothing changed and the form can be retried. */
    data object StoreFailed : PublishOutcome
}

/**
 * A task this teacher published, as the "Publicaciones" segment shows it.
 *
 * [groupNames] is already resolved: the screen must not know how to join a
 * `groupIds` array against the roster.
 */
data class PublishedTask(
    val taskId: String,
    val title: String,
    val subject: String,
    val description: String,
    val groupNames: List<String>,
    val dueDate: Long,
    val createdAt: Long,
    /** Still waiting in the outbox; `false` once Firestore accepted it. */
    val pending: Boolean,
    /** Name of the attached PDF, when the teacher added one. */
    val attachmentName: String? = null,
    /** Whether that PDF made it to Cloudinary, failed, or is still queued. */
    val attachmentState: AttachmentSyncState? = null,
)

/**
 * Where the task PDF is in its upload, as the history shows it.
 *
 * The document is written only after `UPLOADED`, so a published task showing
 * `PENDING` is a task whose file is still on this phone and whose document has
 * not left yet either.
 */
enum class AttachmentSyncState {
    PENDING,
    UPLOADED,
    FAILED,
}

/** A notice this teacher published, with the same resolved shape as [PublishedTask]. */
data class PublishedNotice(
    val noticeId: String,
    val title: String,
    val body: String,
    val groupNames: List<String>,
    val urgent: Boolean,
    val createdAt: Long,
    val pending: Boolean,
)
