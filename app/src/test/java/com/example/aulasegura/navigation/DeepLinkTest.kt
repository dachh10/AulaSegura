package com.example.aulasegura.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `deepLink` comes from a document written outside the app, so it is a whitelist
 * and not a pass-through: a broken or hostile value must open nothing, never an
 * arbitrary route.
 */
class DeepLinkTest {

    @Test
    fun `the known teacher screens resolve to their routes`() {
        assertEquals(Routes.TEACHER_HOME, resolveDeepLink("teacher/home"))
        assertEquals(Routes.TEACHER_GROUPS, resolveDeepLink("teacher/groups"))
        assertEquals(Routes.TEACHER_HISTORY, resolveDeepLink("teacher/history"))
        assertEquals(Routes.TEACHER_NOTIFICATIONS, resolveDeepLink("teacher/notifications"))
        assertEquals(Routes.TEACHER_PROFILE, resolveDeepLink("teacher/profile"))
    }

    @Test
    fun `a leading slash is tolerated, because a server may write it`() {
        assertEquals(Routes.TEACHER_HISTORY, resolveDeepLink("/teacher/history"))
        assertEquals(Routes.TEACHER_HISTORY, resolveDeepLink("  teacher/history  "))
    }

    @Test
    fun `a chat link opens that conversation`() {
        val id = "uid-docente__uid-tutor"

        assertEquals(
            Routes.teacherConversation(id),
            resolveDeepLink("teacher/chat/$id"),
        )
    }

    @Test
    fun `a chat link with a second segment is refused`() {
        assertNull(resolveDeepLink("teacher/chat/uid-a__uid-b/extra"))
        assertNull(resolveDeepLink("teacher/chat/"))
    }

    @Test
    fun `the administrator screens resolve to their routes`() {
        assertEquals(Routes.ADMIN_DASHBOARD, resolveDeepLink("admin/dashboard"))
        assertEquals(Routes.ADMIN_USERS, resolveDeepLink("admin/users"))
        assertEquals(Routes.ADMIN_INCIDENTS, resolveDeepLink("admin/incidents"))
        assertEquals(Routes.ADMIN_NOTIFICATIONS, resolveDeepLink("admin/notifications"))
        assertEquals(Routes.ADMIN_PROFILE, resolveDeepLink("admin/profile"))
    }

    @Test
    fun `a parameterized administrator or tutor link accepts one segment only`() {
        assertEquals(
            Routes.adminGroupDetail("g-1"),
            resolveDeepLink("admin/groups/g-1"),
        )
        assertEquals(Routes.parentTaskDetail("t-1"), resolveDeepLink("parent/tasks/t-1"))
        assertEquals(
            Routes.parentIncidentDetail("i-1"),
            resolveDeepLink("parent/incidents/i-1"),
        )
        assertEquals(
            Routes.parentIncidentMap("i-1"),
            resolveDeepLink("parent/incidents/i-1/map"),
        )
        assertEquals(
            Routes.parentConversation("c-1"),
            resolveDeepLink("parent/chat/c-1"),
        )
        assertNull(resolveDeepLink("admin/groups/g-1/extra"))
        assertNull(resolveDeepLink("parent/incidents/i-1/extra"))
        assertNull(resolveDeepLink("parent/chat/"))
    }

    @Test
    fun `the tutor screens resolve to their routes`() {
        assertEquals(Routes.PARENT_HOME, resolveDeepLink("parent/home"))
        assertEquals(Routes.PARENT_NOTICES, resolveDeepLink("parent/notices"))
        assertEquals(Routes.PARENT_NOTIFICATIONS, resolveDeepLink("parent/notifications"))
        assertEquals(Routes.PARENT_PROFILE, resolveDeepLink("parent/profile"))
    }

    @Test
    fun `unknown and empty links open nothing`() {
        assertNull(resolveDeepLink(null))
        assertNull(resolveDeepLink(""))
        assertNull(resolveDeepLink("   "))
        assertNull(resolveDeepLink("https://aulasegura.mx/algo"))
        assertNull(resolveDeepLink("administrador/usuarios"))
        assertNull(resolveDeepLink("admin/secret"))
    }

    @Test
    fun `dot segments are refused as document ids`() {
        assertNull(resolveDeepLink("admin/groups/.."))
        assertNull(resolveDeepLink("admin/groups/."))
        assertNull(resolveDeepLink("parent/chat/.."))
        assertNull(resolveDeepLink("parent/incidents/../map"))
    }
}
