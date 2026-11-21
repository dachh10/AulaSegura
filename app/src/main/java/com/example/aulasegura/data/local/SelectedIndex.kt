package com.example.aulasegura.data.local

/** Position of the persisted studentId in a roster, defaulting to the first child. */
fun indexForSelection(selectedId: String?, ids: List<String>): Int {
    if (ids.isEmpty()) return 0
    val index = selectedId?.let(ids::indexOf) ?: -1
    return if (index >= 0) index else 0
}
