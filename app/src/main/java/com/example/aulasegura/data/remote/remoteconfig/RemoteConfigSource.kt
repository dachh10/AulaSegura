package com.example.aulasegura.data.remote.remoteconfig

import com.example.aulasegura.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * What [RemoteConfigRepository] needs from a Remote Config backend.
 *
 * [getString] answers `null` when the key has no value at all — neither from the
 * server nor from an installed default. That distinction matters: Firebase's
 * `getLong` returns `0` for a missing key, and a `0` leaking into
 * `incident_media_max` would block every upload without an error anywhere.
 */
interface RemoteConfigSource {

    fun getString(key: String): String?

    /** Whether the current value came from the server rather than a local default. */
    fun isFromRemote(key: String): Boolean

    /** Best effort: `true` when a fetch reached the backend. */
    suspend fun fetchAndActivate(): Boolean
}

/**
 * Production source.
 *
 * Defaults are installed once, at construction, so any read before the first
 * fetch still returns the documented value. `setDefaultsAsync` is fire and
 * forget: the repository validates every value anyway, which closes the tiny
 * window before the defaults land.
 */
class FirebaseRemoteConfigSource(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : RemoteConfigSource {

    init {
        // The production default is a 12 hour cache, which is right for users and
        // wrong for development: a console edit would take half a day to show up
        // and every verification would look broken. Debug builds fetch freely.
        if (BuildConfig.DEBUG) {
            remoteConfig.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build(),
            )
        }
        remoteConfig.setDefaultsAsync(RemoteConfigKeys.defaults)
    }

    override fun getString(key: String): String? {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            null
        } else {
            value.asString()
        }
    }

    override fun isFromRemote(key: String): Boolean =
        remoteConfig.getValue(key).source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE

    /**
     * Best effort: `true` when the fetch reached the backend.
     *
     * The `Boolean` that `fetchAndActivate` resolves is "a new config was
     * activated", which is also `false` when the console simply has no changes
     * since the last fetch. Conflating that with a failure would report every
     * quiet day as an outage, so the signal here is "the call did not throw".
     */
    override suspend fun fetchAndActivate(): Boolean = try {
        remoteConfig.fetchAndActivate().await()
        true
    } catch (e: Exception) {
        false
    }
}
