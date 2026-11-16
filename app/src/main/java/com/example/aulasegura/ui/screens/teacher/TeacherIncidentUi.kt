package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.SyncPhase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * One student the teacher can file an incident for, with the group badge the
 * selector prints next to the name.
 *
 * A flat list instead of group sections: the mockup has a single selector row,
 * and the teacher knows who they are looking for by name.
 */
data class IncidentStudentUi(
    val id: String,
    val fullName: String,
    val initials: String,
    val groupId: String,
    val groupLabel: String,
)

/**
 * What "Registrar incidente" renders, already resolved.
 *
 * [canSave] mirrors the three rules the domain enforces (`NoStudent`,
 * `EmptyDescription`, and a severity the tutor can triage by), so the footer is
 * disabled for the same reasons the use case would reject — the screen does not
 * get to have its own opinion.
 *
 * [severity] starts `null` on purpose: "Moderado" as a silent default turned an
 * unanswered question into a triage decision nobody made.
 *
 * [occurredAt] is the instant captured when the form was opened. The card prints
 * it and the queued incident stores it, so the two cannot drift apart.
 */
data class TeacherIncidentUi(
    val students: List<IncidentStudentUi> = emptyList(),
    val selectedId: String? = null,
    val severity: IncidentSeverity? = null,
    val category: IncidentCategory = IncidentCategory.OTRO,
    val description: String = "",
    val occurredAt: Long = 0L,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val sync: SyncPhase? = null,
    val message: String? = null,
) {

    val selected: IncidentStudentUi?
        get() = students.firstOrNull { it.id == selectedId }

    val canSave: Boolean
        get() = selected != null &&
            severity != null &&
            description.isNotBlank() &&
            !saving &&
            !saved
}

/**
 * `"Mar 3 nov 2026 · 10:42"` — the date block of the form, in the mockup's shape.
 *
 * `java.time` is not used on purpose, same as [com.example.aulasegura.util.AttendanceTimes]:
 * `minSdk` is 24 and there is no core library desugaring.
 */
fun incidentTimestampLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat(INCIDENT_TIMESTAMP, SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
        .replace(".", "")
        .replaceFirstChar { it.uppercase(SPANISH_MX) }

private const val INCIDENT_TIMESTAMP = "EEE d MMM yyyy · HH:mm"

/** Fixed to Spanish because the whole UI is, whatever the device language is. */
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")

/**
 * The strip under the header: first what the local save answered, then the real
 * phase from Room. `null` leaves the screen with its own validation message.
 */
fun incidentSyncMessage(sync: SyncPhase?): String? = when (sync) {
    SyncPhase.PENDING ->
        "Incidente guardado en el dispositivo. Pendiente de sincronizar."

    SyncPhase.SYNCING -> "Enviando el incidente…"

    SyncPhase.SYNCED -> "Incidente guardado y sincronizado."

    SyncPhase.FAILED ->
        "Incidente guardado en el dispositivo. No se pudo enviar: se reintenta " +
            "automáticamente."

    null -> null
}
