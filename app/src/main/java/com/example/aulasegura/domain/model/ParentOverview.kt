package com.example.aulasegura.domain.model

/**
 * One child of the signed-in tutor, as the family screens show them.
 *
 * [groupLabel] is the human name of the group ("4° D"), resolved from
 * `groups/{id}.name`; [listNumber] is what keeps siblings in school order.
 */
data class ParentChild(
    val studentId: String,
    val fullName: String,
    val firstName: String,
    val listNumber: Int,
    val groupId: String,
    val groupLabel: String,
)

/**
 * The tutor's own counters of a child's month.
 *
 * [attended] counts present and late arrivals: arriving late is attending.
 * A justified absence is stored apart because it is still an absence; the
 * calendar and the recent detail show it, the percentage does not count it.
 */
data class ParentAttendanceCounts(
    val present: Int = 0,
    val late: Int = 0,
    val absent: Int = 0,
    val justified: Int = 0,
) {

    val total: Int get() = present + late + absent + justified

    val attended: Int get() = present + late

    /** `null` when no session of the month carries a record for this child. */
    val percent: Int? get() = if (total == 0) null else attended * 100 / total
}

/**
 * One closed session of the child's group, seen from the child.
 *
 * [mark] is `null` when the session exists but has no record for this student:
 * the app then says "sin registro" instead of guessing a state.
 */
data class ParentDayAttendance(
    val date: String,
    val subject: String,
    val startedAt: Long,
    val mark: AttendanceMark?,
    val time: String?,
    val note: String?,
    /** Canonical subject id; empty on sessions written before the subject filter. */
    val subjectId: String = "",
)

/** A month of the child, ready for the calendar and the summary card. */
data class ParentMonthAttendance(
    val year: Int,
    val month: Int,
    val counts: ParentAttendanceCounts,
    /** Sessions of the month, newest first. */
    val days: List<ParentDayAttendance>,
)

/** A task published to the child's group and still due. */
data class ParentTaskBrief(
    val taskId: String,
    val subject: String,
    val title: String,
    val dueDateMillis: Long,
)

/** Everything the home card needs for one child. */
data class ParentChildOverview(
    val child: ParentChild,
    val today: ParentDayAttendance?,
    val month: ParentMonthAttendance,
    val tasks: List<ParentTaskBrief>,
)
