package com.example.aulasegura.data.telemetry

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.domain.repository.AuthResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives a real sign-in so the `login_success` event reaches the Analytics SDK
 * through the production wiring, not through a stub.
 *
 * The assertion is on the sign-in, because Analytics itself has no read-back API
 * on device: the proof that the event was accepted is the `FA` line in logcat,
 * which is collected from the host after this run. What this test rules out is the
 * more common failure — a logger that throws and breaks the flow it instruments.
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsDeliveryTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()

    @Before
    fun signOutFirst() = runBlocking {
        // A previous test may have left a session, and `signIn` is what emits the
        // event; an already-signed-in FirebaseAuth would hide that path.
        app.container.authRepository.signOut()
    }

    @After
    fun cleanUp() = runBlocking {
        app.container.authRepository.signOut()
    }

    @Test
    fun aRealSignInSucceedsWithAnalyticsWiredIn() = runBlocking {
        val result = app.container.authRepository.signIn(DOCENTE_EMAIL, DOCENTE_PASSWORD)

        assertTrue("sign-in failed: $result", result is AuthResult.Success)
    }

    @Test
    fun loggingEveryEventNeverThrows() {
        val logger = app.container.analyticsLogger

        // One of each shape: no params, a string, a boolean-as-long and a number.
        logger.log(com.example.aulasegura.domain.telemetry.AnalyticsEvent.RollCallStarted)
        logger.log(
            com.example.aulasegura.domain.telemetry.AnalyticsEvent.LoginSuccess(
                com.example.aulasegura.domain.model.UserRole.TEACHER,
            ),
        )
        logger.log(com.example.aulasegura.domain.telemetry.AnalyticsEvent.RollCallSaved(synced = false))
        logger.log(
            com.example.aulasegura.domain.telemetry.AnalyticsEvent.SyncFailed(
                "UPSERT_ATTENDANCE_SESSION",
                2,
            ),
        )
    }

    private companion object {
        const val DOCENTE_EMAIL = "docente@aulasegura.com"
        const val DOCENTE_PASSWORD = "123456"
    }
}
