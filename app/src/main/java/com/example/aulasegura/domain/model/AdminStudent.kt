package com.example.aulasegura.domain.model

/**
 * A student as the principal's Alumnos tab sees them.
 *
 * This is a domain model, not the Room entity used by the teacher's roster: it
 * carries the fields the principal creates and the `active` flag that drives the
 * teacher's roll-call query (`groupId == X AND active == true`).
 *
 * Every field has a safe fallback. A malformed document must still produce a row:
 * one hand-edited student cannot be allowed to crash the whole list.
 */
data class AdminStudent(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val curp: String,
    val listNumber: Int,
    val groupId: String,
    val schoolId: String,
    val tutorIds: List<String>,
    val active: Boolean,
) {
    /** The name to render, with a chain of fallbacks for incomplete documents. */
    val displayName: String get() = fullName
        .ifBlank { "$firstName $lastName".trim() }
        .ifBlank { id }

    companion object {
        /** Maps a `students/{id}` document to the domain model. */
        fun fromDocument(id: String, data: Map<String, Any?>): AdminStudent {
            val firstName = (data["firstName"] as? String).orEmpty()
            val lastName = (data["lastName"] as? String).orEmpty()
            return AdminStudent(
                id = id,
                firstName = firstName,
                lastName = lastName,
                fullName = (data["fullName"] as? String).orEmpty()
                    .ifBlank { "$firstName $lastName".trim() }
                    .ifBlank { id },
                curp = data["curp"] as? String ?: "",
                listNumber = (data["listNumber"] as? Number)?.toInt() ?: 0,
                groupId = data["groupId"] as? String ?: "",
                schoolId = data["schoolId"] as? String ?: "",
                tutorIds = stringList(data["tutorIds"]),
                // A student with no explicit flag is treated as active, which is
                // what the roster mapper does for the teacher's cached copy.
                active = data["active"] as? Boolean ?: true,
            )
        }

        private fun stringList(value: Any?): List<String> =
            (value as? List<*>)?.filterIsInstance<String>().orEmpty()
    }
}
