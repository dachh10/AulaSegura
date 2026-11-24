package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.ui.screens.justifications.JustificationCardUi

/** What the teacher's justification inbox renders. */
sealed interface TeacherJustificationsUiState {

    data object Loading : TeacherJustificationsUiState

    data class Loaded(
        val pending: List<JustificationCardUi>,
        val reviewed: List<JustificationCardUi>,
        val busyId: String? = null,
        val notice: String? = null,
        val error: String? = null,
    ) : TeacherJustificationsUiState {

        val isEmpty: Boolean get() = pending.isEmpty() && reviewed.isEmpty()
    }

    data class Failed(val message: String) : TeacherJustificationsUiState
}
