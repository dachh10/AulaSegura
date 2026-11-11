package com.example.aulasegura.data.local

import androidx.room.TypeConverter
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState

/**
 * Room type converters for the handful of shapes SQLite does not have.
 *
 * Timestamps are **not** converted here: entities store epoch milliseconds as
 * `Long` directly, and `data/local/mapper/TimestampMappers.kt` converts to and
 * from `com.google.firebase.Timestamp` when talking to Firestore. Keeping the
 * Firebase type out of the entities is what lets the database be tested without
 * a Firebase session.
 */
class Converters {

    /**
     * `string[]` fields are joined with the ASCII unit separator (`U+001F`),
     * which cannot appear in a group name, a subject or an id typed by a human.
     * An empty list round-trips as an empty string.
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter
    fun fromUploadState(value: UploadState): String = value.name

    @TypeConverter
    fun toUploadState(value: String): UploadState = UploadState.valueOf(value)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}
