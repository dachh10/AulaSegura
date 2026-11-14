package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.repository.RosterFailure
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * This table decides what the teacher reads when a refresh fails, and getting it
 * wrong is invisible in code review: it took two device runs to notice that a
 * Firestore server read with no connection does not report
 * `FirebaseNetworkException`.
 *
 * The Firebase and Firestore exception types cannot be built in a JVM test — their
 * constructors go through `android.text.TextUtils` — so those branches are covered
 * by the airplane-mode run on the device instead of by a mocked Android. The two
 * cases below are the ones that do not need Firebase.
 */
class RosterFailureMappingTest {

    @Test
    fun `a read that never answers is offline`() {
        val timeout = runBlocking {
            runCatching {
                withTimeout(1) { delay(50) }
            }.exceptionOrNull()
        }

        assertEquals(
            RosterFailure.OFFLINE,
            (timeout as TimeoutCancellationException).toRosterFailure(),
        )
    }

    @Test
    fun `anything else stays unknown instead of pretending to be offline`() {
        assertEquals(
            RosterFailure.UNKNOWN,
            IllegalStateException("boom").toRosterFailure(),
        )
    }
}
