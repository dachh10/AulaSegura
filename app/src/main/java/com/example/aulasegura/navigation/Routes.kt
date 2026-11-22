package com.example.aulasegura.navigation

/**
 * Single source of truth for every navigation route in AulaSegura.
 * Routes are grouped by the role that owns them.
 */
object Routes {

    // ---- Shared / access ----
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val CHECK_EMAIL = "check_email"
    const val VERIFY_EMAIL = "verify_email"
    const val ACCOUNT_REVIEW = "account_review"
    const val LINK_CHILD = "link_child?mode={mode}"

fun linkChildRoute(mode: String): String = "link_child?mode=$mode"
    const val PERMISSIONS = "permissions"

    // ---- Parent / tutor ----
    const val PARENT_HOME = "parent/home"
    const val PARENT_ATTENDANCE = "parent/attendance"
    const val PARENT_TASKS = "parent/tasks"
    const val PARENT_TASK_DETAIL = "parent/tasks/{taskId}"
    const val PARENT_NOTICES = "parent/notices"
    const val PARENT_INCIDENTS = "parent/incidents"
    const val PARENT_INCIDENT_DETAIL = "parent/incidents/{incidentId}"
    const val PARENT_INCIDENT_MAP = "parent/incidents/{incidentId}/map"
    const val PARENT_JUSTIFY = "parent/justify"

    /**
     * The same destination plus what the form needs to point at a real absence:
     * the child, and the ISO day of the record the tutor touched.
     *
     * Query arguments and not path segments so the notification deep link keeps
     * working with [PARENT_JUSTIFY] alone.
     */
    const val PARENT_JUSTIFY_ARGS = "$PARENT_JUSTIFY?studentId={studentId}&date={date}"
    const val PARENT_CHAT = "parent/chat"
    const val PARENT_CONVERSATION = "parent/chat/{conversationId}"
    const val PARENT_AI = "parent/assistant"
    const val PARENT_NOTIFICATIONS = "parent/notifications"
    const val PARENT_PROFILE = "parent/profile"

    // ---- Teacher ----
    const val TEACHER_HOME = "teacher/home"
    const val TEACHER_GROUPS = "teacher/groups"
    const val TEACHER_GROUP_DETAIL = "teacher/groups/{groupId}"
    const val TEACHER_ROLL_CALL = "teacher/roll-call/{groupId}"
    const val TEACHER_ROLL_CALL_INSIDE = "teacher/roll-call/inside/{groupId}"
    const val TEACHER_ROLL_CALL_MAP = "teacher/roll-call/map/{groupId}"
    const val TEACHER_ROLL_CALL_OUTSIDE = "teacher/roll-call/outside/{groupId}"
    const val TEACHER_ROLL_CALL_OFFLINE = "teacher/roll-call/offline/{groupId}"
    const val TEACHER_ROLL_CALL_SAVE = "teacher/roll-call/save/{groupId}"
    const val TEACHER_INCIDENT_NEW = "teacher/incident/new"
    const val TEACHER_TASK_NEW = "teacher/task/new?groupId={groupId}"
    const val TEACHER_NOTICE_NEW = "teacher/notice/new?groupId={groupId}"

    /** Opens the form with the group the teacher was viewing preselected. */
    fun teacherTaskNewRoute(groupId: String? = null): String =
        if (groupId.isNullOrBlank()) "teacher/task/new" else "teacher/task/new?groupId=$groupId"

    fun teacherNoticeNewRoute(groupId: String? = null): String =
        if (groupId.isNullOrBlank()) "teacher/notice/new" else "teacher/notice/new?groupId=$groupId"
    const val TEACHER_CHAT = "teacher/chat"
    const val TEACHER_CONVERSATION = "teacher/chat/{conversationId}"
    const val TEACHER_HISTORY = "teacher/history"
    const val TEACHER_NOTIFICATIONS = "teacher/notifications"
    const val TEACHER_PROFILE = "teacher/profile"

    // ---- Administrator ----
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val ADMIN_USERS = "admin/users"
const val ADMIN_TEACHER_REQUESTS = "admin/teacher-requests"
const val ADMIN_UNGROUPED_STUDENTS = "admin/students/ungrouped"
    const val ADMIN_NEW_STUDENT = "admin/users/new"
const val ADMIN_SCAN_STUDENT = "admin/students/scan"
const val ADMIN_REVIEW_STUDENT = "admin/students/review?uid={uid}&alumno={alumno}"

fun reviewStudentRoute(uid: String, alumno: String): String =
    "admin/students/review?uid=${android.net.Uri.encode(uid)}" +
        "&alumno=${android.net.Uri.encode(alumno)}"
    const val ADMIN_NEW_STUDENT_FOR_GROUP = "admin/users/new/{groupId}"
    const val ADMIN_GROUPS = "admin/groups"
    const val ADMIN_GROUP_DETAIL = "admin/groups/{groupId}"
    const val ADMIN_REPORTS = "admin/reports"
    const val ADMIN_INCIDENTS = "admin/incidents"
    const val ADMIN_CAMPUS = "admin/campus"
    const val ADMIN_MORE = "admin/more"
    const val ADMIN_NOTIFICATIONS = "admin/notifications"
    const val ADMIN_PROFILE = "admin/profile"

    // Helpers for parameterized routes
    fun parentTaskDetail(taskId: String) = "parent/tasks/$taskId"
    fun parentIncidentDetail(incidentId: String) = "parent/incidents/$incidentId"
    fun parentIncidentMap(incidentId: String) = "parent/incidents/$incidentId/map"
    fun parentConversation(conversationId: String) = "parent/chat/$conversationId"
    fun teacherGroupDetail(groupId: String) = "teacher/groups/$groupId"
    fun teacherRollCall(groupId: String) = "teacher/roll-call/$groupId"
    fun teacherRollCallInside(groupId: String) = "teacher/roll-call/inside/$groupId"
    fun teacherRollCallMap(groupId: String) = "teacher/roll-call/map/$groupId"
    fun teacherRollCallOutside(groupId: String) = "teacher/roll-call/outside/$groupId"
    fun teacherRollCallOffline(groupId: String) = "teacher/roll-call/offline/$groupId"
    fun teacherConversation(conversationId: String) = "teacher/chat/$conversationId"
    fun adminGroupDetail(groupId: String) = "admin/groups/$groupId"
    fun adminNewStudent(groupId: String) = "admin/users/new/$groupId"

    /**
     * Justificar falta, optionally pointing at one absence.
     *
     * [studentId] keeps the form on the child the tutor was looking at; [date] is
     * the ISO day of the record they touched. Both are optional: the quick access
     * from home sends nothing and the form resolves the newest absence itself.
     */
    fun parentJustify(studentId: String? = null, date: String? = null): String {
        val query = listOfNotNull(
            studentId?.takeIf { it.isNotBlank() }?.let { "studentId=$it" },
            date?.takeIf { it.isNotBlank() }?.let { "date=$it" },
        )
        return if (query.isEmpty()) PARENT_JUSTIFY else "$PARENT_JUSTIFY?${query.joinToString("&")}"
    }
}
