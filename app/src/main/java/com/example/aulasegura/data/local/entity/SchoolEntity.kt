package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of `schools/{schoolId}`.
 *
 * Only the fields the geofence needs are mirrored, plus the name for the map
 * marker. Everything else in the document (`key`, `cycle`, `schedule`) is
 * admin-facing and has no consumer on the teacher path; caching it would be
 * another column to migrate for nothing.
 *
 * `latitude`/`longitude` are nullable because the document can exist without a
 * `location` — a school configured before the map was set up. The mapper turns
 * that into `null` site, which is evaluated as "cannot verify", never as
 * "outside".
 */
@Entity(tableName = "schools")
data class SchoolEntity(
    @PrimaryKey val schoolId: String,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double? = null,
    val updatedAt: Long,
    // Declared last so it matches the column MIGRATION_3_4 appends. The default
    // keeps an old cached row (and every caller that omits it) fail-closed.
    val requireLocation: Boolean = true,
    // Appended by MIGRATION_6_7. Nullable so a cached row from an older version
    // means "no configured window" instead of locking the teacher out.
    val entryTime: String? = null,
    val exitTime: String? = null,
)
