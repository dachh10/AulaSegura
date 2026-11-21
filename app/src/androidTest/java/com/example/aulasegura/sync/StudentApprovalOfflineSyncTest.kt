package com.example.aulasegura.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.os.SystemClock
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.repository.CreateStudentResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end proof of T4b: a QR approval taken with no signal stays PENDING in
 * Room, then the real SyncRunner writes the student and the tutor profile links
 * to Firestore exactly once when the signal returns.
 *
 * Uses the director account already present in the project; the local student
 * and its outbox row are removed on teardown so the test is repeatable.
 */
@RunWith(AndroidJUnit4::class)
class StudentApprovalOfflineSyncTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()
    private val firestore = FirebaseFirestore.getInstance()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private var studentId: String? = null

    @Before
    fun signInAsDirector() = runBlocking {
        setAirplaneMode(enabled = false)
        awaitConnectivity(online = true)
        val session = app.container.authRepository.signIn(DIRECTOR_EMAIL, DIRECTOR_PASSWORD)
        assertTrue("director sign-in failed: $session", session is com.example.aulasegura.domain.repository.AuthResult.Success)
    }

    @After
    fun cleanUp() = runBlocking {
        setAirplaneMode(enabled = false)
        studentId?.let { id ->
            app.container.database.outboxDao().deleteByEntity(id)
            app.container.database.studentDao().findById(id)?.let {
                app.container.database.studentDao().updateSyncStatus(id, SyncState.SYNCED)
            }
        }
        app.container.authRepository.signOut()
    }

    @Test
    fun approvalQueuedInAirplaneModeReachesFirestoreWhenBackOnline() = runBlocking {
        val container = app.container
        val director = checkNotNull(container.authRepository.currentUser)
        val approval = PendingStudentApproval(
            studentId = "stu-qr-offline-${System.currentTimeMillis()}",
            alumno = "Alumno Offline Prueba",
            tutorUid = TUTOR_UID,
            schoolId = director.schoolId,
            createdBy = director.uid,
            grado = "3",
            grupo = "B",
            groupId = "grp-3b",
        )
        studentId = approval.studentId

        // Offline: queue the approval.
        setAirplaneMode(enabled = true)
        awaitConnectivity(online = false)
        container.outboxRepository.enqueueStudentApproval(
            student = com.example.aulasegura.data.local.entity.StudentEntity.fromApproval(
                studentId = approval.studentId,
                alumno = approval.alumno,
                tutorUid = approval.tutorUid,
                groupId = approval.groupId,
                schoolId = approval.schoolId,
                grado = approval.grado,
                grupo = approval.grupo,
                creadoPor = approval.createdBy,
                now = System.currentTimeMillis(),
            ),
            approval = approval,
        )

        val pending = container.database.studentDao().findById(approval.studentId)
        assertNotNull(pending)
        assertEquals(SyncState.PENDING, pending!!.syncStatus)
        assertNotNull(
            container.database.outboxDao().peek(10)
                .firstOrNull { it.entityLocalId == approval.studentId },
        )

        // Back online: the same pass the worker runs.
        setAirplaneMode(enabled = false)
        awaitConnectivity(online = true)
        drainQueue()

        val synced = container.database.studentDao().findById(approval.studentId)
        assertEquals(SyncState.SYNCED, synced?.syncStatus)
        assertEquals(
            0,
            container.database.outboxDao().peek(10)
                .count { it.entityLocalId == approval.studentId },
        )

        val server = firestore.collection("students")
            .document(approval.studentId).get(Source.SERVER).await()
        assertTrue("student never reached Firestore", server.exists())
        assertEquals(
            approval.tutorUid,
            (server.get("tutorIds") as? List<*>)?.firstOrNull(),
        )

        val tutor = firestore.collection("users")
            .document(approval.tutorUid).get(Source.SERVER).await()
        assertTrue(
            "tutor links were not written",
            (tutor.get("studentIds") as? List<*>)?.contains(approval.studentId) == true,
        )
        assertEquals("vinculado", tutor.getString("vinculacion"))
    }

    private suspend fun drainQueue() {
        val runner = SyncRunner(
            outboxDao = app.container.database.outboxDao(),
            registry = app.container.syncHandlers,
            reporter = app.container.syncFailureReporter,
            analytics = app.container.analyticsLogger,
        )
        val deadline = SystemClock.uptimeMillis() + SYNC_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            if (runner.run() is SyncRunResult.Done) return
            delay(RETRY_PAUSE_MILLIS)
        }
        error("the outbox did not drain within ${SYNC_TIMEOUT_MILLIS}ms")
    }

    private fun setAirplaneMode(enabled: Boolean) {
        val command = "cmd connectivity airplane-mode " + if (enabled) "enable" else "disable"
        instrumentation.uiAutomation.executeShellCommand(command).close()
    }

    private fun awaitConnectivity(online: Boolean) {
        val connectivity = app.getSystemService(android.net.ConnectivityManager::class.java)
        val deadline = SystemClock.uptimeMillis() + CONNECTIVITY_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            val capabilities = connectivity.activeNetwork?.let {
                connectivity.getNetworkCapabilities(it)
            }
            val validated = capabilities?.hasCapability(
                android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            ) == true
            if (validated == online) return
            SystemClock.sleep(CONNECTIVITY_POLL_MILLIS)
        }
        error("the device never went " + if (online) "online" else "offline")
    }

    private companion object {
        const val DIRECTOR_EMAIL = "directora@escuela.mx"
        const val DIRECTOR_PASSWORD = "AulaSegura#2024"
        const val TUTOR_UID = "tutor-prueba-qr"
        const val SYNC_TIMEOUT_MILLIS = 60_000L
        const val CONNECTIVITY_TIMEOUT_MILLIS = 15_000L
        const val CONNECTIVITY_POLL_MILLIS = 250L
        const val RETRY_PAUSE_MILLIS = 500L
    }
}
