package com.example.aulasegura.navigation

/**
 * Maps `notifications.deepLink` to a route the NavHost can open.
 *
 * It is a whitelist, not a pass-through: the value comes from a document written
 * outside the app, and feeding it straight into `navigate()` would let a bad
 * document open anything. `null` means "no destination" and the tap simply opens
 * the app where it was.
 */
fun resolveDeepLink(deepLink: String?): String? {
    val path = deepLink?.trim()?.removePrefix("/").orEmpty()
    if (path.isBlank()) return null

    return when {
        path == Routes.TEACHER_HOME -> Routes.TEACHER_HOME
        path == Routes.TEACHER_GROUPS -> Routes.TEACHER_GROUPS
        path == Routes.TEACHER_HISTORY -> Routes.TEACHER_HISTORY
        path == Routes.TEACHER_NOTIFICATIONS -> Routes.TEACHER_NOTIFICATIONS
        path == Routes.TEACHER_PROFILE -> Routes.TEACHER_PROFILE
        path == Routes.TEACHER_CHAT -> Routes.TEACHER_CHAT
        path.startsWith(CONVERSATION_PREFIX) -> {
            val conversationId = path.removePrefix(CONVERSATION_PREFIX)
            // One segment only: a slash means somebody tried to inject a second
            // path into the route.
            if (conversationId.isBlank() || conversationId.contains('/')) {
                null
            } else {
                Routes.teacherConversation(conversationId)
            }
        }
        else -> null
    }
}

private const val CONVERSATION_PREFIX = "teacher/chat/"
