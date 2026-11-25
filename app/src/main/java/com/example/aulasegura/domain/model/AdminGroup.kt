package com.example.aulasegura.domain.model

/**
 * A school group as the principal's Grupos screens see it.
 *
 * This is a domain model, not the Room entity used by the teacher roster: it carries
 * what the principal edits (the two-sided teacher links and the schedule) and nothing
 * about attendance or incidents, which are separate collections.
 *
 * Every field has a safe fallback. A malformed document must still produce a row:
 * the list cannot afford to crash because one hand-edited document is missing a
 * `grade` or carries a list of the wrong type.
 */
data class AdminGroup(
    val id: String,
    val name: String,
    val grade: Int,
    val section: String,
    val schoolId: String,
    val teacherIds: List<String>,
    val titularTeacherId: String,
    val studentCount: Int,
    val cycle: String,
    val schedule: List<AdminScheduleSlot> = emptyList(),
    /** Canonical subject id -> uid of the teacher who owns that slot here. */
    val subjectTeachers: Map<String, String> = emptyMap(),
) {
    /** The group label the mockup prints in the badge ("3° B"). */
    val label: String get() = name

    companion object {
        /** Maps a `groups/{id}` document plus its schedule slots to the domain model. */
        fun fromDocument(
            id: String,
            data: Map<String, Any?>,
            schedule: List<AdminScheduleSlot> = emptyList(),
        ): AdminGroup = AdminGroup(
            id = id,
            name = (data["name"] as? String).orEmpty().ifBlank { id },
            grade = (data["grade"] as? Number)?.toInt() ?: 0,
            section = data["section"] as? String ?: "",
            schoolId = data["schoolId"] as? String ?: "",
            teacherIds = stringList(data["teacherIds"]),
            titularTeacherId = data["titularTeacherId"] as? String ?: "",
            studentCount = (data["studentCount"] as? Number)?.toInt() ?: 0,
            cycle = data["cycle"] as? String ?: "",
            schedule = schedule,
            subjectTeachers = (data["subjectTeachers"] as? Map<*, *>)
                ?.mapNotNull { (key, value) ->
                    (key as? String)?.let { k -> (value as? String)?.let { k to it } }
                }?.toMap().orEmpty(),
        )

        private fun stringList(value: Any?): List<String> =
            (value as? List<*>)?.filterIsInstance<String>().orEmpty()
    }
}

/**
 * One `groups/{groupId}/schedule/{slotId}` row.
 *
 * [id] is the deterministic slot id derived from weekday + start time. It is empty
 * for a row the principal has just added and not yet saved.
 */
data class AdminScheduleSlot(
    val id: String = "",
    val weekday: Int = 1,
    val startTime: String = "",
    val endTime: String = "",
    val subject: String = "",
    val teacherId: String = "",
) {
    companion object {
        fun fromDocument(id: String, data: Map<String, Any?>): AdminScheduleSlot = AdminScheduleSlot(
            id = id,
            weekday = (data["weekday"] as? Number)?.toInt() ?: 1,
            startTime = data["startTime"] as? String ?: "",
            endTime = data["endTime"] as? String ?: "",
            subject = data["subject"] as? String ?: "",
            teacherId = data["teacherId"] as? String ?: "",
        )
    }
}
