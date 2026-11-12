package com.example.aulasegura.sync

/**
 * Operations the outbox can replay.
 *
 * The names are stored verbatim in `outbox.opType`, so they are part of the
 * on-disk contract: renaming a constant silently orphans every queued row.
 * They also match `CONTEXT.md` §8.2.
 */
enum class SyncOperation {
    UPSERT_ATTENDANCE_SESSION,
    UPSERT_INCIDENT,
    UPLOAD_INCIDENT_MEDIA,
    UPSERT_JUSTIFICATION,
    ;

    companion object {
        fun fromName(name: String): SyncOperation? = entries.firstOrNull { it.name == name }
    }
}
