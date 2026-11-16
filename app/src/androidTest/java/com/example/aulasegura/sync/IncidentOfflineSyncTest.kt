package com.example.aulasegura.sync

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentLocation
import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.RosterRefreshResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The block 6 criterion, on the device and against the real project:
 *
 * 1. With the radio actually off (airplane mode, not a mocked flag), the incident
 *    becomes a Room draft plus one outbox row.
 * 2. With the radio back on, the sync runner drains that row and the document is
 *    written to Firestore.
 * 3. The document is there **exactly once**, with severity, student name,
 *    location and the chosen category.
 * 4. A second pass over the same incident does not create a second document.
 *
 * Nothing here is stubbed: the roster comes from the seeded project, the write
 * goes through the same container the app uses, and the read uses
 * `Source.SERVER` so a cached local echo cannot pass as a synced document.
 *
 * Incidents cannot be deleted from the client (`allow delete: if false`), so this
 * test leaves one real document in `incidents/` — the incident id is printed on
 * failure and in the report.
 */
@RunWith(AndroidJUnit4::class)
class IncidentOfflineSyncTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private var createdIncidentId: String? = null

    @Before
    fun signInAndWarmTheRoster() = runBlocking {
        setAirplaneMode(enabled = false)
        awaitConnectivity(online = true)

        // Through the container, not FirebaseAuth directly: `signIn` is what
        // resolves `users/{uid}` and leaves `schoolId` in the session, and every
        // repository below depends on that profile.
        val session = app.container.authRepository.signIn(DOCENTE_EMAIL, DOCENTE_PASSWORD)
        assertTrue("sign-in failed: $session", session is AuthResult.Success)

        // Cache first, while there is still network: the offline phase below must
        // not need a single Firestore read to resolve the group or the student.
        val refreshed = app.container.groupRepository.refreshMyGroups()
        assertTrue("could not warm the roster: $refreshed", refreshed is RosterRefreshResult.Updated)
    }

    @After
    fun cleanUp() = runBlocking {
        setAirplaneMode(enabled = false)
        // Both halves of the local bookkeeping go, not just the draft: a run that
        // fails before the drain leaves an outbox row the worker keeps retrying
        // long after the test is gone. `deleteByEntity` matches the entity id the
        // row carries.
        createdIncidentId?.let { id ->
            app.container.database.outboxDao().deleteByEntity(id)
            app.container.database.incidentDao().deleteDraft(id)
        }
        app.container.authRepository.signOut()
    }

    @Test
    fun incidentCreatedInAirplaneModeReachesFirestoreExactlyOnce() = runBlocking {
        val container = app.container
        val group = requireNotNull(
            withTimeoutOrNull(ROSTER_TIMEOUT_MILLIS) {
                container.groupRepository.observeMyGroups().first().firstOrNull()
            },
        ) {
            "the teacher has no cached groups; the seeded project is not reachable"
        }
        val student = requireNotNull(
            withTimeoutOrNull(ROSTER_TIMEOUT_MILLIS) {
                container.studentRepository.observeRoster(group.id).first().firstOrNull()
            },
        ) {
            "group ${group.id} has no cached roster"
        }

        // ---- Offline ---------------------------------------------------------
        setAirplaneMode(enabled = true)
        awaitConnectivity(online = false)

        val incident = NewIncident(
            groupId = group.id,
            studentId = student.id,
            studentName = student.fullName,
            title = "Prueba de sincronización offline (bloque 6)",
            description = "Prueba automática del bloque 6: incidente creado en modo avión.",
            severity = IncidentSeverity.GRAVE,
            category = IncidentCategory.CAIDA,
            occurredAt = System.currentTimeMillis(),
            location = IncidentLocation(
                latitude = PIN_LAT,
                longitude = PIN_LNG,
                distanceMeters = 0.0,
                verified = true,
            ),
        )

        val queued = container.createIncidentUseCase(incident) as? IncidentSaveOutcome.Queued
            ?: error("the offline incident was not queued: check the repository outcome")
        val incidentId = queued.incidentId
        createdIncidentId = incidentId

        val dao = container.database.incidentDao()
        assertEquals(
            "a queued incident must be PENDING on this device",
            SyncState.PENDING,
            requireNotNull(dao.findDraft(incidentId)).syncState,
        )
        assertTrue(
            "no UPSERT_INCIDENT row for the incident",
            outboxRow(incidentId) != null,
        )
        // No Firestore read here on purpose: with the radio off a `Source.SERVER`
        // query throws, and a cached read would only tell us about the SDK cache.
        // The offline evidence is the PENDING draft plus the queued row.

        // ---- Back online, drain the queue like the worker does ------------------
        setAirplaneMode(enabled = false)
        awaitConnectivity(online = true)
        drainQueue()

        assertEquals(
            "the handler must mark the draft SYNCED",
            SyncState.SYNCED,
            requireNotNull(dao.findDraft(incidentId)).syncState,
        )
        assertEquals("the row must leave the outbox", null, outboxRow(incidentId))

        // ---- The document, read from the server --------------------------------
        val documents = serverDocuments(incidentId)
        assertEquals("the incident must exist exactly once", 1, documents.size)

        val document = documents.single()
        assertEquals("the document id is the client UUID", incidentId, document.id)
        assertEquals("grave", document.getString("severity"))
        assertEquals("caida", document.getString("category"))
        assertEquals(student.fullName, document.getString("studentName"))
        assertEquals(student.id, document.getString("studentId"))
        assertEquals("nuevo", document.getString("status"))
        assertEquals(group.id, document.getString("groupId"))
        assertEquals(requireNotNull(auth.currentUser).uid, document.getString("reportedBy"))
        assertTrue(
            "reportedByName must be denormalised for the tutor",
            !document.getString("reportedByName").isNullOrBlank(),
        )

        val location = requireNotNull(document.get("location") as? Map<*, *>) {
            "the incident was saved without a location"
        }
        assertEquals(PIN_LAT, location["lat"] as Double, COORDINATE_TOLERANCE)
        assertEquals(PIN_LNG, location["lng"] as Double, COORDINATE_TOLERANCE)
        assertEquals(true, location["verified"])

        // ---- Replaying the same incident stays idempotent ----------------------
        container.outboxRepository.enqueueIncident(requireNotNull(dao.findDraft(incidentId)))
        drainQueue()

        assertEquals(
            "a replay must overwrite, not duplicate",
            1,
            serverDocuments(incidentId).size,
        )
    }

    /** Runs the same pass the worker runs, until the queue is empty. */
    private suspend fun drainQueue() {
        val runner = SyncRunner(
            outboxDao = app.container.database.outboxDao(),
            registry = app.container.syncHandlers,
            reporter = app.container.syncFailureReporter,
            analytics = app.container.analyticsLogger,
        )

        val deadline = SystemClock.uptimeMillis() + SYNC_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            val result = runner.run()
            if (result is SyncRunResult.Done) return
            delay(RETRY_PAUSE_MILLIS)
        }
        error("the outbox did not drain within ${SYNC_TIMEOUT_MILLIS}ms")
    }

    private suspend fun outboxRow(incidentId: String): OutboxEntity? = app.container.database
        .outboxDao()
        .peek(OUTBOX_SCAN_LIMIT)
        .firstOrNull { it.entityLocalId == incidentId }

    /**
     * Reads the incident back from the server, filtering the way the rules require.
     *
     * `reportedBy == uid` is not decoration: the rule allows the teacher to read
     * their own incidents, and Firestore can only prove that for a query that
     * filters on it. Without the filter the whole query is rejected, which is the
     * same trap any future "mis incidentes" list has to avoid.
     */
    private suspend fun serverDocuments(incidentId: String): List<DocumentSnapshot> {
        val uid = requireNotNull(auth.currentUser) { "not signed in" }.uid
        return firestore.collection("incidents")
            .whereEqualTo("reportedBy", uid)
            .whereEqualTo("incidentId", incidentId)
            .get(Source.SERVER)
            .await()
            .documents
    }

    private fun setAirplaneMode(enabled: Boolean) {
        val command = "cmd connectivity airplane-mode " + if (enabled) "enable" else "disable"
        instrumentation.uiAutomation.executeShellCommand(command).close()
    }

    /**
     * Waits for the radio, not for a flag: a device can report airplane mode and
     * still keep Wi-Fi up, and in that case the "offline" half of this test would
     * be proving nothing.
     */
    private fun awaitConnectivity(online: Boolean) {
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val deadline = SystemClock.uptimeMillis() + CONNECTIVITY_TIMEOUT_MILLIS

        while (SystemClock.uptimeMillis() < deadline) {
            val capabilities = connectivity.activeNetwork?.let {
                connectivity.getNetworkCapabilities(it)
            }
            val validated = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            ) == true
            if (validated == online) return
            SystemClock.sleep(CONNECTIVITY_POLL_MILLIS)
        }

        error("the device never went " + if (online) "online" else "offline")
    }

    private companion object {
        const val DOCENTE_EMAIL = "docente@aulasegura.com"
        const val DOCENTE_PASSWORD = "123456"

        /** The pin the screen starts from, i.e. the school's courtyard. */
        const val PIN_LAT = 19.2926
        const val PIN_LNG = -99.6557
        const val COORDINATE_TOLERANCE = 0.000001

        const val ROSTER_TIMEOUT_MILLIS = 15_000L
        const val CONNECTIVITY_TIMEOUT_MILLIS = 30_000L
        const val CONNECTIVITY_POLL_MILLIS = 500L
        const val SYNC_TIMEOUT_MILLIS = 90_000L
        const val RETRY_PAUSE_MILLIS = 2_000L
        const val OUTBOX_SCAN_LIMIT = 200
    }
}
