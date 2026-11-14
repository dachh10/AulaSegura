package com.example.aulasegura.data.remote.ai

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AuthResult
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves J3: a real query leaves the device and comes back answered.
 *
 * The session is opened first on purpose. In the app the tutor only reaches the
 * assistant after signing in, and the SDK attaches that user's token to the
 * request — asking while signed out would test a path the product never takes.
 *
 * This is the only test in the suite that costs quota, so it asks one short
 * question and asserts the shape of the answer instead of its wording: a model
 * is free to paraphrase, and asserting prose would break every time it does.
 *
 * The `Log.i` breadcrumbs are not decoration: when the provider stalls, the
 * failure is a timeout with no stack worth reading, so the only way to tell
 * "sign-in hung" from "the model never answered" is the last line printed.
 */
@RunWith(AndroidJUnit4::class)
class AiAssistantInstrumentedTest {

    private val app: AulaSeguraApp = ApplicationProvider.getApplicationContext()

    @Before
    fun openSession(): Unit = runBlocking {
        Log.i(TAG, "session: signing in")
        val result = app.container.authRepository.signIn(DOCENTE_EMAIL, DOCENTE_PASSWORD)
        Log.i(TAG, "session: $result")
        assertTrue("sign-in failed: $result", result is AuthResult.Success)
    }

    @After
    fun closeSession(): Unit = runBlocking {
        app.container.authRepository.signOut()
        Log.i(TAG, "session: signed out")
    }

    @Test
    fun theAssistantAnswersARealQuestion() = runBlocking {
        Log.i(TAG, "ask: starting, assistant available=${app.container.aiAssistant.isAvailable()}")
        withTimeout(90_000) {
            val answer = app.container.askAssistantUseCase(
                role = UserRole.TUTOR,
                prompt = "Responde en una sola frase: ¿qué es la asistencia escolar?",
            )
            Log.i(TAG, "ask: finished with ${answer::class.simpleName}")

            when (answer) {
                is AiAnswer.Answer -> assertTrue(
                    "The model answered with blank text",
                    answer.text.isNotBlank(),
                )

                is AiAnswer.Failure -> throw AssertionError(
                    "The assistant failed with ${answer.reason}: ${answer.reason.userMessage}",
                )
            }
        }
    }

    @Test
    fun appCheckHandsOutATokenForTheRequest() = runBlocking {
        withTimeout(30_000) {
            val token = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            assertTrue("App Check returned an empty token", token.token.isNotBlank())
        }
    }

    private companion object {
        const val TAG = "AiAssistantTest"
        const val DOCENTE_EMAIL = "docente@aulasegura.com"
        const val DOCENTE_PASSWORD = "123456"
    }
}
