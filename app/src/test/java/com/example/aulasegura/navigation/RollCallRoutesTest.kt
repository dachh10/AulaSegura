package com.example.aulasegura.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** The subject is part of the session identity; losing it in a hop would merge two teachers' lists. */
class RollCallRoutesTest {

    private val group = "grp-3b"
    private val subject = "g3-matematicas-iii"

    @Test
    fun `every roll call hop carries the subject id`() {
        assertEquals(
            "teacher/roll-call/$group/$subject",
            Routes.teacherRollCall(group, subject),
        )
        assertEquals(
            "teacher/roll-call/inside/$group/$subject",
            Routes.teacherRollCallInside(group, subject),
        )
        assertEquals(
            "teacher/roll-call/map/$group/$subject",
            Routes.teacherRollCallMap(group, subject),
        )
        assertEquals(
            "teacher/roll-call/outside/$group/$subject",
            Routes.teacherRollCallOutside(group, subject),
        )
        assertEquals(
            "teacher/roll-call/offline/$group/$subject",
            Routes.teacherRollCallOffline(group, subject),
        )
    }

    @Test
    fun `route patterns declare the subject argument`() {
        listOf(
            Routes.TEACHER_ROLL_CALL,
            Routes.TEACHER_ROLL_CALL_INSIDE,
            Routes.TEACHER_ROLL_CALL_MAP,
            Routes.TEACHER_ROLL_CALL_OUTSIDE,
            Routes.TEACHER_ROLL_CALL_OFFLINE,
        ).forEach { route ->
            assertEquals(true, route.contains("{groupId}"))
            assertEquals(true, route.contains("{subjectId}"))
        }
    }
}
