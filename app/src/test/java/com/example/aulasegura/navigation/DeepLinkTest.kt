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
    fun `unknown and empty links open nothing`() {
        assertNull(resolveDeepLink(null))
        assertNull(resolveDeepLink(""))
        assertNull(resolveDeepLink("   "))
        assertNull(resolveDeepLink("https://aulasegura.mx/algo"))
        assertNull(resolveDeepLink("administrador/usuarios"))
    }
}
