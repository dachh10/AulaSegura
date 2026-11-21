package com.example.aulasegura.ui.screens.access

/**
 * Decides whether a live roster emission contains a child that was not there
 * when the user generated the QR for another child.
 *
 * The first emission is the baseline when no baseline was supplied: on a cold
 * install the UI state has not received the listener yet, and treating that
 * first (already existing) child as "new" bounced the user straight back home.
 */
internal class NewChildWatcher(seed: Set<String> = emptySet()) {
    private var baseline: Set<String>? = seed.takeIf { it.isNotEmpty() }

    fun hasNewChild(ids: List<String>): Boolean {
        val current = ids.toSet()
        val base = baseline
        if (base == null) {
            baseline = current
            return false
        }
        return current.any { it !in base }
    }
}
