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
    /**
     * The linking flow's vocabulary. Defaults mirror the legacy fields so every
     * existing call site keeps working while the two documents converge.
     */
    val tutores: List<String> = tutorIds,
    val grado: String = "",
    val grupo: String = groupId,
    val creadoPor: String = "",
    /** `PENDIENTE` in the requirement doc is the existing `PENDING` sync phase. */
    val syncStatus: String = "SYNCED",
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
            val fullName = (data["nombre"] as? String).orEmpty()
                .ifBlank { data["fullName"] as? String ?: "" }
                .ifBlank { "$firstName $lastName".trim() }
                .ifBlank { id }
            val rawTutores = stringList(data["tutores"])
            val legacyTutorIds = stringList(data["tutorIds"]).ifEmpty { rawTutores }
            val rawGrupo = data["grupo"] as? String ?: ""
            val legacyGroupId = (data["groupId"] as? String).orEmpty().ifBlank { rawGrupo }
            return AdminStudent(
                id = id,
                firstName = firstName,
                lastName = lastName,
                fullName = fullName,
                curp = data["curp"] as? String ?: "",
                listNumber = (data["listNumber"] as? Number)?.toInt() ?: 0,
                groupId = legacyGroupId,
                schoolId = data["schoolId"] as? String ?: "",
                tutorIds = legacyTutorIds,
                // A student with no explicit flag is treated as active, which is
                // what the roster mapper does for the teacher's cached copy.
                active = data["active"] as? Boolean ?: data["activo"] as? Boolean ?: true,
                tutores = rawTutores.ifEmpty { legacyTutorIds },
                grupo = rawGrupo.ifBlank { legacyGroupId },
                grado = data["grado"] as? String ?: "",
                creadoPor = data["creadoPor"] as? String ?: data["createdBy"] as? String ?: "",
                syncStatus = data["syncStatus"] as? String ?: "SYNCED",
            )
        }

        private fun stringList(value: Any?): List<String> =
            (value as? List<*>)?.filterIsInstance<String>().orEmpty()
    }
}
