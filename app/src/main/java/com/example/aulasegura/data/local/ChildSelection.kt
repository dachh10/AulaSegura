package com.example.aulasegura.data.local

import kotlinx.coroutines.flow.StateFlow

/** Contract the parent ViewModels use; SelectedChildStore is the real impl. */
interface ChildSelection {
    val selectedId: StateFlow<String?>
    fun select(currentUid: String, studentId: String)
    fun ensureValid(currentUid: String, knownIds: List<String>): String?
}
