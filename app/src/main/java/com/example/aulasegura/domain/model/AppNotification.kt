package com.example.aulasegura.domain.model

/**
 * One row of `notifications/{notificationId}`.
 *
 * A document **per recipient**: the id is unique for the person, so `read` is a
 * per-user fact and not a shared flag (`CONTEXT.md` §`notifications`).
 *
 * [deepLink] is an internal route ("teacher/history"), not a URL: the app decides
 * what to open, the server only says where the teacher probably wants to go.
 */
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val deepLink: String?,
    val read: Boolean,
    val createdAt: Long,
)

/** `notifications.type`, narrowed to what the UI can style. */
enum class NotificationType(val firestoreValue: String) {
    ASISTENCIA("asistencia"),
    INCIDENTE("incidente"),
    TAREA("tarea"),
    AVISO("aviso"),
    MENSAJE("mensaje"),
    SISTEMA("sistema"),
    OTRO("otro"),
    ;

    companion object {
        fun from(value: String?): NotificationType =
            entries.firstOrNull { it.firestoreValue == value } ?: OTRO
    }
}
