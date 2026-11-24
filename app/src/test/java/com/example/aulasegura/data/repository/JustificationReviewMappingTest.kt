package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.repository.JustificationReviewFailure
import com.example.aulasegura.ui.screens.justifications.justificationReviewFailureMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The principal's error message is decided here. A network failure must read as
 * "retry", a missing report as "reload"; confusing them hides a real problem.
 */
class JustificationReviewMappingTest {

    @Test
    fun `a timeout reads as offline`() = runBlocking {
        val timeout = runCatching { withTimeout(1) { delay(50) } }.exceptionOrNull()!! as Exception
        val failure = timeout.toReviewFailure()

        assertEquals(JustificationReviewFailure.OFFLINE, failure)
        assertEquals(
            "Sin conexión. La decisión no se guardó; inténtalo de nuevo.",
            justificationReviewFailureMessage(failure),
        )
    }

    @Test
    fun `an unavailable or deadline firestore error reads as offline`() {
        assertEquals(
            JustificationReviewFailure.OFFLINE,
            firestoreError(FirebaseFirestoreException.Code.UNAVAILABLE).toReviewFailure(),
        )
        assertEquals(
            JustificationReviewFailure.OFFLINE,
            firestoreError(FirebaseFirestoreException.Code.DEADLINE_EXCEEDED).toReviewFailure(),
        )
    }

    @Test
    fun `a permission error reads as denied`() {
        val failure = firestoreError(FirebaseFirestoreException.Code.PERMISSION_DENIED)
            .toReviewFailure()

        assertEquals(JustificationReviewFailure.DENIED, failure)
        assertEquals(
            "La escuela no pudo guardar la decisión. Revisa los permisos de tu cuenta.",
            justificationReviewFailureMessage(failure),
        )
    }

    @Test
    fun `a missing document reads as reload`() {
        val failure = firestoreError(FirebaseFirestoreException.Code.NOT_FOUND).toReviewFailure()

        assertEquals(JustificationReviewFailure.NOT_FOUND, failure)
    }

    @Test
    fun `an unexpected error stays unknown`() {
        val failure = IllegalStateException("what happened?").toReviewFailure()

        assertEquals(JustificationReviewFailure.UNKNOWN, failure)
        assertEquals(
            "No se pudo guardar la decisión. Inténtalo de nuevo.",
            justificationReviewFailureMessage(failure),
        )
    }

    private fun firestoreError(code: FirebaseFirestoreException.Code) =
        FirebaseFirestoreException("failure", code)
}
