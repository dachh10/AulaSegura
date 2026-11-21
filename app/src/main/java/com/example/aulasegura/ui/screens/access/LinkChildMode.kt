package com.example.aulasegura.ui.screens.access

/**
 * Why the linking screen is open. Kept explicit: first linking and adding a
 * sibling must not share the same state, or the second child inherits the first
 * one's name and the screen tries to write a profile field the rules deny.
 */
enum class LinkChildMode(val title: String) {
    PENDING("Pendiente de vinculación"),
    ADD("Agregar otro hijo"),
}

/** Pure, local state of the linking screen. Nothing here is persisted remotely. */
data class LinkChildState(
    val mode: LinkChildMode,
    val childName: String,
    val showQr: Boolean,
    val message: String? = null,
)

fun linkInitialState(mode: LinkChildMode, profileChildName: String): LinkChildState = when (mode) {
    LinkChildMode.PENDING -> LinkChildState(
        mode = mode,
        childName = profileChildName,
        showQr = profileChildName.isNotBlank(),
    )
    LinkChildMode.ADD -> LinkChildState(
        mode = mode,
        childName = "",
        showQr = false,
    )
}

fun onNameEntered(state: LinkChildState, name: String): LinkChildState =
    state.copy(childName = name, message = null, showQr = false)

/** Generates the QR locally; no Firestore write — the name travels in the QR. */
fun onGenerateQr(state: LinkChildState): LinkChildState {
    val name = state.childName.trim()
    return if (name.isBlank()) {
        state.copy(message = "Escribe el nombre del alumno.")
    } else {
        state.copy(childName = name, showQr = true, message = null)
    }
}
