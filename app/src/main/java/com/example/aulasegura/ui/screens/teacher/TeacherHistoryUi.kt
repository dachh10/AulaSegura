package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttachmentSyncState
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionRecord
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.util.AttendanceTimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * What the history screen renders, already resolved.
 *
 * The screen gets this and nothing else: no repository, no Firestore, no Room.
 * Grouping by date is a pure function so the buckets can be tested without a
 * clock, which is the only way to test them at all.
 */
data class TeacherHistoryUi(
    /** `0` is the current month, `-1` the previous one; never positive. */
    val monthOffset: Int = 0,
    /** `"Noviembre 2026"`, derived from [monthOffset]. */
    val monthLabel: String = "",
    val canGoNextMonth: Boolean = false,
    val sections: List<HistorySectionUi> = emptyList(),
    val incidentSections: List<IncidentSectionUi> = emptyList(),
    val publications: List<HistoryPublicationUi> = emptyList(),
    val refreshing: Boolean = true,
    val notice: String? = null,
    val expandedId: String? = null,
    val expandedIncidentId: String? = null,
    val detailLoading: Boolean = false,
    val totalSessions: Int = 0,
    val totalIncidents: Int = 0,
)


data class HistorySectionUi(
    val label: String,
    val sessions: List<HistorySessionUi>,
)

data class HistorySessionUi(
    val id: String,
    /** `"15"` — the big number of the date block. */
    val day: String,
    /** `"SÁB"` — the weekday under it. */
    val weekday: String,
    /** `"3° B · Matemáticas"`. */
    val title: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val justified: Int,
    val verified: Boolean?,
    val sync: RollCallSync,
    val expanded: Boolean,
    val records: List<HistoryRecordUi> = emptyList(),
    val recordsLoading: Boolean = false,
)

data class HistoryRecordUi(
    val number: String,
    val name: String,
    val mark: AttendanceMark?,
    val time: String?,
)

/** One bucket of the incident history: the same labels the roll calls use. */
data class IncidentSectionUi(
    val label: String,
    val incidents: List<HistoryIncidentUi>,
)

/**
 * One incident row.
 *
 * [severity] is `null` when the stored wire value is not one this build knows:
 * the chip disappears instead of printing a raw `"leve "` with a typo in it.
 *
 * [title] falls back to the first line of the description because that is how the
 * write path derives it; a document without a title still has something to print.
 */
data class HistoryIncidentUi(
    val id: String,
    val day: String,
    val weekday: String,
    val studentName: String,
    val title: String,
    val severity: String?,
    val category: String,
    val description: String,
    val status: String,
    val occurredLabel: String,
    val locationLabel: String?,
    val sync: SyncPhase,
    val mediaCount: Int,
    val expanded: Boolean,
    val evidence: List<HistoryEvidenceUi> = emptyList(),
)

/**
 * One task or notice the teacher published, as the "Publicaciones" segment shows
 * it.
 *
 * Both kinds share a row because the teacher is reading one timeline of what they
 * sent to families; splitting them into two lists would make them ask "where is
 * the notice I sent yesterday?" at the wrong tab.
 */
data class HistoryPublicationUi(
    val id: String,
    /** `"Tarea"`, `"Aviso"` or `"Aviso urgente"`. */
    val kindLabel: String,
    val title: String,
    /** Subject of a task, first line of a notice's body. */
    val detail: String,
    /** `"3° B"` or `"3° B · 4° A"`, already resolved against the roster. */
    val audience: String,
    val postedLabel: String,
    /** Still in the outbox: Firestore does not have it yet. */
    val pending: Boolean,
    val urgent: Boolean,
    /**
     * File name of the task's PDF, when it has one. The row prints a `PDF` chip
     * instead of the name — a 40-character file name would eat the card — so this
     * is only here for the icon's accessibility label.
     */
    val attachmentName: String? = null,
    val attachmentState: AttachmentSyncState? = null,
    /** Set only when the PDF reached Cloudinary: then the row can open it. */
    val attachmentUrl: String? = null,
    /** Only for ordering; the row prints [postedLabel]. */
    val createdAt: Long,
)

/**
 * One evidence entry of the expanded incident.
 *
 * [url] is the Cloudinary delivery URL: the local file was deleted once the
 * upload finished, so the URL is the only way back to the photo.
 */
data class HistoryEvidenceUi(
    val mediaId: String,
    val label: String,
    val url: String?,
    val phase: MediaUploadPhase,
)

/*
 * Months.
 *
 * The header used to print a fixed "Noviembre 2026" while the arrows did nothing,
 * so in December the screen would still claim November. A month is compared as
 * `year * 12 + month`, an int, which keeps "same month" a single equality in every
 * list and needs no `java.time`.
 */

/** `2026-11-17` in [zone] → `2026 * 12 + 10`. `null` when the date is unreadable. */
fun monthKeyOfIso(iso: String?, zone: TimeZone = TimeZone.getDefault()): Int? {
    val millis = iso?.let { AttendanceTimes.isoDateToMillis(it, zone) } ?: return null
    return monthKey(millis, zone)
}

/** The month of [epochMillis] as a comparable int. */
fun monthKey(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = epochMillis }
    return calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
}

/** The month [offset] months before [now]; `0` is the current one. */
fun monthKeyBefore(now: Long, offset: Int, zone: TimeZone = TimeZone.getDefault()): Int =
    monthKey(now, zone) + offset

/** `"Noviembre 2026"` for [offset] months before [now]. */
fun monthLabel(now: Long, offset: Int, zone: TimeZone = TimeZone.getDefault()): String {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = now
        add(Calendar.MONTH, offset)
    }
    val label = SimpleDateFormat("MMMM yyyy", Locale("es", "MX"))
        .apply { timeZone = zone }
        .format(calendar.time)

    return label.replaceFirstChar { it.uppercase(Locale("es", "MX")) }
}

/** How far forward the arrows may go: never past the current month. */
fun nextMonthOffset(current: Int): Int = minOf(current + 1, 0)

/**
 * Buckets the sessions by recency, newest first.
 *
 * [todayIso] is passed in instead of read from the clock so the grouping is
 * reproducible in a test. A session whose date cannot be parsed falls into
 * "ANTERIORES": showing it in the wrong bucket is better than hiding a saved
 * attendance because one document had a malformed date.
 */
fun historySections(
    sessions: List<AttendanceSessionSummary>,
    todayIso: String,
    expandedId: String? = null,
    detail: AttendanceSessionDetail? = null,
    detailLoading: Boolean = false,
): List<HistorySectionUi> {
    if (sessions.isEmpty()) return emptyList()

    val today = AttendanceTimes.isoDateToMillis(todayIso)
    val records = detail?.takeIf { it.summary.sessionId == expandedId }?.records.orEmpty()

    val buckets = linkedMapOf<String, MutableList<HistorySessionUi>>()
    sessions.forEach { session ->
        val label = bucketOf(session.date, today)
        val isOpen = session.sessionId == expandedId
        buckets.getOrPut(label) { mutableListOf() } += session.toUi(
            expanded = isOpen,
            // Only the open row gets the records: handing them to every session
            // would paint the same roster under each card.
            records = if (isOpen) records else emptyList(),
            recordsLoading = isOpen && detailLoading,
        )
    }

    // Fixed order, not insertion order: "HOY" must stay on top even when the
    // oldest session is the one that arrived last from the server.
    return SECTION_ORDER.mapNotNull { label ->
        buckets[label]?.let { HistorySectionUi(label = label, sessions = it) }
    }
}

/**
 * Buckets the teacher's incidents with the same recency labels as the roll calls.
 *
 * [todayIso] and [zone] are passed in so the grouping is reproducible in a test;
 * an unparseable date lands in "ANTERIORES" for the same reason it does in
 * [historySections] — a wrong bucket beats a hidden incident.
 *
 * Only the expanded row receives [evidence]: handing the files to every row would
 * paint the same photos under each card.
 */
fun incidentSections(
    incidents: List<IncidentSummary>,
    todayIso: String,
    expandedId: String? = null,
    evidence: List<IncidentEvidence> = emptyList(),
    zone: TimeZone = TimeZone.getDefault(),
): List<IncidentSectionUi> {
    if (incidents.isEmpty()) return emptyList()

    val today = AttendanceTimes.isoDateToMillis(todayIso, zone)
    val buckets = linkedMapOf<String, MutableList<HistoryIncidentUi>>()

    incidents.forEach { incident ->
        val iso = AttendanceTimes.isoDate(incident.occurredAt, zone)
        val label = bucketOf(iso, today)
        val isOpen = incident.incidentId == expandedId
        buckets.getOrPut(label) { mutableListOf() } += incident.toUi(
            expanded = isOpen,
            evidence = if (isOpen) evidence else emptyList(),
            zone = zone,
        )
    }

    return SECTION_ORDER.mapNotNull { label ->
        buckets[label]?.let { IncidentSectionUi(label = label, incidents = it) }
    }
}

/**
 * Merges the teacher's tasks and notices into one timeline, newest first.
 *
 * Pending is per row and not per list: a task written offline shows as pending
 * while the notice published before it already reached Firestore, and collapsing
 * both into one state would tell the teacher the wrong thing about one of them.
 */
fun publicationRows(
    tasks: List<PublishedTask>,
    notices: List<PublishedNotice>,
    zone: TimeZone = TimeZone.getDefault(),
): List<HistoryPublicationUi> {
    if (tasks.isEmpty() && notices.isEmpty()) return emptyList()

    val rows = tasks.map { task ->
        HistoryPublicationUi(
            id = task.taskId,
            kindLabel = "Tarea",
            title = task.title,
            detail = task.subject,
            audience = task.groupNames.joinToString(" · "),
            postedLabel = publicationTimestampLabel(task.createdAt, zone),
            pending = task.pending,
            urgent = false,
            attachmentName = task.attachmentName,
            attachmentState = task.attachmentState,
            attachmentUrl = task.attachmentUrl,
            createdAt = task.createdAt,
        )
    } + notices.map { notice ->
        HistoryPublicationUi(
            id = notice.noticeId,
            kindLabel = if (notice.urgent) "Aviso urgente" else "Aviso",
            title = notice.title,
            detail = notice.body.lineSequence().firstOrNull().orEmpty().trim(),
            audience = notice.groupNames.joinToString(" · "),
            postedLabel = publicationTimestampLabel(notice.createdAt, zone),
            pending = notice.pending,
            urgent = notice.urgent,
            createdAt = notice.createdAt,
        )
    }

    return rows.sortedByDescending { it.createdAt }
}

/**
 * The short state printed next to the `PDF` chip.
 *
 * It is evidence, not decoration: `Pendiente` means the file is still on this
 * phone and `No se subió` means the task went out without it. An uploaded file
 * needs no word: the row already says `Abrir`, which is only there when the URL
 * exists, so a successful upload is the one case that prints nothing extra.
 */
fun attachmentStateLabel(state: AttachmentSyncState?): String? = when (state) {
    AttachmentSyncState.UPLOADED -> null
    AttachmentSyncState.FAILED -> "No se subió"
    AttachmentSyncState.PENDING, null -> "Pendiente"
}

/** `"12 nov · 08:00"` — when the teacher published it, in local time. */
fun publicationTimestampLabel(
    epochMillis: Long,
    zone: TimeZone = TimeZone.getDefault(),
): String = SimpleDateFormat("d MMM · HH:mm", SPANISH_MX)
    .apply { timeZone = zone }
    .format(Date(epochMillis))
    .replace(".", "")

/** `"Pendiente de enviar"` / `"Publicado"` — the pill of one row. */
fun publicationSyncLabel(pending: Boolean): String =
    if (pending) "Pendiente de enviar" else "Publicado"

internal fun IncidentSummary.toUi(
    expanded: Boolean,
    evidence: List<IncidentEvidence>,
    zone: TimeZone,
): HistoryIncidentUi {
    val iso = AttendanceTimes.isoDate(occurredAt, zone)
    return HistoryIncidentUi(
        id = incidentId,
        day = iso.substringAfterLast('-').padStart(2, '0'),
        weekday = AttendanceTimes.weekdayLabel(iso, zone),
        studentName = studentName,
        title = title.ifBlank { description.lineSequence().firstOrNull().orEmpty() },
        severity = severity?.label,
        category = category?.label.orEmpty(),
        description = description,
        status = status.label,
        occurredLabel = incidentTimestampLabel(occurredAt, zone),
        locationLabel = incidentLocationLabel(locationVerified, locationDistanceMeters),
        sync = sync,
        mediaCount = mediaCount,
        expanded = expanded,
        evidence = evidence.map { it.toUi() },
    )
}

/**
 * `"Dentro de la escuela · 12 m"`, or `null` when no pin was captured.
 *
 * The distance is the pin's distance to the school, so it is printed only when it
 * exists; "verified" without a distance still says something useful.
 */
fun incidentLocationLabel(verified: Boolean?, distanceMeters: Double?): String? {
    val distance = distanceMeters?.takeIf { it.isFinite() }?.roundToInt()?.let { "$it m" }
    return when (verified) {
        true -> listOfNotNull("Dentro de la escuela", distance).joinToString(" · ")
        false -> listOfNotNull("Fuera del radio", distance).joinToString(" · ")
        null -> null
    }
}

internal fun IncidentEvidence.toUi(): HistoryEvidenceUi = HistoryEvidenceUi(
    mediaId = mediaId,
    label = evidenceLabel(format, bytes),
    url = url,
    phase = phase,
)

/** `"JPG · 200 KB"` — what the expanded row prints next to one file. */
fun evidenceLabel(format: String?, bytes: Long): String =
    listOfNotNull(format?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }, evidenceSizeLabel(bytes))
        .joinToString(" · ")

fun evidenceSizeLabel(bytes: Long): String = when {
    bytes <= 0L -> "tamaño desconocido"
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${(bytes / 1024.0).roundToInt()} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun bucketOf(date: String, todayMillis: Long?): String {
    val sessionMillis = AttendanceTimes.isoDateToMillis(date)
    if (todayMillis == null || sessionMillis == null) return OLDER

    val days = ((todayMillis - sessionMillis) / MILLIS_PER_DAY).toInt()
    return when {
        days <= 0 -> TODAY
        days < 7 -> THIS_WEEK
        days < 14 -> LAST_WEEK
        else -> OLDER
    }
}

internal fun AttendanceSessionSummary.toUi(
    expanded: Boolean,
    records: List<AttendanceSessionRecord>,
    recordsLoading: Boolean,
): HistorySessionUi = HistorySessionUi(
    id = sessionId,
    day = date.substringAfterLast('-').padStart(2, '0'),
    weekday = AttendanceTimes.weekdayLabel(date),
    title = listOf(groupLabel, subject.ifBlank { null }).filterNotNull().joinToString(" · "),
    present = counts.present,
    absent = counts.absent,
    late = counts.late,
    justified = counts.justified,
    verified = verified,
    sync = sync,
    expanded = expanded,
    records = records.map { it.toUi() },
    recordsLoading = recordsLoading,
)

internal fun AttendanceSessionRecord.toUi(): HistoryRecordUi = HistoryRecordUi(
    number = listNumber.toString().padStart(2, '0'),
    name = fullName,
    mark = mark,
    time = time,
)

const val TODAY = "HOY"
const val THIS_WEEK = "ESTA SEMANA"
const val LAST_WEEK = "SEMANA ANTERIOR"
const val OLDER = "ANTERIORES"

/** Fixed to Spanish because the whole UI is, whatever the device language is. */
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")

private val SECTION_ORDER = listOf(TODAY, THIS_WEEK, LAST_WEEK, OLDER)
private const val MILLIS_PER_DAY = 86_400_000L
