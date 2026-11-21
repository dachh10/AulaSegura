package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.data.local.ChildSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory selection for unit tests (no DataStore/Android). */
class TestChildSelection : ChildSelection {
    private val _selectedId = MutableStateFlow<String?>(null)
    override val selectedId: StateFlow<String?> = _selectedId

    override fun select(currentUid: String, studentId: String) {
        _selectedId.value = studentId
    }

    override fun ensureValid(currentUid: String, knownIds: List<String>): String? {
        val current = _selectedId.value
        val resolved = if (current in knownIds) current else knownIds.firstOrNull()
        _selectedId.value = resolved
        return resolved
    }
}
