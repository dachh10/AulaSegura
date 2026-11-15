package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.SyncPhase

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
 * [canSave] mirrors the two rules the domain enforces (`NoStudent`,
 * `EmptyDescription`), so the footer is disabled for the same reasons the use
 * case would reject — the screen does not get to have its own opinion.
 */
data class TeacherIncidentUi(
    val students: List<IncidentStudentUi> = emptyList(),
    val selectedId: String? = null,
    val severity: IncidentSeverity = IncidentSeverity.MODERADO,
    val description: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val sync: SyncPhase? = null,
    val message: String? = null,
) {

    val selected: IncidentStudentUi?
        get() = students.firstOrNull { it.id == selectedId }

    val canSave: Boolean
        get() = selected != null && description.isNotBlank() && !saving && !saved
}

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
