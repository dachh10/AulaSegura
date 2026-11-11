package com.example.aulasegura.data.local.entity

/**
 * Lifecycle of a locally written row that still owes Firestore a write.
 *
 * Kept as an enum instead of a string constant so the compiler catches typos in
 * the sync layer; [com.example.aulasegura.data.local.Converters] persists the
 * name so the stored value stays readable in a database dump.
 */
enum class SyncState {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}

/**
 * Lifecycle of an incident media file that still owes Cloudinary an upload.
 */
enum class UploadState {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
}
