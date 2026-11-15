package com.example.aulasegura.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.location.evaluateGeofence
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.location.rememberSchoolSite
import com.example.aulasegura.location.toSessionLocation
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.screens.access.ForgotPasswordScreen
import com.example.aulasegura.ui.screens.access.LoginScreen
import com.example.aulasegura.ui.screens.access.LoginViewModel
import com.example.aulasegura.ui.screens.access.OnboardingScreen
import com.example.aulasegura.ui.screens.access.PermissionsScreen
import com.example.aulasegura.ui.screens.access.SessionState
import com.example.aulasegura.ui.screens.access.SessionViewModel
import com.example.aulasegura.ui.screens.access.SplashScreen
import com.example.aulasegura.ui.screens.admin.AdminDashboardScreen
import com.example.aulasegura.ui.screens.admin.AdminCampusScreen
import com.example.aulasegura.ui.screens.admin.AdminGroupsScreen
import com.example.aulasegura.ui.screens.admin.AdminGroupDetailScreen
import com.example.aulasegura.ui.screens.admin.AdminIncidentsScreen
import com.example.aulasegura.ui.screens.admin.AdminMoreScreen
import com.example.aulasegura.ui.screens.admin.AdminNewStudentScreen
import com.example.aulasegura.ui.screens.admin.AdminNotificationsScreen
import com.example.aulasegura.ui.screens.admin.AdminProfileScreen
import com.example.aulasegura.ui.screens.admin.AdminReportsScreen
import com.example.aulasegura.ui.screens.admin.AdminUsersScreen
import com.example.aulasegura.ui.screens.parent.ParentAIScreen
import com.example.aulasegura.ui.screens.parent.ParentAttendanceScreen
import com.example.aulasegura.ui.screens.parent.ParentChatScreen
import com.example.aulasegura.ui.screens.parent.ParentConversationScreen
import com.example.aulasegura.ui.screens.parent.ParentHomeScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentDetailScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentMapScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentsScreen
import com.example.aulasegura.ui.screens.parent.ParentJustifyScreen
import com.example.aulasegura.ui.screens.parent.ParentNoticesScreen
import com.example.aulasegura.ui.screens.parent.ParentNotificationsScreen
import com.example.aulasegura.ui.screens.parent.ParentProfileScreen
import com.example.aulasegura.ui.screens.parent.ParentTaskDetailScreen
import com.example.aulasegura.ui.screens.parent.ParentTasksScreen
import com.example.aulasegura.ui.screens.teacher.RollCallScreen
import com.example.aulasegura.ui.screens.teacher.RollCallViewModel
import com.example.aulasegura.ui.screens.teacher.RollCallViewModelFactory
import com.example.aulasegura.ui.screens.teacher.TeacherChatScreen
import com.example.aulasegura.ui.screens.teacher.TeacherConversationScreen
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsScreen
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHistoryScreen
import com.example.aulasegura.ui.screens.teacher.TeacherHistoryViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherIncidentNewScreen
import com.example.aulasegura.ui.screens.teacher.TeacherIncidentViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHomeScreen
import com.example.aulasegura.ui.screens.teacher.TeacherLocationVerifyingScreen
import com.example.aulasegura.ui.screens.teacher.TeacherNoticeNewScreen
import com.example.aulasegura.ui.screens.teacher.TeacherNotificationsScreen
import com.example.aulasegura.ui.screens.teacher.TeacherOutsideRadiusScreen
import com.example.aulasegura.ui.screens.teacher.TeacherProfileScreen
import com.example.aulasegura.ui.screens.teacher.TeacherRollCallMapScreen
import com.example.aulasegura.ui.screens.teacher.TeacherRollCallOfflineScreen
import com.example.aulasegura.ui.screens.teacher.TeacherTaskNewScreen

/**
 * Navigation graph for the whole app.
 *
 * Every screen is currently a stub so the full flow is walkable end to end.
 * Each stub is replaced by the real Compose screen as its Stitch design is ported.
 */
@Composable
fun AulaSeguraNavHost(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = viewModel(factory = rememberAppContainer().viewModelFactory),
    loginViewModel: LoginViewModel = viewModel(factory = rememberAppContainer().viewModelFactory),
) {
    val sessionState by sessionViewModel.state.collectAsState()

    // Single place that reacts to a lost session: logout from any role, or a
    // deactivated account being ejected while it is already inside the app.
    LaunchedEffect(sessionState) {
        if (sessionState !is SessionState.SignedOut) return@LaunchedEffect
        val route = navController.currentBackStackEntry?.destination?.route
        val isAccessRoute = route == null ||
            route == Routes.SPLASH ||
            route == Routes.ONBOARDING ||
            route == Routes.LOGIN ||
            route == Routes.FORGOT_PASSWORD ||
            route == Routes.CHECK_EMAIL ||
            route == Routes.PERMISSIONS
        if (isAccessRoute) return@LaunchedEffect
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInVertically(animationSpec = tween(300)) { height -> height / 40 }
        },
        exitTransition = { fadeOut(animationSpec = tween(180)) },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInVertically(animationSpec = tween(300)) { height -> height / 40 }
        },
        popExitTransition = { fadeOut(animationSpec = tween(180)) },
    ) {
        // ---- Access ----
        composable(Routes.SPLASH) {
            val sessionState by sessionViewModel.state.collectAsState()
            var splashFinished by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) { sessionViewModel.resolveSession() }

            // Navigate only when both conditions are met: the splash animation
            // finished AND the persisted session was resolved. Navigating on the
            // animation alone would send an already signed-in user to onboarding.
            LaunchedEffect(sessionState, splashFinished) {
                if (!splashFinished) return@LaunchedEffect
                val target = when (val state = sessionState) {
                    SessionState.Resolving -> return@LaunchedEffect
                    SessionState.SignedOut -> Routes.ONBOARDING
                    is SessionState.SignedIn -> state.role.homeRoute()
                }
                navController.navigate(target) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }

            SplashScreen(onFinished = { splashFinished = true })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            val uiState by loginViewModel.uiState.collectAsState()

            LaunchedEffect(uiState.authenticatedRole) {
                val role = uiState.authenticatedRole ?: return@LaunchedEffect
                sessionViewModel.onSignedIn(role)
                loginViewModel.consumeNavigation()
                navController.navigate(role.homeRoute()) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }

            LoginScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onLogin = loginViewModel::signIn,
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        // CHECK_EMAIL state is now embedded inside ForgotPasswordScreen (single-screen two-state design).
        // Keep this route as an alias that navigates to the forgot password flow.
        composable(Routes.CHECK_EMAIL) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onContinue = { navController.popBackStack() },
                onSkip = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        // ---- Parent / tutor ----
        composable(Routes.PARENT_HOME) {
            ParentHomeScreen(
                onNotifications = { navController.navigate(Routes.PARENT_NOTIFICATIONS) },
                onQuickAvisos = { navController.navigate(Routes.PARENT_NOTICES) },
                onQuickIncidentes = { navController.navigate(Routes.PARENT_INCIDENTS) },
                onQuickJustificar = { navController.navigate(Routes.PARENT_JUSTIFY) },
                onQuickAsistente = { navController.navigate(Routes.PARENT_AI) },
                onVerTareas = { navController.navigate(Routes.PARENT_TASKS) },
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_ATTENDANCE) {
            ParentAttendanceScreen(
                onJustificar = { navController.navigate(Routes.PARENT_JUSTIFY) },
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_TASKS) {
            ParentTasksScreen(
                onOpenTask = { navController.navigate(Routes.parentTaskDetail("1")) },
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_TASK_DETAIL) {
            ParentTaskDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARENT_NOTICES) {
            ParentNoticesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARENT_INCIDENTS) {
            ParentIncidentsScreen(
                onBack = { navController.popBackStack() },
                onOpenIncident = { navController.navigate(Routes.parentIncidentDetail("1")) },
            )
        }
        composable(Routes.PARENT_INCIDENT_DETAIL) {
            ParentIncidentDetailScreen(
                onBack = { navController.popBackStack() },
                onTalkToTeacher = { navController.navigate(Routes.parentConversation("1")) },
                onOpenMap = { navController.navigate(Routes.parentIncidentMap("1")) },
            )
        }
        composable(Routes.PARENT_INCIDENT_MAP) {
            ParentIncidentMapScreen(
                onBack = { navController.popBackStack() },
                onTalkToTeacher = { navController.navigate(Routes.parentConversation("1")) },
            )
        }
        composable(Routes.PARENT_JUSTIFY) {
            ParentJustifyScreen(
                onBack = { navController.popBackStack() },
                onSend = { navController.popBackStack() },
            )
        }
        composable(Routes.PARENT_CHAT) {
            ParentChatScreen(
                onOpenConversation = { conversationId ->
                    navController.navigate(Routes.parentConversation(conversationId))
                },
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_CONVERSATION) {
            ParentConversationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARENT_AI) {
            ParentAIScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARENT_NOTIFICATIONS) {
            ParentNotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PARENT_PROFILE) {
            ParentProfileScreen(
                onSelectTab = { navController.openParentTab(it) },
                onLogout = { sessionViewModel.signOut() },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
            )
        }

        // ---- Teacher ----
        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen(
                onIrAGrupos = { navController.navigate(Routes.TEACHER_GROUPS) },
                onIrAPaseLista = { navController.navigate(Routes.TEACHER_GROUPS) },
                onIrARegistrarIncidente = { navController.navigate(Routes.TEACHER_INCIDENT_NEW) },
                onIrAPublicarTarea = { navController.navigate(Routes.TEACHER_TASK_NEW) },
                onIrAPublicarAviso = { navController.navigate(Routes.TEACHER_NOTICE_NEW) },
                onIrAChat = { navController.navigate(Routes.TEACHER_CHAT) },
                onIrAHistorial = { navController.navigate(Routes.TEACHER_HISTORY) },
                onIrANotificaciones = { navController.navigate(Routes.TEACHER_NOTIFICATIONS) },
                onIrAPerfil = { navController.navigate(Routes.TEACHER_PROFILE) },
            )
        }
        composable(Routes.TEACHER_GROUPS) {
            val groupsViewModel: TeacherGroupsViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val groupsState by groupsViewModel.state.collectAsState()

            TeacherGroupsScreen(
                groups = groupsState.groups,
                refreshing = groupsState.refreshing,
                notice = groupsState.notice,
                onRetry = groupsViewModel::refresh,
                onIrAInicio = { navController.popBackStack() },
                onIrAChat = { navController.navigate(Routes.TEACHER_CHAT) },
                onIrAHistorial = { navController.navigate(Routes.TEACHER_HISTORY) },
                onIrAPerfil = { navController.navigate(Routes.TEACHER_PROFILE) },
                onIrANotificaciones = { navController.navigate(Routes.TEACHER_NOTIFICATIONS) },
                onPasarLista = { groupId -> navController.navigate(Routes.teacherRollCall(groupId)) },
                onVerAlumnos = { groupId -> navController.navigate(Routes.teacherGroupDetail(groupId)) },
            )
        }
        composable(Routes.TEACHER_GROUP_DETAIL) {
            PlaceholderScreen(
                title = "Detalle de grupo",
                next = "Pase de lista",
                onNext = { navController.navigate(Routes.teacherRollCall("3B")) },
                extraActions = listOf("Volver" to { navController.popBackStack() }),
            )
        }
        // GPS-first: entrar a "Pase de lista" dispara la verificación real de
        // ubicación. La lista de alumnos sólo se alcanza dentro del radio.
        composable(Routes.TEACHER_ROLL_CALL) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            val school = rememberSchoolSite()
            TeacherLocationVerifyingScreen(
                groupId = groupId,
                school = school.site,
                schoolLoaded = school.loaded,
                onBack = { navController.popBackStack() },
                onInsideRadius = {
                    navController.navigate(Routes.teacherRollCallInside(groupId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
                onOutsideRadius = {
                    navController.navigate(Routes.teacherRollCallOutside(groupId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
                onContinueUnverified = {
                    navController.navigate(Routes.teacherRollCallInside(groupId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_INSIDE) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            val container = rememberAppContainer()
            val rollCallViewModel: RollCallViewModel = viewModel(
                key = "roll-call-$groupId",
                factory = RollCallViewModelFactory(
                    groupId = groupId,
                    startRollCall = container.startRollCallUseCase,
                    saveRollCall = container.saveRollCallUseCase,
                    students = container.studentRepository,
                    attendance = container.attendanceRepository,
                ),
            )
            val rollCall by rollCallViewModel.state.collectAsState()
            val school = rememberSchoolSite()
            val location = rememberCurrentLocation()
            val geofence = remember(location.fix, school.site) {
                evaluateGeofence(location.fix, school.site)
            }
            // The verdict is frozen here, from the reading of this moment. The
            // session stores that value and never recomputes it on read: a later
            // radius change must not rewrite whether this class was verified.
            LaunchedEffect(location.fix, school.site) {
                val fix = location.fix ?: return@LaunchedEffect
                rollCallViewModel.setLocation(fix.toSessionLocation(geofence))
            }

            RollCallScreen(
                ui = rollCall,
                geofence = geofence,
                onBack = { navController.popBackStack() },
                onMark = rollCallViewModel::mark,
                onMarkAllPresent = rollCallViewModel::markAllPresent,
                onSave = rollCallViewModel::save,
                onOpenMap = { navController.navigate(Routes.teacherRollCallMap(groupId)) },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_MAP) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            val school = rememberSchoolSite()
            TeacherRollCallMapScreen(
                groupId = groupId,
                school = school.site,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                onOpenOfflineMode = {
                    navController.navigate(Routes.teacherRollCallOffline(groupId))
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_OUTSIDE) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            val school = rememberSchoolSite()
            TeacherOutsideRadiusScreen(
                groupId = groupId,
                school = school.site,
                onBack = { navController.popBackStack() },
                onRetry = {
                    navController.navigate(Routes.teacherRollCall(groupId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL_OUTSIDE) { inclusive = true }
                    }
                },
                onBackToGroups = {
                    navController.popBackStack(Routes.TEACHER_GROUPS, inclusive = false)
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_OFFLINE) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            TeacherRollCallOfflineScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_SAVE) {
            PlaceholderScreen(
                title = "Guardar asistencia — Panel de Docente",
                next = "Volver al pase de lista",
                onNext = { navController.popBackStack() },
            )
        }
        composable(Routes.TEACHER_INCIDENT_NEW) {
            val container = rememberAppContainer()
            val incidentViewModel: TeacherIncidentViewModel =
                viewModel(factory = container.viewModelFactory)
            val incidentUi by incidentViewModel.state.collectAsState()
            val school = rememberSchoolSite()
            TeacherIncidentNewScreen(
                ui = incidentUi,
                school = school.site,
                onBack = { navController.popBackStack() },
                onSave = incidentViewModel::save,
                onSelectStudent = incidentViewModel::selectStudent,
                onSeverityChange = incidentViewModel::setSeverity,
                onDescriptionChange = incidentViewModel::setDescription,
            )
        }
        composable(Routes.TEACHER_TASK_NEW) {
            TeacherTaskNewScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_NOTICE_NEW) {
            TeacherNoticeNewScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_CHAT) {
            TeacherChatScreen(
                onOpenConversation = { conversationId ->
                    navController.navigate(Routes.teacherConversation(conversationId))
                },
                onSelectTab = { navController.openTeacherTab(it) },
            )
        }
        composable(Routes.TEACHER_CONVERSATION) {
            TeacherConversationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEACHER_HISTORY) {
            val container = rememberAppContainer()
            val historyViewModel: TeacherHistoryViewModel = viewModel(
                factory = container.viewModelFactory,
            )
            val history by historyViewModel.state.collectAsState()
            TeacherHistoryScreen(
                ui = history,
                onSelectTab = { navController.openTeacherTab(it) },
                onRetry = historyViewModel::refresh,
                onToggleSession = historyViewModel::toggleSession,
            )
        }
        composable(Routes.TEACHER_NOTIFICATIONS) {
            TeacherNotificationsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TEACHER_PROFILE) {
            TeacherProfileScreen(
                onSelectTab = { navController.openTeacherTab(it) },
                onLogout = { sessionViewModel.signOut() },
            )
        }

        // ---- Administrator ----
        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onNotificaciones = { navController.navigate(Routes.ADMIN_NOTIFICATIONS) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(
                onAgregarUsuario = { navController.navigate(Routes.ADMIN_NEW_STUDENT) },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_NEW_STUDENT) {
            AdminNewStudentScreen(
                onBack = { navController.popBackStack() },
                onGuardar = { navController.popBackStack() },
            )
        }
        composable(Routes.ADMIN_GROUPS) {
            AdminGroupsScreen(
                onBack = { navController.popBackStack() },
                onAgregarGrupo = {},
                onGrupo = { name ->
                    // La ruta no admite espacios ni "°": 3° B -> 3B
                    navController.navigate(Routes.adminGroupDetail(name.replace(" ", "").replace("°", "")))
                },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_GROUP_DETAIL) { entry ->
            val groupId = entry.arguments?.getString("groupId") ?: "3B"
            val groupLabel = if (groupId.length == 2) "${groupId.first()}° ${groupId.last()}" else groupId
            AdminGroupDetailScreen(
                groupLabel = groupLabel,
                onBack = { navController.popBackStack() },
                onMore = {},
                onAddStudent = { navController.navigate(Routes.ADMIN_NEW_STUDENT) },
            )
        }
        composable(Routes.ADMIN_REPORTS) {
            AdminReportsScreen(
                onDescargar = {},
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_INCIDENTS) {
            AdminIncidentsScreen(
                onBack = { navController.popBackStack() },
                onFiltrar = {},
                onIncidente = {},
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_CAMPUS) {
            AdminCampusScreen(
                onBack = { navController.popBackStack() },
                onAyuda = {},
                onGuardar = { navController.popBackStack() },
            )
        }
        composable(Routes.ADMIN_MORE) {
            AdminMoreScreen(
                onCampus = { navController.navigate(Routes.ADMIN_CAMPUS) },
                onIncidents = { navController.navigate(Routes.ADMIN_INCIDENTS) },
                onNotifications = { navController.navigate(Routes.ADMIN_NOTIFICATIONS) },
                onProfile = { navController.navigate(Routes.ADMIN_PROFILE) },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
            )
        }
        composable(Routes.ADMIN_NOTIFICATIONS) {
            AdminNotificationsScreen(
                onBack = { navController.popBackStack() },
                onRevisar = { navController.navigate(Routes.ADMIN_INCIDENTS) },
            )
        }
        composable(Routes.ADMIN_PROFILE) {
            AdminProfileScreen(
                onLogout = { sessionViewModel.signOut() },
                onBack = { navController.popBackStack() },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
            )
        }
    }
}

/**
 * Switches the parent shell to [tab] while keeping a clean stack of
 * [PARENT_HOME, destination], so the system back button always returns
 * to the dashboard instead of walking through previously visited tabs.
 */
private fun NavHostController.openParentTab(tab: ParentTab) {
    val route = when (tab) {
        ParentTab.Inicio -> Routes.PARENT_HOME
        ParentTab.Asistencia -> Routes.PARENT_ATTENDANCE
        ParentTab.Tareas -> Routes.PARENT_TASKS
        ParentTab.Chat -> Routes.PARENT_CHAT
        ParentTab.Perfil -> Routes.PARENT_PROFILE
    }
    navigate(route) {
        popUpTo(Routes.PARENT_HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * Switches the teacher shell to [tab] while keeping a clean stack of
 * [TEACHER_HOME, destination], so the system back button always returns
 * to the teacher dashboard instead of walking through previously visited tabs.
 */
private fun NavHostController.openTeacherTab(tab: TeacherTab) {
    val route = when (tab) {
        TeacherTab.Inicio -> Routes.TEACHER_HOME
        TeacherTab.Grupos -> Routes.TEACHER_GROUPS
        TeacherTab.Chat -> Routes.TEACHER_CHAT
        TeacherTab.Historial -> Routes.TEACHER_HISTORY
        TeacherTab.Perfil -> Routes.TEACHER_PROFILE
    }
    navigate(route) {
        popUpTo(Routes.TEACHER_HOME) { inclusive = false }
        launchSingleTop = true
    }
}

/**
 * Temporary screen used until the real design for that route is ported.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    next: String,
    onNext: () -> Unit,
    extraActions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Pantalla pendiente de portar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onNext) {
                Text(next)
            }
            extraActions.forEach { (label, action) ->
                Button(onClick = action) {
                    Text(label)
                }
            }
        }
    }
}
