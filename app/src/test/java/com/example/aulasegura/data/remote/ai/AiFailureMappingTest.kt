package com.example.aulasegura.data.remote.ai

import com.example.aulasegura.domain.repository.AiFailure
import com.google.firebase.FirebaseException
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how an SDK failure turns into copy the tutor reads.
 *
 * The 429 branch is the one that matters: the free tier has per-minute and
 * per-day quotas, so the limit is not an edge case but the predictable end of a
 * busy morning. If it fell into UNKNOWN, the screen would say "try again in a
 * while" forever instead of asking the user to wait for the quota to reset.
 *
 * The SDK marks its exception constructors `internal`, so the samples are built
 * through reflection. That is deliberate: the mapping only behaves correctly for
 * the real types, and a stand-in subclass of a `final` class is not an option.
 * If the SDK ever renames a class, these tests fail loudly instead of silently
 * covering nothing.
 */
class AiFailureMappingTest {

    @Test
    fun `quota exception is a rate limit`() {
        assertEquals(
            AiFailure.RATE_LIMITED,
            aiFailureFor(sdkException("QuotaExceededException", "quota exceeded")),
        )
    }

    @Test
    fun `server error that mentions 429 is a rate limit too`() {
        // Some SDK paths wrap the quota in a generic ServerException; the status
        // is only in the message, so it has to be read from there.
        assertEquals(
            AiFailure.RATE_LIMITED,
            aiFailureFor(sdkException("ServerException", "429 RESOURCE_EXHAUSTED: quota exceeded")),
        )
        assertEquals(
            AiFailure.RATE_LIMITED,
            aiFailureFor(sdkException("ServerException", "quota exhausted for model")),
        )
    }

    @Test
    fun `a plain server error is not blamed on the quota`() {
        assertEquals(
            AiFailure.UNKNOWN,
            aiFailureFor(sdkException("ServerException", "500 internal error")),
        )
    }

    @Test
    fun `unconfigured and disabled projects tell the user to call the school`() {
        listOf(
            "APINotConfiguredException",
            "ServiceDisabledException",
            "InvalidAPIKeyException",
            "PermissionMissingException",
        ).forEach { type ->
            assertEquals(type, AiFailure.NOT_CONFIGURED, aiFailureFor(sdkException(type, "nope")))
        }
    }

    @Test
    fun `blocked prompts and blocked answers are not errors to retry`() {
        assertEquals(
            AiFailure.BLOCKED,
            aiFailureFor(sdkException("PromptBlockedException", "blocked prompt")),
        )
        assertEquals(
            AiFailure.BLOCKED,
            aiFailureFor(sdkException("ContentBlockedException", "blocked content")),
        )
    }

    @Test
    fun `network failures are reported as offline`() {
        assertEquals(AiFailure.NETWORK, aiFailureFor(UnknownHostException("api.example")))
        assertEquals(AiFailure.NETWORK, aiFailureFor(IOException("socket closed")))
    }

    @Test
    fun `anything unexpected falls back to unknown`() {
        assertEquals(AiFailure.UNKNOWN, aiFailureFor(IllegalStateException("boom")))
    }

    @Test
    fun `an app check attestation rejection is a configuration problem, not a retry`() {
        // Observed on a release APK whose signing certificate was not accepted by
        // the App Check Play Integrity provider: the AI SDK wraps the App Check
        // failure in a plain FirebaseException with this exact body. Telling the
        // tutor to "try again in a while" is wrong — no retry clears it.
        assertEquals(
            AiFailure.NOT_CONFIGURED,
            aiFailureFor(
                FirebaseException(
                    "Error returned from API. code: 403 body: App attestation failed.",
                ),
            ),
        )
    }

    @Test
    fun `a firebase error that is not about attestation keeps the generic message`() {
        assertEquals(
            AiFailure.UNKNOWN,
            aiFailureFor(FirebaseException("Error returned from API. code: 500 body: internal")),
        )
    }

    @Test
    fun `every failure carries copy the UI can show`() {
        AiFailure.entries.forEach { failure ->
            assertTrue("${failure.name} has no user message", failure.userMessage.isNotBlank())
        }
    }

    /**
     * Builds one of the SDK exception types by name.
     *
     * Every one of them exposes a public `(String, Throwable)` constructor in
     * bytecode; only Kotlin visibility marks it internal, which is why reflection
     * reaches it from another module.
     */
    private fun sdkException(simpleName: String, message: String): Throwable {
        val type = Class.forName("com.google.firebase.ai.type.$simpleName")
        val created: Any = type.getConstructor(String::class.java, Throwable::class.java)
            .newInstance(message, null)
        return created as Throwable
    }
}
