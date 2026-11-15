package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallStudent
import com.example.aulasegura.domain.model.RollCallSync

/**
 * What the roll call screen renders, already resolved.
 *
 * The screen gets this and nothing else: no roster flow, no draft, no
 * repository. That keeps the mockup's visual code untouched by the data work
 * and makes the counting rules testable without Compose.
 *
 * [unmarked] is separate from [counts] on purpose — the four statuses never add
 * up to the roster while rows are untouched, and the difference is exactly what
 * the save button has to complain about.
 */
data class RollCallUi(
    val subtitle: String = "",
    val students: List<RollCallRowUi> = emptyList(),
    val counts: AttendanceCounts = AttendanceCounts(),
    val unmarked: Int = 0,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    /**
     * Phase of the saved session in the outbox, or `null` before anything was
     * saved. The screen paints this instead of promising "se sincroniza en
     * segundo plano": pending and rejected have to look different.
     */
    val sync: RollCallSync? = null,
    val message: String? = null,
)

/** One row of the list: a name, its official number and the mark on it. */
data class RollCallRowUi(
    val id: String,
    val number: String,
    val name: String,
    val initials: String,
    val mark: AttendanceMark?,
    val detail: String,
)

/** `"3° B · Matemáticas · Vie 14 nov"`, skipping empty parts. */
fun rollCallSubtitle(groupLabel: String, subject: String, dateLabel: String): String =
    listOf(groupLabel, subject.ifBlank { NO_SUBJECT }, dateLabel)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

fun RollCallStudent.toRowUi(): RollCallRowUi {
    val number = listNumber.toString().padStart(2, '0')
    return RollCallRowUi(
        id = id,
        number = number,
        name = fullName,
        initials = initials,
        mark = mark,
        detail = detailOf(number, mark),
    )
}

/**
 * First letter of the given name and of the first surname: "Mariana" + "Gómez
 * Ruiz" → "MG", "Ana Sofía" + "Aguilar" → "AA".
 *
 * The seed keeps surnames together in `lastName`, so the first letter of the
 * field is the one the mockup prints, without guessing how many given names a
 * full name carries.
 */
fun initialsOf(firstName: String, lastName: String): String {
    val first = firstName.trim().firstOrNull()?.uppercase() ?: ""
    val last = lastName.trim().firstOrNull()?.uppercase() ?: ""
    return (first + last).ifEmpty { "?" }
}

/** The second line of the row: the number, plus what the mark means. */
private fun detailOf(number: String, mark: AttendanceMark?): String = when (mark) {
    null -> "$number · Sin registrar"
    AttendanceMark.PRESENT -> number
    AttendanceMark.LATE -> "$number · Retardo"
    AttendanceMark.ABSENT -> "$number · Falta"
    AttendanceMark.JUSTIFIED -> "$number · Justificado"
}

private const val NO_SUBJECT = "Sin materia asignada"

/**
 * What the strip under the counters says for each sync phase.
 *
 * `null` means "nothing saved yet", which is what keeps this from overwriting the
 * validation messages (missing marks, local write failure) that happen before a
 * session exists.
 */
fun rollCallSyncMessage(sync: RollCallSync?): String? = when (sync) {
    RollCallSync.PENDING ->
        "Asistencia guardada en el dispositivo. Pendiente de sincronizar."

    RollCallSync.SYNCING -> "Sincronizando la asistencia…"

    RollCallSync.SYNCED -> "Asistencia guardada y sincronizada."

    RollCallSync.FAILED ->
        "Asistencia guardada en el dispositivo. No se pudo sincronizar: se reintenta " +
            "automáticamente."

    null -> null
}
