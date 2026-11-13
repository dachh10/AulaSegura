package com.example.aulasegura.data.remote.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Remote Config is edited by a human in a console, so the interesting cases are
 * the wrong ones: a missing key, a value in the wrong unit, a typo. Each one must
 * degrade to the documented default instead of reaching a screen as `0`.
 */
class RemoteConfigRepositoryTest {

    @Test
    fun `documented defaults apply when nothing has been configured`() {
        val repo = RemoteConfigRepository(FakeSource())

        assertEquals(150, repo.defaultRadiusMeters())
        assertTrue(repo.isAiAssistantEnabled())
        assertFalse(repo.isMaintenanceMode())
        assertEquals(1, repo.minAppVersion())
        assertEquals(3, repo.incidentMediaMax())
    }

    @Test
    fun `values published in the console win over the defaults`() {
        val repo = RemoteConfigRepository(
            FakeSource(
                values = mapOf(
                    RemoteConfigKeys.DEFAULT_RADIUS_METERS to "300",
                    RemoteConfigKeys.AI_ASSISTANT_ENABLED to "false",
                    RemoteConfigKeys.MAINTENANCE_MODE to "true",
                    RemoteConfigKeys.MIN_APP_VERSION to "4",
                    RemoteConfigKeys.INCIDENT_MEDIA_MAX to "5",
                ),
            ),
        )

        assertEquals(300, repo.defaultRadiusMeters())
        assertFalse(repo.isAiAssistantEnabled())
        assertTrue(repo.isMaintenanceMode())
        assertEquals(4, repo.minAppVersion())
        assertEquals(5, repo.incidentMediaMax())
    }

    @Test
    fun `an incident media limit of zero never blocks uploads`() {
        val repo = RemoteConfigRepository(
            FakeSource(values = mapOf(RemoteConfigKeys.INCIDENT_MEDIA_MAX to "0")),
        )

        assertEquals(3, repo.incidentMediaMax())
    }

    @Test
    fun `a radius outside the sane range falls back`() {
        listOf("0", "-50", "999999").forEach { raw ->
            val repo = RemoteConfigRepository(
                FakeSource(values = mapOf(RemoteConfigKeys.DEFAULT_RADIUS_METERS to raw)),
            )

            assertEquals("radius $raw must fall back", 150, repo.defaultRadiusMeters())
        }
    }

    @Test
    fun `unparseable numbers fall back instead of throwing`() {
        val repo = RemoteConfigRepository(
            FakeSource(
                values = mapOf(
                    RemoteConfigKeys.DEFAULT_RADIUS_METERS to "ciento cincuenta",
                    RemoteConfigKeys.MIN_APP_VERSION to "v1.0",
                ),
            ),
        )

        assertEquals(150, repo.defaultRadiusMeters())
        assertEquals(1, repo.minAppVersion())
    }

    @Test
    fun `booleans accept the spellings a console accepts`() {
        listOf("TRUE", "true", "1").forEach { raw ->
            val repo = RemoteConfigRepository(
                FakeSource(values = mapOf(RemoteConfigKeys.MAINTENANCE_MODE to raw)),
            )
            assertTrue("'$raw' must read as true", repo.isMaintenanceMode())
        }

        listOf("FALSE", "0").forEach { raw ->
            val repo = RemoteConfigRepository(
                FakeSource(values = mapOf(RemoteConfigKeys.AI_ASSISTANT_ENABLED to raw)),
            )
            assertFalse("'$raw' must read as false", repo.isAiAssistantEnabled())
        }
    }

    @Test
    fun `an unreadable boolean keeps the documented default`() {
        val repo = RemoteConfigRepository(
            FakeSource(values = mapOf(RemoteConfigKeys.AI_ASSISTANT_ENABLED to "quizá")),
        )

        assertTrue(repo.isAiAssistantEnabled())
    }

    @Test
    fun `reports whether a value came from the console`() {
        val repo = RemoteConfigRepository(
            FakeSource(
                values = mapOf(RemoteConfigKeys.DEFAULT_RADIUS_METERS to "300"),
                fromRemote = setOf(RemoteConfigKeys.DEFAULT_RADIUS_METERS),
            ),
        )

        assertTrue(repo.isDeliveredFromConsole(RemoteConfigKeys.DEFAULT_RADIUS_METERS))
        assertFalse(repo.isDeliveredFromConsole(RemoteConfigKeys.MAINTENANCE_MODE))
    }

    @Test
    fun `refresh reports the backend outcome instead of throwing`() = kotlinx.coroutines.runBlocking {
        assertTrue(RemoteConfigRepository(FakeSource(fetchWorks = true)).refresh())
        assertFalse(RemoteConfigRepository(FakeSource(fetchWorks = false)).refresh())
    }

    private class FakeSource(
        private val values: Map<String, String> = emptyMap(),
        private val fromRemote: Set<String> = emptySet(),
        private val fetchWorks: Boolean = true,
    ) : RemoteConfigSource {

        override fun getString(key: String): String? = values[key]

        override fun isFromRemote(key: String): Boolean = key in fromRemote

        override suspend fun fetchAndActivate(): Boolean = fetchWorks
    }
}
