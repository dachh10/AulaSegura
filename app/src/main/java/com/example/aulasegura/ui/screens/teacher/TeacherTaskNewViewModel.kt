package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.local.media.KeptTaskAttachment
import com.example.aulasegura.data.local.media.TaskAttachmentStore
import com.example.aulasegura.domain.model.NewTask
import com.example.aulasegura.domain.model.NewTaskAttachment
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.TaskRepository
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Publicar tarea".
 *
 * Same offline-first contract as the incident form: publishing writes Room and
 * the outbox, and the screen says "guardada" because that is the truth on this
 * device. The teacher's own "Publicaciones" segment is where the phone tells
 * them whether it already left.
 *
 * The PDF follows the incident-photo pattern: the bytes are copied into private
 * storage while the form is open (so the content URI is not needed after the
 * picker closes and a retry uploads the same bytes), and the copy is deleted once
 * Cloudinary accepts it or the teacher removes it.
 *
 * The due date starts one week out at 08:00 so the form is publishable as soon as
 * the group and the title are set, and every value can still be changed.
 */
class TeacherTaskNewViewModel(
    groups: GroupRepository,
    auth: AuthRepository,
    private val tasks: TaskRepository,
    private val attachmentStore: TaskAttachmentStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val selectedGroupId = MutableStateFlow<String?>(null)
    private val subject = MutableStateFlow(auth.currentUser?.subjects?.firstOrNull() ?: DEFAULT_SUBJECT)
    private val title = MutableStateFlow("")
    private val description = MutableStateFlow("")
    private val dueDate = MutableStateFlow(defaultDueDate())
    private val attachment = MutableStateFlow<KeptTaskAttachment?>(null)
    private val attaching = MutableStateFlow(false)
    private val publishing = MutableStateFlow(false)
    private val published = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val subjects: List<String> =
        auth.currentUser?.subjects?.takeIf { it.isNotEmpty() } ?: listOf(DEFAULT_SUBJECT)

    private val myGroups = groups.observeMyGroups()
        .map { list -> list.map { PublishGroupUi(id = it.id, label = it.label) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private data class Form(
        val subject: String,
        val title: String,
        val description: String,
        val dueDate: Long,
    )

    private val form = combine(subject, title, description, dueDate) { subjectValue, titleValue, text, due ->
        Form(subject = subjectValue, title = titleValue, description = text, dueDate = due)
    }

    private data class Flags(
        val publishing: Boolean,
        val published: Boolean,
        val attaching: Boolean,
        val message: String?,
    )

    private val flags = combine(publishing, published, attaching, message) {
            isPublishing,
            isPublished,
            isAttaching,
            text,
        ->
        Flags(
            publishing = isPublishing,
            published = isPublished,
            attaching = isAttaching,
            message = text,
        )
    }

    val state: StateFlow<TeacherTaskNewUi> =
        combine(myGroups, selectedGroupId, form, attachment, flags) {
                groupList,
                groupId,
                fields,
                keptFile,
                status,
            ->
            TeacherTaskNewUi(
                groups = groupList,
                selectedGroupId = groupId,
                selectedGroupLabel = groupList.firstOrNull { it.id == groupId }?.label,
                subjects = subjects,
                subject = fields.subject,
                title = fields.title,
                description = fields.description,
                dueDate = fields.dueDate,
                dueDateLabel = taskDueDateLabel(fields.dueDate, zone),
                dueTimeLabel = taskDueTimeLabel(fields.dueDate, zone),
                publishing = status.publishing,
                published = status.published,
                message = status.message,
                canPublish = canPublishTask(
                    selectedGroupId = groupId,
                    title = fields.title,
                    publishing = status.publishing,
                    published = status.published,
                ),
                attachmentName = keptFile?.fileName,
                attachmentSizeLabel = keptFile?.let { taskAttachmentSizeLabel(it.bytes) },
                attaching = status.attaching,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TeacherTaskNewUi(
                subjects = subjects,
                subject = subject.value,
                dueDate = dueDate.value,
                dueDateLabel = taskDueDateLabel(dueDate.value, zone),
                dueTimeLabel = taskDueTimeLabel(dueDate.value, zone),
            ),
        )

    /** Preselects the classroom the teacher was viewing before opening the form. */
    fun preselectGroup(groupId: String) {
        if (published.value) return
        selectedGroupId.value = groupId
    }

    fun selectGroup(groupId: String) {
        if (published.value) return
        selectedGroupId.value = groupId
        message.value = null
    }

    fun selectSubject(value: String) {
        if (published.value) return
        subject.value = value
    }

    fun setTitle(value: String) {
        if (published.value) return
        title.value = value.take(TITLE_MAX_CHARS)
        message.value = null
    }

    fun setDescription(value: String) {
        if (published.value) return
        description.value = value.take(DESCRIPTION_MAX_CHARS)
        message.value = null
    }

    /** Replaces the day of [dueDate], keeping the hour the teacher already chose. */
    fun setDueDate(year: Int, month: Int, dayOfMonth: Int) {
        if (published.value) return
        val calendar = calendarAt(dueDate.value)
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        dueDate.value = calendar.timeInMillis
    }

    /** Replaces the hour and minute of [dueDate], keeping the day. */
    fun setDueTime(hourOfDay: Int, minute: Int) {
        if (published.value) return
        val calendar = calendarAt(dueDate.value)
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        dueDate.value = calendar.timeInMillis
    }

    /**
     * Keeps the chosen PDF, replacing whatever was attached before.
     *
     * The picker already filters to `application/pdf`, but the limit is enforced
     * here too: a provider can hand over a `.pdf` that is not one, and the size
     * ceiling is a Cloudinary quota, not a UI preference.
     */
    fun attachPdf(fileName: String, bytes: ByteArray) {
        if (publishing.value || published.value || attaching.value) return

        val cleanName = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .ifBlank { DEFAULT_ATTACHMENT_NAME }

        if (!cleanName.lowercase().endsWith(".pdf")) {
            message.value = "Solo se puede adjuntar un PDF."
            return
        }
        if (bytes.isEmpty()) {
            message.value = "Ese PDF está vacío."
            return
        }
        if (bytes.size > TASK_ATTACHMENT_MAX_BYTES) {
            message.value = "El PDF supera los 10 MB. Comprímelo o divide la tarea."
            return
        }

        attaching.value = true
        message.value = null

        viewModelScope.launch {
            val kept = attachmentStore.keep(bytes, cleanName)
            if (kept == null) {
                message.value = "No se pudo guardar el PDF en el teléfono."
            } else {
                // The previous file has no reader left: the form shows one PDF.
                attachment.value?.let { previous -> attachmentStore.discard(previous.localPath) }
                attachment.value = kept
            }
            attaching.value = false
        }
    }

    fun removeAttachment() {
        if (publishing.value || published.value) return
        attachment.value?.let { attachmentStore.discard(it.localPath) }
        attachment.value = null
        message.value = null
    }

    fun publish() {
        if (publishing.value || published.value) return

        val groupId = selectedGroupId.value
        if (groupId == null) {
            message.value = "Elige el grupo de la tarea."
            return
        }
        if (title.value.isBlank()) {
            message.value = "Escribe el título de la tarea."
            return
        }

        publishing.value = true
        message.value = null

        val keptFile = attachment.value

        viewModelScope.launch {
            val outcome = tasks.publish(
                NewTask(
                    groupIds = listOf(groupId),
                    subject = subject.value,
                    title = title.value.trim(),
                    description = description.value.trim(),
                    dueDate = dueDate.value,
                    attachment = keptFile?.let { kept ->
                        NewTaskAttachment(
                            fileName = kept.fileName,
                            localPath = kept.localPath,
                            bytes = kept.bytes,
                        )
                    },
                ),
            )

            when (outcome) {
                is PublishOutcome.Queued -> published.value = true
                else -> message.value = taskPublishMessage(outcome)
            }

            publishing.value = false
        }
    }

    override fun onCleared() {
        // The screen was closed without publishing: that copy will never be
        // uploaded, and once the task is queued the worker owns the file.
        if (!published.value) {
            attachment.value?.let { attachmentStore.discard(it.localPath) }
        }
        super.onCleared()
    }

    /** One week from now at 08:00, which is the earliest a school day starts. */
    private fun defaultDueDate(): Long =
        calendarAt(now()).apply {
            add(Calendar.DAY_OF_YEAR, DEFAULT_DUE_DAYS)
            set(Calendar.HOUR_OF_DAY, DEFAULT_DUE_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun calendarAt(epochMillis: Long): Calendar =
        Calendar.getInstance(zone).apply { timeInMillis = epochMillis }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TITLE_MAX_CHARS = 120
        const val DESCRIPTION_MAX_CHARS = 500
        const val DEFAULT_DUE_DAYS = 7
        const val DEFAULT_DUE_HOUR = 8
        const val DEFAULT_SUBJECT = "General"
        const val DEFAULT_ATTACHMENT_NAME = "documento.pdf"
    }
}
