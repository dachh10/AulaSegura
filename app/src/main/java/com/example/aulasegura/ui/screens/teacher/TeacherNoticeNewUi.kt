package com.example.aulasegura.ui.screens.teacher

/**
 * What "Publicar aviso" renders, already resolved.
 *
 * The chips are the teacher's real groups and can be combined: the schema stores
 * `audience.groupIds` as an array, so sending one notice to two of them is the
 * same document, not two.
 */
data class TeacherNoticeNewUi(
    val groups: List<PublishGroupUi> = emptyList(),
    val selectedGroupIds: Set<String> = emptySet(),
    val selectedGroupLabels: List<String> = emptyList(),
    val teacherName: String = "",
    val title: String = "",
    val body: String = "",
    val urgent: Boolean = false,
    val publishing: Boolean = false,
    val published: Boolean = false,
    val message: String? = null,
    val canPublish: Boolean = false,
)

/**
 * A notice needs its audience and its title; the body is optional because the
 * title is what the tutor reads in the list, and a short "Traer cartulina" is a
 * complete notice.
 */
fun canPublishNotice(
    selectedGroupIds: Set<String>,
    title: String,
    publishing: Boolean,
    published: Boolean,
): Boolean = !publishing && !published && selectedGroupIds.isNotEmpty() && title.isNotBlank()

/**
 * `"Normal"` / `"Importante"` — the two priorities the mockup offers.
 *
 * The words are the form's; the document only ever stores `urgent`,
 * `category` being derived from it.
 */
fun noticePriorityLabel(urgent: Boolean): String = if (urgent) "Importante" else "Normal"
