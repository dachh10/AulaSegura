package com.example.aulasegura.navigation

/**
 * Maps `notifications.deepLink` to a route the NavHost can open.
 *
 * It is a whitelist, not a pass-through: the value comes from a document written
 * outside the app, and feeding it straight into `navigate()` would let a bad
 * document open anything. `null` means "no destination" and the tap simply opens
 * the app where it was. Parameterized routes accept one segment only, so a
 * document cannot inject a second path into a route.
 */
fun resolveDeepLink(deepLink: String?): String? {
    val path = deepLink?.trim()?.removePrefix("/").orEmpty()
    if (path.isBlank()) return null

    return when {
        // ---- Teacher ----
        path == Routes.TEACHER_HOME -> Routes.TEACHER_HOME
        path == Routes.TEACHER_GROUPS -> Routes.TEACHER_GROUPS
        path == Routes.TEACHER_HISTORY -> Routes.TEACHER_HISTORY
        path == Routes.TEACHER_NOTIFICATIONS -> Routes.TEACHER_NOTIFICATIONS
        path == Routes.TEACHER_PROFILE -> Routes.TEACHER_PROFILE
        path == Routes.TEACHER_CHAT -> Routes.TEACHER_CHAT
        path.startsWith(CONVERSATION_PREFIX) ->
            singleSegmentAfter(path, CONVERSATION_PREFIX)?.let(Routes::teacherConversation)

        // ---- Administrator ----
        path == Routes.ADMIN_DASHBOARD -> Routes.ADMIN_DASHBOARD
        path == Routes.ADMIN_USERS -> Routes.ADMIN_USERS
        path == Routes.ADMIN_NEW_STUDENT -> Routes.ADMIN_NEW_STUDENT
        path == Routes.ADMIN_GROUPS -> Routes.ADMIN_GROUPS
        path == Routes.ADMIN_REPORTS -> Routes.ADMIN_REPORTS
        path == Routes.ADMIN_INCIDENTS -> Routes.ADMIN_INCIDENTS
        path == Routes.ADMIN_CAMPUS -> Routes.ADMIN_CAMPUS
        path == Routes.ADMIN_MORE -> Routes.ADMIN_MORE
        path == Routes.ADMIN_NOTIFICATIONS -> Routes.ADMIN_NOTIFICATIONS
        path == Routes.ADMIN_PROFILE -> Routes.ADMIN_PROFILE
        path.startsWith(ADMIN_GROUP_PREFIX) ->
            singleSegmentAfter(path, ADMIN_GROUP_PREFIX)?.let(Routes::adminGroupDetail)

        // ---- Parent / tutor ----
        path == Routes.PARENT_HOME -> Routes.PARENT_HOME
        path == Routes.PARENT_ATTENDANCE -> Routes.PARENT_ATTENDANCE
        path == Routes.PARENT_TASKS -> Routes.PARENT_TASKS
        path == Routes.PARENT_NOTICES -> Routes.PARENT_NOTICES
        path == Routes.PARENT_INCIDENTS -> Routes.PARENT_INCIDENTS
        path == Routes.PARENT_JUSTIFY -> Routes.PARENT_JUSTIFY
        path == Routes.PARENT_CHAT -> Routes.PARENT_CHAT
        path == Routes.PARENT_AI -> Routes.PARENT_AI
        path == Routes.PARENT_NOTIFICATIONS -> Routes.PARENT_NOTIFICATIONS
        path == Routes.PARENT_PROFILE -> Routes.PARENT_PROFILE
        path.startsWith(PARENT_INCIDENT_PREFIX) -> {
            val rest = path.removePrefix(PARENT_INCIDENT_PREFIX)
            when {
                rest.endsWith("/map") ->
                    singleSegment(rest.removeSuffix("/map"))?.let(Routes::parentIncidentMap)
                else -> singleSegment(rest)?.let(Routes::parentIncidentDetail)
            }
        }
        path.startsWith(PARENT_TASK_PREFIX) ->
            singleSegmentAfter(path, PARENT_TASK_PREFIX)?.let(Routes::parentTaskDetail)
        path.startsWith(PARENT_CONVERSATION_PREFIX) ->
            singleSegmentAfter(path, PARENT_CONVERSATION_PREFIX)?.let(Routes::parentConversation)

        else -> null
    }
}

/** The one path segment after [prefix], or `null` when it is empty or injects a slash. */
private fun singleSegmentAfter(path: String, prefix: String): String? =
    singleSegment(path.removePrefix(prefix))

private fun singleSegment(value: String): String? =
    value.takeIf { it.isNotBlank() && !it.contains('/') && it != "." && it != ".." }

private const val CONVERSATION_PREFIX = "teacher/chat/"
private const val ADMIN_GROUP_PREFIX = "admin/groups/"
private const val PARENT_TASK_PREFIX = "parent/tasks/"
private const val PARENT_INCIDENT_PREFIX = "parent/incidents/"
private const val PARENT_CONVERSATION_PREFIX = "parent/chat/"
