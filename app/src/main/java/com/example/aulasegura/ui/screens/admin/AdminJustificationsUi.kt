package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.ui.screens.justifications.JustificationCardUi

/** The principal's inbox: the same reports the teacher reads, plus the decision. */
data class AdminJustificationsUiState(
    val loading: Boolean = true,
    val pending: List<JustificationCardUi> = emptyList(),
    val reviewed: List<JustificationCardUi> = emptyList(),
    val error: String? = null,
    val notice: String? = null,
    /** Report being written, so the buttons cannot be tapped twice. */
    val busyId: String? = null,
) {
    val pendingCount: Int get() = pending.size
    val isEmpty: Boolean get() = pending.isEmpty() && reviewed.isEmpty()
}
