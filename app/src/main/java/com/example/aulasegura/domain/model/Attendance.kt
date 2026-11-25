package com.example.aulasegura.domain.model

/**
 * How one student was marked in a roll call.
 *
 * The four values are the only ones the Firestore schema accepts
 * (`CONTEXT.md` §6.2), and there is deliberately no "unmarked" member: a
 * student without a mark is not a status, it is a missing record. Writing
 * "absent" for someone the teacher never touched would lie to the tutor, so the
 * UI blocks the save until every row has a mark instead.
 *
 * [wire] is the exact string stored in `records/{studentId}.status` and in the
 * `counts` keys; the enum name is not.
 */
enum class AttendanceMark(val wire: String) {

    PRESENT("present"),
    LATE("late"),
    ABSENT("absent"),
    JUSTIFIED("justified"),
    ;

    companion object {

        /** `null` when the stored value is not one of the four known statuses. */
        fun fromWire(value: String): AttendanceMark? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * One student in the roll call being taken.
 *
 * [mark] is `null` while the row is untouched. [listNumber] comes from the
 * roster and is what keeps the official order in Firestore and on screen.
 *
 * [initials] is display data derived from the structured name fields, not from
 * [fullName]: splitting a full name cannot know where the given names end, and
 * the two seeded cases prove it ("Mariana Gómez Ruiz" is MG, "Ana Sofía
 * Aguilar" is AA).
 */
data class RollCallStudent(
    val id: String,
    val listNumber: Int,
    val fullName: String,
    val initials: String,
    val mark: AttendanceMark? = null,
)

/** One record to persist: the full set of marked students of a session. */
data class RollCallEntry(
    val studentId: String,
    val listNumber: Int,
    val mark: AttendanceMark,
)

/**
 * The four counters of a session.
 *
 * Used by the screen for the chips. The counters that reach Firestore are
 * recomputed from the records being written (`AttendanceSyncMapper.withCountsFrom`),
 * never copied from here: two devices marking the same group would otherwise
 * persist counters that contradict their own records.
 */
data class AttendanceCounts(
    val present: Int = 0,
    val absent: Int = 0,
    val late: Int = 0,
    val justified: Int = 0,
) {

    val marked: Int get() = present + absent + late + justified

    companion object {

        fun of(marks: Iterable<AttendanceMark?>): AttendanceCounts {
            var present = 0
            var absent = 0
            var late = 0
            var justified = 0
            marks.forEach { mark ->
                when (mark) {
                    AttendanceMark.PRESENT -> present++
                    AttendanceMark.ABSENT -> absent++
                    AttendanceMark.LATE -> late++
                    AttendanceMark.JUSTIFIED -> justified++
                    null -> Unit
                }
            }
            return AttendanceCounts(
                present = present,
                absent = absent,
                late = late,
                justified = justified,
            )
        }
    }
}

/**
 * Where the teacher was when the session was closed, as one value.
 *
 * [verified] is computed by `evaluateGeofence` and stored as-is: recomputing it
 * later against a radius that may have changed would rewrite history. The
 * distance is kept next to the coordinates so a reader does not have to trust a
 * boolean it cannot check.
 */
data class SessionLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null,
    val distanceMeters: Double? = null,
    val verified: Boolean = false,
)

/**
 * Header of the session being taken, resolved before anything is written.
 *
 * [sessionId] is a client-generated UUID and becomes the Firestore document id,
 * which is what makes a retry overwrite the same session instead of duplicating
 * it (`CONTEXT.md` §8.3). [groupLabel] is display data for the screen; the
 * session itself only stores [groupId].
 *
 * [location] stays `null` when the device had no fix or no permission. That is a
 * valid session — unverified, not a failed one.
 */
data class RollCallDraft(
    val sessionId: String,
    val groupId: String,
    val groupLabel: String,
    val schoolId: String,
    val teacherId: String,
    val subject: String,
    /**
     * Canonical catalog id of [subject], e.g. `g3-matematicas-iii`.
     *
     * Empty only for legacy callers: a session opened from a slot always carries
     * the id, which is what keeps two teachers of the same group from writing the
     * same (group, date, subject) session.
     */
    val subjectId: String = "",
    /** ISO date, `"2026-11-03"`. */
    val date: String,
    val startedAt: Long,
    val location: SessionLocation? = null,
)

/**
 * The subject part of a slot key. The label is the durable value: every saved
 * session stores it, including legacy ones with no canonical id. The repository
 * also checks `subjectId`, so a draft with a canonical id matches both shapes.
 */
val RollCallDraft.subjectKey: String get() = subject
