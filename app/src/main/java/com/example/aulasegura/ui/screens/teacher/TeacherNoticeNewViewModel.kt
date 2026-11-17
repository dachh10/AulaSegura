package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.NewNotice
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Publicar aviso".
 *
 * The audience is the teacher's real groups, cache-first, and starts empty: a
 * notice published to "all my groups" because the first chip was preselected is
 * a message sent to families who have nothing to do with it.
 *
 * The audience is always the tutors of those groups. The preview says "ASÍ LO
 * VERÁN LAS FAMILIAS", and addressing teachers too would need a teacher inbox
 * that Fase 1 does not have.
 */
class TeacherNoticeNewViewModel(
    groups: GroupRepository,
    auth: AuthRepository,
    private val notices: NoticeRepository,
) : ViewModel() {

    private val selectedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    private val title = MutableStateFlow("")
    private val body = MutableStateFlow("")
    private val urgent = MutableStateFlow(false)
    private val publishing = MutableStateFlow(false)
    private val published = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val teacherName: String = auth.currentUser?.fullName.orEmpty()

    private val myGroups = groups.observeMyGroups()
        .map { list -> list.map { PublishGroupUi(id = it.id, label = it.label) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private data class Form(
        val title: String,
        val body: String,
        val urgent: Boolean,
        val selected: Set<String>,
    )

    private val form = combine(title, body, urgent, selectedGroupIds) { value, text, isUrgent, groups ->
        Form(title = value, body = text, urgent = isUrgent, selected = groups)
    }

    private data class Flags(
        val publishing: Boolean,
        val published: Boolean,
        val message: String?,
    )

    private val flags = combine(publishing, published, message) { isPublishing, isPublished, text ->
        Flags(publishing = isPublishing, published = isPublished, message = text)
    }

    val state: StateFlow<TeacherNoticeNewUi> =
        combine(myGroups, form, flags) { groupList, fields, status ->
            TeacherNoticeNewUi(
                groups = groupList,
                selectedGroupIds = fields.selected,
                selectedGroupLabels = groupList.filter { it.id in fields.selected }.map { it.label },
                teacherName = teacherName,
                title = fields.title,
                body = fields.body,
                urgent = fields.urgent,
                publishing = status.publishing,
                published = status.published,
                message = status.message,
                canPublish = canPublishNotice(
                    selectedGroupIds = fields.selected,
                    title = fields.title,
                    publishing = status.publishing,
                    published = status.published,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TeacherNoticeNewUi(teacherName = teacherName),
        )

    fun toggleGroup(groupId: String) {
        if (published.value) return
        selectedGroupIds.value = selectedGroupIds.value.let { current ->
            if (groupId in current) current - groupId else current + groupId
        }
        message.value = null
    }

    /**
     * The "Todos mis grupos" chip: selects every group, or clears them when they
     * are already all selected.
     */
    fun toggleAllGroups() {
        if (published.value) return
        val all = myGroups.value.map { it.id }.toSet()
        selectedGroupIds.value = if (selectedGroupIds.value == all) emptySet() else all
        message.value = null
    }

    fun setTitle(value: String) {
        if (published.value) return
        title.value = value.take(TITLE_MAX_CHARS)
        message.value = null
    }

    fun setBody(value: String) {
        if (published.value) return
        body.value = value.take(BODY_MAX_CHARS)
        message.value = null
    }

    fun setUrgent(value: Boolean) {
        if (published.value) return
        urgent.value = value
    }

    fun publish() {
        if (publishing.value || published.value) return

        if (selectedGroupIds.value.isEmpty()) {
            message.value = "Elige a qué grupo va el aviso."
            return
        }
        if (title.value.isBlank()) {
            message.value = "Escribe el título del aviso."
            return
        }

        publishing.value = true
        message.value = null

        viewModelScope.launch {
            val outcome = notices.publish(
                NewNotice(
                    groupIds = selectedGroupIds.value.toList(),
                    audienceRoles = listOf(AUDIENCE_TUTOR),
                    title = title.value.trim(),
                    body = body.value.trim(),
                    urgent = urgent.value,
                ),
            )

            when (outcome) {
                is PublishOutcome.Queued -> published.value = true
                else -> message.value = noticePublishMessage(outcome)
            }

            publishing.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TITLE_MAX_CHARS = 80
        const val BODY_MAX_CHARS = 300

        /** The only audience this panel writes; see the class KDoc. */
        const val AUDIENCE_TUTOR = "tutor"
    }
}
