package com.example.aulasegura.data.remote.remoteconfig

/**
 * Typed reads of the Remote Config values the app is allowed to change remotely.
 *
 * Everything is validated, not just cast. Remote Config is edited from a console
 * by a human, so the realistic failure is not "no value" but "a value in the
 * wrong unit": a radius of `0` or an `incident_media_max` of `0` would disable a
 * feature while looking like a successful configuration. Out-of-range values fall
 * back to the documented default and never reach the UI.
 */
class RemoteConfigRepository(
    private val source: RemoteConfigSource,
) {

    /**
     * Pulls the current configuration and activates it.
     *
     * Best effort by design: a failed refresh keeps whatever was last activated,
     * which is the whole point of shipping defaults. `false` means "the app is
     * running on cached or default values", not "the app is broken".
     */
    suspend fun refresh(): Boolean = source.fetchAndActivate()

    /** Default search radius for attendance, in metres. */
    fun defaultRadiusMeters(): Int =
        int(RemoteConfigKeys.DEFAULT_RADIUS_METERS, RemoteConfigKeys.FALLBACK_RADIUS_METERS) {
            it in MIN_RADIUS_METERS..MAX_RADIUS_METERS
        }

    /** Whether the tutor-facing assistant is visible at all. */
    fun isAiAssistantEnabled(): Boolean =
        bool(RemoteConfigKeys.AI_ASSISTANT_ENABLED, RemoteConfigKeys.FALLBACK_AI_ASSISTANT_ENABLED)

    /** Whether the app should show the maintenance screen instead of its content. */
    fun isMaintenanceMode(): Boolean =
        bool(RemoteConfigKeys.MAINTENANCE_MODE, RemoteConfigKeys.FALLBACK_MAINTENANCE_MODE)

    /** Lowest `versionCode` still allowed to run. */
    fun minAppVersion(): Int =
        int(RemoteConfigKeys.MIN_APP_VERSION, RemoteConfigKeys.FALLBACK_MIN_APP_VERSION) { it >= 1 }

    /** How many media files one incident can carry. */
    fun incidentMediaMax(): Int =
        int(RemoteConfigKeys.INCIDENT_MEDIA_MAX, RemoteConfigKeys.FALLBACK_INCIDENT_MEDIA_MAX) {
            it in 1..MAX_INCIDENT_MEDIA
        }

    /** True when this key was actually delivered by the console, not by the APK defaults. */
    fun isDeliveredFromConsole(key: String): Boolean = source.isFromRemote(key)

    /**
     * Reads an integer, falling back when the value is absent, unparseable or out
     * of [isValid].
     */
    private fun int(key: String, fallback: Int, isValid: (Int) -> Boolean): Int {
        val raw = source.getString(key)?.trim() ?: return fallback
        val parsed = raw.toIntOrNull() ?: return fallback
        return if (isValid(parsed)) parsed else fallback
    }

    private fun bool(key: String, fallback: Boolean): Boolean =
        when (source.getString(key)?.trim()?.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> fallback
        }

    private companion object {
        const val MIN_RADIUS_METERS = 20
        const val MAX_RADIUS_METERS = 5_000
        const val MAX_INCIDENT_MEDIA = 10
    }
}
