package com.example.aulasegura.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.selectedChildDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "selected_child",
)

/**
 * The tutor's selected child, shared by every parent tab and persisted across
 * sessions. Stored with the owning tutor uid so a different account on the same
 * device never inherits a sibling's selection.
 */
class SelectedChildStore(
    private val context: Context,
) : ChildSelection {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _selectedId = MutableStateFlow<String?>(null)
    override val selectedId: StateFlow<String?> = _selectedId

    private var savedUid: String? = null
    private var savedStudentId: String? = null

    suspend fun load() {
        val prefs = context.selectedChildDataStore.data.first()
        savedUid = prefs[KEY_UID]
        savedStudentId = prefs[KEY_STUDENT_ID]
        _selectedId.value = savedStudentId?.takeIf { savedUid != null }
    }

    override fun select(currentUid: String, studentId: String) {
        savedUid = currentUid
        savedStudentId = studentId
        _selectedId.value = studentId
        scope.launch {
            context.selectedChildDataStore.edit { prefs ->
                prefs[KEY_UID] = currentUid
                prefs[KEY_STUDENT_ID] = studentId
            }
        }
    }

    /**
     * Reconciles the persisted id with the children actually linked to the
     * current tutor. Returns the id every tab should use from now on.
     */
    override fun ensureValid(currentUid: String, knownIds: List<String>): String? {
        val resolved = resolve(savedUid, savedStudentId, currentUid, knownIds)
        _selectedId.update { resolved }
        if (resolved != null && (savedUid != currentUid || savedStudentId != resolved)) {
            select(currentUid, resolved)
        }
        return resolved
    }

    companion object {
        private val KEY_UID = stringPreferencesKey("uid")
        private val KEY_STUDENT_ID = stringPreferencesKey("studentId")

        /** Pure core, kept free of Android so it can be unit-tested. */
        fun resolve(
            savedUid: String?,
            savedStudentId: String?,
            currentUid: String,
            knownIds: List<String>,
        ): String? {
            if (savedUid == null || savedStudentId == null || savedUid != currentUid) {
                return knownIds.firstOrNull()
            }
            return if (savedStudentId in knownIds) savedStudentId else knownIds.firstOrNull()
        }
    }
}
