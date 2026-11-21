package com.example.aulasegura.data.remote

import com.google.firebase.firestore.FieldValue

/**
 * Converts the plain tutor links produced by `tutorProfileOnApproval` into
 * additive Firestore transforms.
 *
 * The plain helper is shared with the offline outbox, so it must stay a simple
 * `Map<String, Any>`. At the Firestore boundary the two link arrays need
 * `arrayUnion`: a plain `update` with `listOf(...)` REPLACES the array, so
 * approving a second child silently drops the first child's group from
 * `users.groupIds`. The rules scope every tutor read through `myGroups()`, so
 * that overwrite makes all of the first child's screens fail with
 * PERMISSION_DENIED.
 */
fun tutorApprovalFirestoreUpdate(plain: Map<String, Any>): MutableMap<String, Any> {
    val out = plain.toMutableMap()
    addAll(out, "studentIds")
    addAll(out, "groupIds")
    return out
}

private fun addAll(out: MutableMap<String, Any>, key: String) {
    val values = (out[key] as? List<*>)?.filterNotNull() ?: return
    if (values.isNotEmpty()) out[key] = FieldValue.arrayUnion(*values.toTypedArray())
}
