package com.example.aulasegura.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The whole justification, stored as JSON inside the outbox row's `payloadJson`.
 *
 * Why the payload and not a table: a justification is **write only** — no tutor
 * screen reads one back — so a Room entity would add a migration and a row
 * nobody reads. The outbox row already survives restarts, holds the local file
 * path while the upload is pending and keeps the Cloudinary URL once it exists.
 *
 * The document id is the outbox row's `entityLocalId` (a UUID generated when the
 * tutor sends), so replaying the row overwrites the same document instead of
 * filing the absence twice.
 */
@Serializable
data class JustificationPayload(
    val studentId: String,
    val tutorId: String,
    val schoolId: String,
    val groupId: String,
    /** Local midnight of the absence; the schema keeps a range, the form one day. */
    val dateFromMillis: Long,
    val dateToMillis: Long,
    /** One of `enfermedad | cita | viaje | otro` (`CONTEXT.md` §6). */
    val reason: String,
    val description: String,
    val createdAtMillis: Long,
    val attachment: StoredJustificationAttachment? = null,
    /**
     * Session the absence belongs to. Optional so payloads queued by older
     * builds still decode; the principal then resolves the session by group and
     * date.
     */
    val sessionId: String = "",
    /**
     * Display data for the inbox. The principal and the teacher have no roster
     * open when they read the justification, and a document id is not a name.
     */
    val studentName: String = "",
    val groupLabel: String = "",
)

/**
 * The single file of a justification, and where it is on its way to Cloudinary.
 *
 * `PENDING` → the worker still has to upload it. `UPLOADED` → the URL is final.
 * `FAILED` → the file is gone or the account refused it, and the document is
 * created without evidence instead of losing the tutor's report.
 */
@Serializable
data class StoredJustificationAttachment(
    val fileName: String,
    val bytes: Long,
    val mimeType: String,
    /** Local copy; only while the upload has not finished. */
    val localPath: String? = null,
    val publicId: String? = null,
    val url: String? = null,
    val state: String = PENDING,
) {

    val isUploaded: Boolean get() = state == UPLOADED && !url.isNullOrBlank()

    /** `image` | `pdf`, the field the reader uses to decide how to open it. */
    val type: String get() = if (mimeType.startsWith(IMAGE_PREFIX)) "image" else "pdf"

    companion object {
        const val PENDING = "PENDING"
        const val UPLOADED = "UPLOADED"
        const val FAILED = "FAILED"

        private const val IMAGE_PREFIX = "image/"
    }
}

/**
 * Tolerant on purpose: a row written by a future build must not crash the
 * worker. An unreadable payload is reported as `null` and the row is dropped
 * with a permanent failure instead of being retried forever.
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeJustificationPayload(payload: JustificationPayload): String =
    runCatching { json.encodeToString(payload) }.getOrDefault("")

fun decodeJustificationPayload(raw: String): JustificationPayload? =
    runCatching { json.decodeFromString<JustificationPayload>(raw) }.getOrNull()

/**
 * The `attachment` map of the document: only what the school needs to open the
 * file. The name and the size stay on the phone; `CONTEXT.md` §6 fixes exactly
 * `{ publicId, url, type }`.
 */
fun StoredJustificationAttachment.toFirestoreMap(): Map<String, Any?> = mapOf(
    "publicId" to publicId,
    "url" to url,
    "type" to type,
)

/**
 * The `justifications/{justificationId}` document, ready for `set()`.
 *
 * `status` is always `pendiente`: the tutor creates and only the principal may
 * change it (`firestore.rules`), so sending anything else would be refused. The
 * evidence is attached only when Cloudinary already accepted it — a document
 * without `attachment` is the honest result of a file that could not travel,
 * never a placeholder URL.
 */
fun JustificationPayload.toDocument(): Map<String, Any?> = buildMap {
    put("studentId", studentId)
    put("tutorId", tutorId)
    put("schoolId", schoolId)
    put("groupId", groupId)
    if (sessionId.isNotBlank()) put("sessionId", sessionId)
    if (studentName.isNotBlank()) put("studentName", studentName)
    if (groupLabel.isNotBlank()) put("groupLabel", groupLabel)
    put("dateFrom", Timestamp(Date(dateFromMillis)))
    put("dateTo", Timestamp(Date(dateToMillis)))
    put("reason", reason)
    put("description", description)
    put("status", STATUS_PENDING)
    put("createdAt", FieldValue.serverTimestamp())
    attachment?.takeIf { it.isUploaded }?.let { put("attachment", it.toFirestoreMap()) }
}

/** The only state a tutor can write; the principal owns the outcome. */
const val STATUS_PENDING = "pendiente"
