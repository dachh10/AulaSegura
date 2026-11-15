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
)
