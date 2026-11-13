package com.example.aulasegura.data.remote.remoteconfig

/**
 * The five Remote Config keys the app reads, with their documented defaults.
 *
 * The defaults are duplicated here on purpose. They are the values that ship
 * inside the APK, so a console outage, a first launch without network and a key
 * someone forgot to create all degrade to the same known behaviour instead of a
 * zero (CONTEXT §9).
 */
object RemoteConfigKeys {

    const val DEFAULT_RADIUS_METERS = "default_radius_meters"
    const val AI_ASSISTANT_ENABLED = "ai_assistant_enabled"
    const val MAINTENANCE_MODE = "maintenance_mode"
    const val MIN_APP_VERSION = "min_app_version"
    const val INCIDENT_MEDIA_MAX = "incident_media_max"

    /** Fallback values, exactly as documented in CONTEXT §9. */
    const val FALLBACK_RADIUS_METERS: Int = 150
    const val FALLBACK_AI_ASSISTANT_ENABLED: Boolean = true
    const val FALLBACK_MAINTENANCE_MODE: Boolean = false
    const val FALLBACK_MIN_APP_VERSION: Int = 1
    const val FALLBACK_INCIDENT_MEDIA_MAX: Int = 3

    /** Installed with `setDefaultsAsync`, so a fetch is never the first source. */
    val defaults: Map<String, Any> = mapOf(
        DEFAULT_RADIUS_METERS to FALLBACK_RADIUS_METERS,
        AI_ASSISTANT_ENABLED to FALLBACK_AI_ASSISTANT_ENABLED,
        MAINTENANCE_MODE to FALLBACK_MAINTENANCE_MODE,
        MIN_APP_VERSION to FALLBACK_MIN_APP_VERSION,
        INCIDENT_MEDIA_MAX to FALLBACK_INCIDENT_MEDIA_MAX,
    )
}
