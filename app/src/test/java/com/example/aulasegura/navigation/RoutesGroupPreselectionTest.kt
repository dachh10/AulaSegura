package com.example.aulasegura.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesGroupPreselectionTest {

    @Test
    fun `task and notice routes carry the group being viewed`() {
        assertEquals("teacher/task/new?groupId=grp-3b", Routes.teacherTaskNewRoute("grp-3b"))
        assertEquals(
            "teacher/notice/new?groupId=grp-3b",
            Routes.teacherNoticeNewRoute("grp-3b"),
        )
    }

    @Test
    fun `the group argument is omitted when there is no source group`() {
        assertEquals("teacher/task/new", Routes.teacherTaskNewRoute(null))
        assertEquals("teacher/notice/new", Routes.teacherNoticeNewRoute(""))
    }
}
