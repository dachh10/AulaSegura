package com.example.aulasegura.data.remote.ai

import com.example.aulasegura.data.remote.remoteconfig.RemoteConfigRepository
import com.example.aulasegura.domain.model.ASSISTANT_SYSTEM_INSTRUCTION
import com.example.aulasegura.domain.repository.AiAnswer
import com.example.aulasegura.domain.repository.AiAssistant
import com.example.aulasegura.domain.repository.AiFailure
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PermissionMissingException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.content
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Firebase AI Logic adapter, using the **Gemini Developer API** backend.
 *
 * `GenerativeBackend.googleAI()` is the only backend with a free tier, and the
 * project stays on Spark: the Agent Platform (formerly Vertex AI) backend needs
 * billing. There is no API key anywhere in this file on purpose — the SDK
 * resolves it from the Firebase project, which is what keeps a leak impossible.
 *
 * The model is a stable text model. Preview and media models are excluded by
 * cost, and every answer is capped so one question cannot burn a day of quota.
 */
class AiAssistantDataSource(
    private val remoteConfig: RemoteConfigRepository,
    /**
     * The rules the model cannot break, sent with the model instead of the
     * question: anything inside the user turn can be argued with, and this text
     * is what keeps the answer inside the tutor's own children and data.
     */
    private val systemInstruction: String = ASSISTANT_SYSTEM_INSTRUCTION,
) : AiAssistant {

    /**
     * Created on first use, not at construction: `AppContainer` builds this
     * class during `Application.onCreate`, and touching the Firebase AI graph
     * there would pay for a plugin nobody asked for yet.
     */
    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = GenerationConfig.builder()
                .setTemperature(TEMPERATURE)
                .setMaxOutputTokens(MAX_OUTPUT_TOKENS)
                .build(),
            systemInstruction = content { text(systemInstruction) },
            requestOptions = RequestOptions(timeoutInMillis = REQUEST_TIMEOUT_MILLIS),
        )
    }

    override fun isAvailable(): Boolean = remoteConfig.isAiAssistantEnabled()

    override suspend fun ask(prompt: String): AiAnswer {
        return try {
            val text = model.generateContent(prompt).text?.trim().orEmpty()
            if (text.isEmpty()) {
                AiAnswer.Failure(AiFailure.EMPTY_RESPONSE)
            } else {
                AiAnswer.Answer(text)
            }
        } catch (cancelled: CancellationException) {
            // Cancellation is the caller leaving the screen, not a failed query.
            // Swallowing it here would keep a dead coroutine alive.
            throw cancelled
        } catch (error: Throwable) {
            AiAnswer.Failure(aiFailureFor(error))
        }
    }

    private companion object {
        /**
         * The high-volume free-tier text model.
         *
         * The console offers exactly two models without billing: `gemini-3.8-flash`
         * and this one. The assistant answers short questions about a child's
         * attendance, so frontier quality buys nothing, while the lite model
         * carries the higher quota and the lower latency — and it was the only one
         * answering while `gemini-3.8-flash` was returning capacity errors.
         *
         * Plain name, never `*-preview`: a preview can be pulled without notice and
         * the tutor would be left with a screen that stopped answering.
         */
        const val MODEL_NAME = "gemini-3.5-flash-lite"

        /** Low enough to keep answers predictable, high enough to explain a grade. */
        const val TEMPERATURE = 0.4f

        /** CONTEXT §9.2 asks for short answers; this bounds one query's cost. */
        const val MAX_OUTPUT_TOKENS = 512

        /**
         * Hard ceiling on one request.
         *
         * The tutor is staring at a screen with a spinner, so "the SDK never
         * answered" cannot mean "spin forever". Sixty seconds is generous for a
         * 512-token flash answer and still short enough that the user gets a
         * message and a retry instead of a frozen screen. It also sits below the
         * SDK default, so the app decides when to give up instead of inheriting
         * a wait nobody measured.
         */
        const val REQUEST_TIMEOUT_MILLIS = 60_000L
    }
}

/**
 * Translates an SDK failure into something the tutor can act on.
 *
 * Kept top level and `internal` so a JVM test can pin every branch without a
 * device, a network or a Firebase project: the mapping is the part that decides
 * whether the UI says "try again" or "this is off", and getting it wrong is
 * invisible until a user is stuck.
 */
internal fun aiFailureFor(error: Throwable): AiFailure = when (error) {
    // The dedicated 429 RESOURCE_EXHAUSTED exception.
    is QuotaExceededException -> AiFailure.RATE_LIMITED

    // Older SDK paths surface the quota as a generic server error, so the
    // status is re-read from the message before giving up on it.
    is ServerException -> if (error.looksLikeQuotaExhaustion()) {
        AiFailure.RATE_LIMITED
    } else {
        AiFailure.UNKNOWN
    }

    is APINotConfiguredException,
    is ServiceDisabledException,
    is InvalidAPIKeyException,
    is PermissionMissingException,
    -> AiFailure.NOT_CONFIGURED

    is PromptBlockedException,
    is ContentBlockedException,
    -> AiFailure.BLOCKED

    // App Check refused the attestation, which is what a release APK gets when the
    // provider in the console does not accept its signing certificate. The SDK
    // wraps that in a plain FirebaseException whose body is the only clue, and no
    // retry clears it: the tutor has to reach the school.
    is FirebaseException -> if (error.isAttestationFailure()) {
        AiFailure.NOT_CONFIGURED
    } else {
        AiFailure.UNKNOWN
    }

    is RequestTimeoutException -> AiFailure.NETWORK

    is IOException -> AiFailure.NETWORK

    else -> AiFailure.UNKNOWN
}

private fun ServerException.looksLikeQuotaExhaustion(): Boolean {
    val text = message?.uppercase() ?: return false
    return "RESOURCE_EXHAUSTED" in text || "429" in text || "QUOTA" in text
}

/** The App Check body, which is the only thing the SDK exposes for this case. */
private fun FirebaseException.isAttestationFailure(): Boolean =
    message?.contains("App attestation failed", ignoreCase = true) == true
