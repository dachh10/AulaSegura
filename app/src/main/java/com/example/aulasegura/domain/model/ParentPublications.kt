package com.example.aulasegura.domain.model

/**
 * What the tutor reads from `tasks/{taskId}` and `notices/{noticeId}`, in the
 * shape `CONTEXT.md` §6.2 already documents for those collections.
 *
 * No field here is derived from a design mock. `attachments` carries only what
 * Cloudinary and the document agree on (name, URL, MIME type): the schema has no
 * size, so the UI cannot print one.
 */

/** One file the teacher attached to a task or a notice. */
data class ParentAttachment(
    val name: String,
    val url: String,
    /** MIME type stored at upload time, e.g. `application/pdf`. */
    val type: String,
) {
    val isPdf: Boolean get() = type.contains("pdf", ignoreCase = true)
}

/**
 * A task published to the group of one child.
 *
 * [groupIds] travels along because the detail screen names the group, and the
 * task carries no group name of its own: `groups/{id}.name` is resolved by the
 * caller with the children it already loaded.
 */
data class ParentTaskItem(
    val taskId: String,
    val groupIds: List<String>,
    val subject: String,
    val title: String,
    val description: String,
    val dueDateMillis: Long,
    val createdAtMillis: Long,
    val createdByName: String,
    val attachments: List<ParentAttachment> = emptyList(),
)

/**
 * A notice addressed to the families of the child's group.
 *
 * [category] is the publisher's own value (`escuela`, `grupo`, `urgente`), kept
 * as a string the way it is stored: only `escuela` changes how the card paints
 * its source, and inventing an enum for a value the schema leaves open would
 * drop a notice written by a newer version of the teacher panel.
 */
data class ParentNoticeItem(
    val noticeId: String,
    val title: String,
    val body: String,
    val category: String,
    val urgent: Boolean,
    val createdByName: String,
    val createdAtMillis: Long,
    val attachments: List<ParentAttachment> = emptyList(),
    /** `true` when `users/{uid}/noticeReads/{noticeId}` exists. */
    val read: Boolean = false,
) {
    val fromSchool: Boolean get() = category == CATEGORY_SCHOOL

    private companion object {
        const val CATEGORY_SCHOOL = "escuela"
    }
}
