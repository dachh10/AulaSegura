package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.PublishOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What "Publicar tarea" renders, already resolved.
 *
 * The screen gets this and nothing else: the pure helpers below are what the
 * tests exercise, because a date label and a disabled footer are exactly the
 * parts that break silently.
 */
data class TeacherTaskNewUi(
    val groups: List<PublishGroupUi> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedGroupLabel: String? = null,
    val subjects: List<String> = emptyList(),
    val subject: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0L,
    val dueDateLabel: String = "",
    val dueTimeLabel: String = "",
    val publishing: Boolean = false,
    val published: Boolean = false,
    val message: String? = null,
    val canPublish: Boolean = false,
    /** Name of the PDF already copied into private storage. */
    val attachmentName: String? = null,
    /** `"1.2 MB"`, next to the name. */
    val attachmentSizeLabel: String? = null,
    val attaching: Boolean = false,
)

/**
 * The only attachment a task accepts is one PDF.
 *
 * One file because a task is an instruction, not a file transfer; PDF because it
 * opens the same on every phone the families own, and Word or HEIC do not.
 * 10 MB is the ceiling: a scanned worksheet fits, a 40-page textbook scan does
 * not, and Cloudinary's free tier is what pays for the difference.
 */
const val TASK_ATTACHMENT_MAX_BYTES: Long = 10L * 1024 * 1024

/** `"1.2 MB"` / `"840 KB"` for the chip under the form. */
fun taskAttachmentSizeLabel(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%d KB", bytes / 1024L)
    else -> "$bytes B"
}

/** One group of the selector. */
data class PublishGroupUi(val id: String, val label: String)

/**
 * The footer is enabled only when the task can actually be published.
 *
 * The group is required and is never auto-selected: a task sent to the wrong
 * group reaches thirty families, and "it was already selected" is not a defence
 * the teacher can give.
 */
fun canPublishTask(
    selectedGroupId: String?,
    title: String,
    publishing: Boolean,
    published: Boolean,
): Boolean = !publishing && !published && selectedGroupId != null && title.isNotBlank()

/** `"Mié 5 nov 2026"` — what the date field prints. */
fun taskDueDateLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("EEE d MMM yyyy", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
        .replace(".", "")
        .replaceFirstChar { it.uppercase(SPANISH_MX) }

/** `"08:00"` — the time field, always 24 h so it matches `AttendanceTimes`. */
fun taskDueTimeLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("HH:mm", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))

/** Why a publication could not be queued, in the teacher's words. */
fun taskPublishMessage(outcome: PublishOutcome): String = when (outcome) {
    is PublishOutcome.Queued -> "Tarea guardada."
    is PublishOutcome.GroupNotMine -> "Ese grupo ya no está en tu lista. Actualiza tus grupos."
    PublishOutcome.SignedOut -> "Tu sesión expiró. Vuelve a iniciar sesión."
    PublishOutcome.StoreFailed -> "No se pudo guardar la tarea. Vuelve a intentarlo."
}

/** Same sentence pattern as [taskPublishMessage], for the notice screen. */
fun noticePublishMessage(outcome: PublishOutcome): String = when (outcome) {
    is PublishOutcome.Queued -> "Aviso guardado."
    is PublishOutcome.GroupNotMine -> "Ese grupo ya no está en tu lista. Actualiza tus grupos."
    PublishOutcome.SignedOut -> "Tu sesión expiró. Vuelve a iniciar sesión."
    PublishOutcome.StoreFailed -> "No se pudo guardar el aviso. Vuelve a intentarlo."
}

/** Fixed to Spanish because the whole UI is, whatever the device language is. */
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")
