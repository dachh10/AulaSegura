package com.example.aulasegura.data.remote.remoteconfig

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.AulaSeguraApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves that the five keys are published in the console — not just that the app
 * can read its own defaults.
 *
 * That distinction is the whole point of I3: reading a default proves the code
 * compiles, while [RemoteConfigRepository.isDeliveredFromConsole] proves the
 * value actually travelled from Firebase.
 */
@RunWith(AndroidJUnit4::class)
class RemoteConfigInstrumentedTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()

    @Test
    fun theFiveDocumentedKeysAreDeliveredByTheConsole() = runBlocking {
        val repo = app.container.remoteConfigRepository

        val missing = fetchUntilDelivered(repo, KEYS, timeoutMillis = 30_000)

        assertTrue(
            "These keys did not arrive from the Firebase console, so the app is " +
                "running on its APK defaults: $missing. Check that the template is " +
                "published and that the device has internet.",
            missing.isEmpty(),
        )

        // Reading them must also be safe, and a published value must still land in
        // a usable range: a zero here would disable attendance radius or incident
        // media without any error.
        assertTrue(repo.defaultRadiusMeters() >= 20)
        assertTrue(repo.minAppVersion() >= 1)
        assertTrue(repo.incidentMediaMax() in 1..10)
    }

    @Test
    fun defaultsAreReadableWithoutAnyFetch() {
        // A repository whose source has never fetched anything must still answer
        // with the documented values: that is the offline first launch.
        val offline = RemoteConfigRepository(EmptySource())

        assertEquals(150, offline.defaultRadiusMeters())
        assertTrue(offline.isAiAssistantEnabled())
        assertFalse(offline.isMaintenanceMode())
        assertEquals(1, offline.minAppVersion())
        assertEquals(3, offline.incidentMediaMax())
    }

    /**
     * Retries inside a bounded window.
     *
     * `AulaSeguraApp.onCreate` already starts a fetch, so the first call here can
     * lose a race against it ("fetch already in progress"). Waiting for the values
     * to arrive is the honest assertion; failing on the first attempt would just
     * measure scheduling luck.
     */
    private suspend fun fetchUntilDelivered(
        repo: RemoteConfigRepository,
        keys: List<String>,
        timeoutMillis: Long,
    ): List<String> {
        val deadline = System.currentTimeMillis() + timeoutMillis

        while (true) {
            val missing = keys.filterNot(repo::isDeliveredFromConsole)
            if (missing.isEmpty()) return emptyList()
            if (System.currentTimeMillis() >= deadline) return missing

            repo.refresh()
            delay(1_000)
        }
    }

    private class EmptySource : RemoteConfigSource {
        override fun getString(key: String): String? = null
        override fun isFromRemote(key: String): Boolean = false
        override suspend fun fetchAndActivate(): Boolean = false
    }

    private companion object {
        val KEYS = listOf(
            RemoteConfigKeys.DEFAULT_RADIUS_METERS,
            RemoteConfigKeys.AI_ASSISTANT_ENABLED,
            RemoteConfigKeys.MAINTENANCE_MODE,
            RemoteConfigKeys.MIN_APP_VERSION,
            RemoteConfigKeys.INCIDENT_MEDIA_MAX,
        )
    }
}
