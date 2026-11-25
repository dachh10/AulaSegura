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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.location.evaluateGeofence
import com.example.aulasegura.location.honouring
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.location.rememberSchoolSite
import com.example.aulasegura.location.toSessionLocation
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.RegistrationKind
import com.example.aulasegura.ui.screens.access.AccessStateUiState
import com.example.aulasegura.ui.screens.access.AccessStateViewModel
import com.example.aulasegura.ui.screens.access.AccountReviewScreen
import com.example.aulasegura.ui.screens.access.ForgotPasswordScreen
import com.example.aulasegura.ui.screens.access.LinkChildScreen
import com.example.aulasegura.ui.screens.access.LinkChildMode
import com.example.aulasegura.ui.screens.access.LoginScreen
import com.example.aulasegura.ui.screens.access.LoginViewModel
import com.example.aulasegura.ui.screens.access.OnboardingScreen
import com.example.aulasegura.ui.screens.access.PermissionsScreen
import com.example.aulasegura.ui.screens.access.RegisterScreen
import com.example.aulasegura.ui.screens.access.RegisterViewModel
import com.example.aulasegura.ui.screens.access.ResubmitTeacherScreen
import com.example.aulasegura.ui.screens.access.SessionState
import com.example.aulasegura.ui.screens.access.SessionViewModel
import com.example.aulasegura.ui.screens.access.SplashScreen
import com.example.aulasegura.ui.screens.access.VerifyEmailScreen
import com.example.aulasegura.ui.screens.access.nextRoute
import com.example.aulasegura.ui.screens.admin.AdminDashboardScreen
import com.example.aulasegura.ui.screens.admin.formatTime12Hour
import com.example.aulasegura.ui.screens.admin.AdminCampusScreen
import com.example.aulasegura.ui.screens.admin.AdminGroupsScreen
import com.example.aulasegura.ui.screens.admin.AdminGroupDetailScreen
import com.example.aulasegura.ui.screens.admin.AdminIncidentsScreen
import com.example.aulasegura.ui.screens.admin.AdminJustificationsScreen
import com.example.aulasegura.ui.screens.admin.AdminJustificationsViewModel
import com.example.aulasegura.ui.screens.admin.AdminMoreScreen
import com.example.aulasegura.ui.screens.admin.AdminNewStudentScreen
import com.example.aulasegura.ui.screens.admin.AdminNotificationsScreen
import com.example.aulasegura.ui.screens.admin.AdminProfileScreen
import com.example.aulasegura.ui.screens.admin.AdminReportsScreen
import com.example.aulasegura.ui.screens.admin.ReportPdfExporter
import com.example.aulasegura.ui.screens.admin.AdminUsersScreen
import java.time.LocalTime
import com.example.aulasegura.ui.screens.admin.ReviewStudentScreen
import com.example.aulasegura.ui.screens.admin.ScanStudentScreen
import com.example.aulasegura.ui.screens.admin.TeacherRequestsScreen
import com.example.aulasegura.ui.screens.admin.UngroupedStudentsScreen
import com.example.aulasegura.ui.screens.parent.ParentAIScreen
import com.example.aulasegura.ui.screens.parent.ParentAttendanceScreen
import com.example.aulasegura.ui.screens.parent.ParentAttendanceUiState
import com.example.aulasegura.ui.screens.parent.ParentAttendanceViewModel
import com.example.aulasegura.ui.screens.parent.ParentAIViewModel
import com.example.aulasegura.ui.screens.parent.ParentChatScreen
import com.example.aulasegura.ui.screens.parent.ParentChatViewModel
import com.example.aulasegura.ui.screens.parent.ParentConversationScreen
import com.example.aulasegura.ui.screens.parent.ParentConversationViewModel
import com.example.aulasegura.ui.screens.parent.ParentHomeScreen
import com.example.aulasegura.ui.screens.parent.ParentHomeUiState
import com.example.aulasegura.ui.screens.parent.ParentHomeViewModel
import com.example.aulasegura.ui.screens.parent.ParentIncidentDetailScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentDetailViewModel
import com.example.aulasegura.ui.screens.parent.ParentIncidentMapScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentsScreen
import com.example.aulasegura.ui.screens.parent.ParentIncidentsViewModel
import com.example.aulasegura.ui.screens.parent.ParentJustifyScreen
import com.example.aulasegura.ui.screens.parent.ParentJustifyViewModel
import com.example.aulasegura.ui.screens.parent.ParentNoticesScreen
import com.example.aulasegura.ui.screens.parent.ParentNoticesViewModel
import com.example.aulasegura.ui.screens.parent.ParentNotificationsScreen
import com.example.aulasegura.ui.screens.parent.ParentNotificationsViewModel
import com.example.aulasegura.ui.screens.parent.ParentProfileScreen
import com.example.aulasegura.ui.screens.parent.ParentProfileViewModel
import com.example.aulasegura.ui.screens.parent.ParentTaskDetailScreen
import com.example.aulasegura.ui.screens.parent.ParentTaskDetailViewModel
import com.example.aulasegura.ui.screens.parent.ParentTasksScreen
import com.example.aulasegura.ui.screens.parent.ParentTasksViewModel
import com.example.aulasegura.ui.screens.teacher.RollCallScreen
import com.example.aulasegura.ui.screens.teacher.RollCallViewModel
import com.example.aulasegura.ui.screens.teacher.RollCallViewModelFactory
import com.example.aulasegura.ui.screens.teacher.TeacherChatScreen
import com.example.aulasegura.ui.screens.teacher.TeacherChatViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherConversationViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherConversationScreen
import com.example.aulasegura.ui.screens.teacher.TeacherGroupDetailScreen
import com.example.aulasegura.ui.screens.teacher.TeacherGroupDetailViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsScreen
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHistoryScreen
import com.example.aulasegura.ui.screens.teacher.TeacherHistoryViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherIncidentNewScreen
import com.example.aulasegura.ui.screens.teacher.TeacherIncidentViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHomeScreen
import com.example.aulasegura.ui.screens.teacher.TeacherHomeViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherJustificationsScreen
import com.example.aulasegura.ui.screens.teacher.TeacherJustificationsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherLocationVerifyingScreen
import com.example.aulasegura.ui.screens.teacher.TeacherNoClassScreen
import com.example.aulasegura.ui.screens.teacher.TeacherNoticeNewScreen
import com.example.aulasegura.ui.screens.teacher.TeacherNoticeNewViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherProfileViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherNotificationsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherNotificationsScreen
import com.example.aulasegura.ui.screens.teacher.TeacherOutsideRadiusScreen
import com.example.aulasegura.ui.screens.teacher.TeacherProfileScreen
import com.example.aulasegura.ui.screens.teacher.TeacherRollCallMapScreen
import com.example.aulasegura.ui.screens.teacher.TeacherRollCallOfflineScreen
import com.example.aulasegura.ui.screens.teacher.TeacherTaskNewScreen
import com.example.aulasegura.ui.screens.teacher.TeacherTaskNewViewModel
import com.example.aulasegura.util.SchoolCalendar

/**
 * Navigation graph for the whole app.
 *
 * Each route resolves the session and wires a ViewModel from the container. The
 * screens for phases already delivered are real; the ones still pending keep
 * their visual port until their own block lands.
 */
@Composable
fun AulaSeguraNavHost(
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = viewModel(factory = rememberAppContainer().viewModelFactory),
    loginViewModel: LoginViewModel = viewModel(factory = rememberAppContainer().viewModelFactory),
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val sessionState by sessionViewModel.state.collectAsState()

    // A notification tap is only followed once there is a session: navigating to
    // a teacher screen from Splash would land on the login guard instead.
    LaunchedEffect(deepLink, sessionState) {
        if (sessionState !is SessionState.SignedIn) return@LaunchedEffect
        val route = resolveDeepLink(deepLink) ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        onDeepLinkHandled()
    }

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
            fadeIn(animationSpec = tween(140)) +
                slideInVertically(animationSpec = tween(140)) { height -> height / 40 }
        },
        exitTransition = { fadeOut(animationSpec = tween(100)) },
        popEnterTransition = {
            fadeIn(animationSpec = tween(140)) +
                slideInVertically(animationSpec = tween(140)) { height -> height / 40 }
        },
        popExitTransition = { fadeOut(animationSpec = tween(100)) },
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
                    is SessionState.SignedIn -> state.user.destinationRoute()
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
            val container = rememberAppContainer()

            LaunchedEffect(uiState.authenticatedRole) {
                if (uiState.authenticatedRole == null) return@LaunchedEffect
                val signedUser = container.authRepository.currentUser
                    ?: return@LaunchedEffect
                // Persist the verified-email flag here, not behind a button: a
                // teacher who confirms their mail and reopens the app is picked
                // up by the director's queue without any extra tap.
                val verified = loginViewModel.persistEmailVerificationIfNeeded()
                loginViewModel.consumeNavigation()
                sessionViewModel.onSignedIn(signedUser)
                val target = if (!verified) {
                    Routes.VERIFY_EMAIL
                } else {
                    signedUser.destinationRoute()
                }
                navController.navigate(target) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }

            LoginScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onLogin = loginViewModel::signIn,
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRegister = { navController.navigate(Routes.REGISTER) },
            )
        }
        composable(Routes.REGISTER) {
            val container = rememberAppContainer()
            val registerViewModel: RegisterViewModel =
                viewModel(factory = container.viewModelFactory)
            val registerUi by registerViewModel.uiState.collectAsState()

            LaunchedEffect(registerUi.succeeded) {
                if (!registerUi.succeeded) return@LaunchedEffect
                registerViewModel.consumeNavigation()
                val signedUser = container.authRepository.currentUser
                    ?: return@LaunchedEffect
                sessionViewModel.onSignedIn(signedUser)
                navController.navigate(Routes.VERIFY_EMAIL) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }

            RegisterScreen(
                onBack = { navController.popBackStack() },
                onCreated = { uri -> registerViewModel.submit(uri) },
                onLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                ui = registerUi,
                onKindChange = registerViewModel::setKind,
                onFullNameChange = registerViewModel::setFullName,
                onEmailChange = registerViewModel::setEmail,
                onPhoneChange = registerViewModel::setPhone,
                onPasswordChange = registerViewModel::setPassword,
                onConfirmPasswordChange = registerViewModel::setConfirmPassword,
                onChildNameChange = registerViewModel::setChildName,
                onSubjectChange = registerViewModel::setSubject,
                onSubjectIdChange = registerViewModel::setSubjectId,
                onGradeChange = registerViewModel::setGrade,
                onSectionChange = registerViewModel::setSection,
                onSelectionChanged = registerViewModel::refreshTakenSubjects,
                onAcceptedChange = registerViewModel::setAccepted,
                isLoading = registerUi.isLoading,
                errorMessage = registerUi.errorMessage,
            )
        }
        composable(Routes.RESUBMIT_TEACHER) {
            val container = rememberAppContainer()
            val vm: RegisterViewModel = viewModel(factory = container.viewModelFactory)
            val ui by vm.uiState.collectAsState()
            val submitting = remember { mutableStateOf(false) }

            fun submitResubmission() {
                if (submitting.value) return
                submitting.value = true
                vm.submitResubmission(onDone = {
                    submitting.value = false
                    navController.navigate(Routes.ACCOUNT_REVIEW) {
                        popUpTo(Routes.RESUBMIT_TEACHER) { inclusive = true }
                    }
                })
            }

            ResubmitTeacherScreen(
                ui = ui.copy(kind = RegistrationKind.DOCENTE, accepted = true),
                onGradeChange = vm::setGrade,
                onSectionChange = vm::setSection,
                onSubjectIdChange = vm::setSubjectId,
                onRefreshTakenSubjects = vm::refreshTakenSubjects,
                onSubmit = { submitResubmission() },
                onBack = { navController.popBackStack() },
                isLoading = submitting.value,
                errorMessage = ui.errorMessage,
            )
        }
        composable(Routes.VERIFY_EMAIL) {
            val container = rememberAppContainer()
            val vm: AccessStateViewModel = viewModel(factory = container.viewModelFactory)
            val ui by vm.uiState.collectAsState()

            LaunchedEffect(ui.proceed) {
                if (!ui.proceed) return@LaunchedEffect
                vm.consumeProceed()
                val target = container.authRepository.currentUser?.destinationRoute()
                    ?: ui.nextRoute()
                navController.navigate(target) {
                    popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
                }
            }

            VerifyEmailScreen(
                email = ui.email,
                message = ui.message,
                isLoading = ui.isLoading,
                onBack = { navController.popBackStack() },
                onChecked = vm::checkEmail,
                onResend = vm::resend,
                onSignOut = {
                    sessionViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.VERIFY_EMAIL) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ACCOUNT_REVIEW) {
            val container = rememberAppContainer()
            val vm: AccessStateViewModel = viewModel(factory = container.viewModelFactory)
            val ui by vm.uiState.collectAsState()

            LaunchedEffect(ui.proceed) {
                if (!ui.proceed) return@LaunchedEffect
                vm.consumeProceed()
                navController.navigate(ui.nextRoute()) {
                    popUpTo(Routes.ACCOUNT_REVIEW) { inclusive = true }
                }
            }

            AccountReviewScreen(
                rejected = ui.isRejected,
                isLoading = ui.isLoading,
                onBack = { navController.popBackStack() },
                onRefresh = vm::refreshState,
                onResubmit = { navController.navigate(Routes.RESUBMIT_TEACHER) },
                onSignOut = {
                    sessionViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ACCOUNT_REVIEW) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.LINK_CHILD,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = when (entry.arguments?.getString("mode")) {
                "add" -> LinkChildMode.ADD
                else -> LinkChildMode.PENDING
            }
            val container = rememberAppContainer()
            // ADD mode is known here, before the ViewModel is constructed: the
            // linking listener must not bounce a tutor who came to add a child.
            val vm: AccessStateViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AccessStateViewModel(
                            auth = container.authRepository,
                            parents = container.parentRepository,
                            addMode = mode == LinkChildMode.ADD,
                        )
                    }
                },
            )
            val ui by vm.uiState.collectAsState()

            LaunchedEffect(ui.proceed) {
                if (!ui.proceed) return@LaunchedEffect
                vm.consumeProceed()
                navController.navigate(Routes.PARENT_HOME) {
                    popUpTo(Routes.LINK_CHILD) { inclusive = true }
                }
            }

            LinkChildScreen(
                uid = ui.uid,
                mode = mode,
                childName = ui.childName,
                rejected = ui.vinculacion == "rechazada",
                isLoading = ui.isLoading,
                message = ui.message,
                onChildNameChange = vm::setChildName,
                onSaveChildName = vm::saveChildName,
                knownChildIds = ui.knownChildIds,
                onQrGenerated = { known -> vm.watchForNewChild(known) },
                onBack = { navController.popBackStack() },
                onRefresh = vm::refreshState,
                onSignOut = {
                    sessionViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
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
            // Same source as the teacher's home: the session profile Splash
            // already resolved, so the greeting cannot drift from the profile.
            val parent = rememberAppContainer().authRepository.currentUser
            val homeViewModel: ParentHomeViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val homeState by homeViewModel.state.collectAsState()
            val unreadNotifications by homeViewModel.unreadNotifications.collectAsState()
            val homeLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(homeLifecycle) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) homeViewModel.refresh()
                }
                homeLifecycle.lifecycle.addObserver(observer)
                onDispose { homeLifecycle.lifecycle.removeObserver(observer) }
            }

            ParentHomeScreen(
                ui = homeState,
                fullName = parent?.fullName.orEmpty(),
                photoUrl = parent?.photoUrl,
                onNotifications = { navController.navigate(Routes.PARENT_NOTIFICATIONS) },
                onQuickAvisos = { navController.navigate(Routes.PARENT_NOTICES) },
                onQuickIncidentes = { navController.navigate(Routes.PARENT_INCIDENTS) },
                onQuickJustificar = {
                    // The quick access justifies the child the card is showing;
                    // which absence is resolved by the form itself.
                    val childId = (homeState as? ParentHomeUiState.Loaded)?.child?.studentId
                    navController.navigate(Routes.parentJustify(studentId = childId))
                },
                onQuickAsistente = { navController.navigate(Routes.PARENT_AI) },
                onVerTareas = { navController.navigate(Routes.PARENT_TASKS) },
                onSelectChild = homeViewModel::select,
                onAddChild = { navController.navigate(Routes.linkChildRoute("add")) },
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_ATTENDANCE) {
            val attendanceViewModel: ParentAttendanceViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val attendanceState by attendanceViewModel.state.collectAsState()

            ParentAttendanceScreen(
                ui = attendanceState,
                onJustificar = { record ->
                    val loaded = attendanceState as? ParentAttendanceUiState.Loaded
                    navController.navigate(
                        Routes.parentJustify(
                            studentId = loaded?.studentId,
                            date = record.isoDate,
                            sessionId = record.sessionId,
                        ),
                    )
                },
                onPreviousMonth = attendanceViewModel::previousMonth,
                onNextMonth = attendanceViewModel::nextMonth,
                onCycleChild = attendanceViewModel::cycleChild,
                onSelectSubject = attendanceViewModel::selectSubject,
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_TASKS) {
            val tasksViewModel: ParentTasksViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val tasksState by tasksViewModel.state.collectAsState()

            ParentTasksScreen(
                ui = tasksState,
                onOpenTask = { taskId ->
                    navController.navigate(Routes.parentTaskDetail(taskId))
                },
                onSelectFilter = tasksViewModel::selectFilter,
                onCycleChild = tasksViewModel::cycleChild,
                onRetry = tasksViewModel::refresh,
                onSelectTab = { navController.openParentTab(it) },
            )
        }
        composable(Routes.PARENT_TASK_DETAIL) { entry ->
            val taskId = entry.arguments?.getString("taskId").orEmpty()
            val container = rememberAppContainer()
            val factory = remember(taskId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        container.viewModelFactory.parentTaskDetailViewModel(taskId) as T
                }
            }
            val detailViewModel: ParentTaskDetailViewModel = viewModel(factory = factory)
            val detailState by detailViewModel.state.collectAsState()

            ParentTaskDetailScreen(
                ui = detailState,
                onBack = { navController.popBackStack() },
                onRetry = detailViewModel::refresh,
            )
        }
        composable(Routes.PARENT_NOTICES) {
            val noticesViewModel: ParentNoticesViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val noticesState by noticesViewModel.state.collectAsState()

            ParentNoticesScreen(
                ui = noticesState,
                onBack = { navController.popBackStack() },
                onRetry = noticesViewModel::refresh,
                onSelectFilter = noticesViewModel::selectFilter,
                onMarkRead = noticesViewModel::markRead,
            )
        }
        composable(Routes.PARENT_INCIDENTS) {
            val incidentsViewModel: ParentIncidentsViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val incidentsState by incidentsViewModel.state.collectAsState()

            ParentIncidentsScreen(
                ui = incidentsState,
                onBack = { navController.popBackStack() },
                onOpenIncident = { incidentId ->
                    navController.navigate(Routes.parentIncidentDetail(incidentId))
                },
                onCycleChild = incidentsViewModel::cycleChild,
                onRetry = incidentsViewModel::refresh,
            )
        }
        composable(Routes.PARENT_INCIDENT_DETAIL) { entry ->
            val incidentId = entry.arguments?.getString("incidentId").orEmpty()
            val container = rememberAppContainer()
            val factory = remember(incidentId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        container.viewModelFactory.parentIncidentDetailViewModel(incidentId) as T
                }
            }
            val incidentViewModel: ParentIncidentDetailViewModel = viewModel(factory = factory)
            val incidentState by incidentViewModel.state.collectAsState()

            ParentIncidentDetailScreen(
                ui = incidentState,
                onBack = { navController.popBackStack() },
                onOpenMap = {
                    navController.navigate(Routes.parentIncidentMap(incidentId))
                },
                onTalkToTeacher = { navController.navigate(Routes.PARENT_CHAT) },
                onAcknowledge = incidentViewModel::acknowledge,
                onRetry = incidentViewModel::refresh,
            )
        }
        composable(Routes.PARENT_INCIDENT_MAP) { entry ->
            val incidentId = entry.arguments?.getString("incidentId").orEmpty()
            val container = rememberAppContainer()
            val factory = remember(incidentId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        container.viewModelFactory.parentIncidentDetailViewModel(incidentId) as T
                }
            }
            val incidentViewModel: ParentIncidentDetailViewModel = viewModel(factory = factory)
            val incidentState by incidentViewModel.state.collectAsState()

            ParentIncidentMapScreen(
                ui = incidentState,
                onBack = { navController.popBackStack() },
                onTalkToTeacher = { navController.navigate(Routes.PARENT_CHAT) },
                onAcknowledge = incidentViewModel::acknowledge,
                onRetry = incidentViewModel::refresh,
            )
        }
        // Optional query arguments, so the plain route (the notification deep link)
        // still matches this destination and the form resolves the absence itself.
        composable(
            route = Routes.PARENT_JUSTIFY_ARGS,
            arguments = listOf(
                navArgument("studentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val studentId = entry.arguments?.getString("studentId")
            val absenceDate = entry.arguments?.getString("date")
            val absenceSessionId = entry.arguments?.getString("sessionId")
            val container = rememberAppContainer()
            val factory = remember(studentId, absenceDate, absenceSessionId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        container.viewModelFactory
                            .parentJustifyViewModel(studentId, absenceDate, absenceSessionId) as T
                }
            }
            val justifyViewModel: ParentJustifyViewModel = viewModel(factory = factory)
            val justifyState by justifyViewModel.state.collectAsState()

            ParentJustifyScreen(
                ui = justifyState,
                onBack = { navController.popBackStack() },
                // The form already stored the receipt and queued the write; leaving
                // it is the only confirmation the tutor needs.
                onSent = { navController.popBackStack() },
                onRetry = justifyViewModel::refresh,
                onSelectReason = justifyViewModel::selectReason,
                onDetailsChange = justifyViewModel::updateDetails,
                onAttach = justifyViewModel::attach,
                onRemoveReceipt = justifyViewModel::removeReceipt,
                onToggleAbsence = justifyViewModel::toggleAbsence,
                onApplyToAllChange = justifyViewModel::setApplyToAll,
                onSend = justifyViewModel::send,
            )
        }
        composable(Routes.PARENT_CHAT) {
            val container = rememberAppContainer()
            val chatViewModel: ParentChatViewModel = viewModel(factory = container.viewModelFactory)
            val chatUi by chatViewModel.state.collectAsState()
            val openedConversation by chatViewModel.openedConversation.collectAsState()

            // Starting a chat is a suspend call, so the id is consumed here and
            // cleared right after; a rotation does not reopen it.
            LaunchedEffect(openedConversation) {
                openedConversation?.let { conversationId ->
                    navController.navigate(Routes.parentConversation(conversationId))
                    chatViewModel.openedHandled()
                }
            }

            ParentChatScreen(
                ui = chatUi,
                onOpenConversation = { conversationId ->
                    navController.navigate(Routes.parentConversation(conversationId))
                },
                onSelectTab = { navController.openParentTab(it) },
                onNuevoMensaje = chatViewModel::openContacts,
                onQueryChange = chatViewModel::setQuery,
                onContactsDismiss = chatViewModel::dismissContacts,
                onStartConversation = chatViewModel::startConversation,
            )
        }
        composable(Routes.PARENT_CONVERSATION) { entry ->
            val conversationId = entry.arguments?.getString("conversationId").orEmpty()
            val container = rememberAppContainer()
            val conversationViewModel: ParentConversationViewModel =
                viewModel(factory = container.viewModelFactory)
            val conversationUi by conversationViewModel.state.collectAsState()

            LaunchedEffect(conversationId) {
                if (conversationId.isNotBlank()) {
                    conversationViewModel.open(conversationId)
                }
            }

            ParentConversationScreen(
                ui = conversationUi,
                onBack = { navController.popBackStack() },
                onDraftChange = conversationViewModel::setDraft,
                onSend = conversationViewModel::send,
            )
        }
        composable(Routes.PARENT_AI) {
            val container = rememberAppContainer()
            val aiViewModel: ParentAIViewModel = viewModel(factory = container.viewModelFactory)
            val aiUi by aiViewModel.state.collectAsState()

            ParentAIScreen(
                ui = aiUi,
                onBack = { navController.popBackStack() },
                onDraftChange = aiViewModel::setDraft,
                onSend = aiViewModel::send,
                onAsk = aiViewModel::ask,
                onRetry = aiViewModel::refresh,
                onOpenTask = { taskId ->
                    navController.navigate(Routes.parentTaskDetail(taskId))
                },
            )
        }
        composable(Routes.PARENT_NOTIFICATIONS) {
            val container = rememberAppContainer()
            val notificationsViewModel: ParentNotificationsViewModel =
                viewModel(factory = container.viewModelFactory)
            val notificationsUi by notificationsViewModel.state.collectAsState()
            val notificationDeepLink by notificationsViewModel.pendingDeepLink.collectAsState()

            LaunchedEffect(notificationDeepLink) {
                if (notificationDeepLink == null) return@LaunchedEffect
                // The document comes from outside the app: the whitelist decides
                // whether it opens anything. An unknown route is consumed too, or
                // a later tap with the same value would never fire again.
                resolveDeepLink(notificationDeepLink)?.let { route ->
                    navController.navigate(route) { launchSingleTop = true }
                }
                notificationsViewModel.deepLinkHandled()
            }

            ParentNotificationsScreen(
                onBack = { navController.popBackStack() },
                ui = notificationsUi,
                onFilterChange = notificationsViewModel::setFilter,
                onMarkAllRead = notificationsViewModel::markAllRead,
                onOpen = notificationsViewModel::open,
            )
        }
        composable(Routes.PARENT_PROFILE) {
            val profileViewModel: ParentProfileViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val profileUi by profileViewModel.ui.collectAsState()
            LaunchedEffect(Unit) { profileViewModel.refresh() }
            ParentProfileScreen(
                ui = profileUi,
                onSelectTab = { navController.openParentTab(it) },
                onLogout = { sessionViewModel.signOut() },
                onChangePhoto = profileViewModel::changePhoto,
                onDismissMessage = profileViewModel::clearMessage,
                onEditProfile = profileViewModel::openEditProfile,
                onChangePassword = profileViewModel::openChangePassword,
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onEditNameChange = profileViewModel::setEditName,
                onSaveProfile = profileViewModel::saveProfile,
                onCurrentPasswordChange = profileViewModel::setCurrentPassword,
                onNewPasswordChange = profileViewModel::setNewPassword,
                onConfirmPasswordChange = profileViewModel::setConfirmPassword,
                onSavePassword = profileViewModel::savePassword,
                onCloseDialog = profileViewModel::closeSection,
            )
        }

        // ---- Teacher ----
        composable(Routes.TEACHER_HOME) {
            // The session profile is already resolved by Splash; reading it here
            // keeps the greeting and avatar on the same source as the profile screen.
            val teacher = rememberAppContainer().authRepository.currentUser
            val homeViewModel: TeacherHomeViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val homeState by homeViewModel.state.collectAsState()

            TeacherHomeScreen(
                ui = homeState,
                fullName = teacher?.fullName.orEmpty(),
                subjects = teacher?.subjects.orEmpty(),
                photoUrl = teacher?.photoUrl,
                onIrAGrupos = { navController.navigate(Routes.TEACHER_GROUPS) },
                // Con horario, "Pasar lista" va directo al grupo de la clase; sin
                // él no hay grupo que adivinar: se abre la lista de grupos.
                onIrAPaseLista = { groupId, subjectId ->
                    if (groupId != null) {
                        navController.navigate(Routes.teacherRollCall(groupId, subjectId))
                    } else {
                        navController.navigate(Routes.TEACHER_GROUPS)
                    }
                },
                onIrARegistrarIncidente = { navController.navigate(Routes.TEACHER_INCIDENT_NEW) },
                onIrAPublicarTarea = { navController.navigate(Routes.TEACHER_TASK_NEW) },
                onIrAPublicarAviso = { navController.navigate(Routes.TEACHER_NOTICE_NEW) },
                onIrAJustificaciones = { navController.navigate(Routes.TEACHER_JUSTIFICATIONS) },
                onRefreshJustifications = homeViewModel::refreshJustifications,
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
                onPasarLista = { groupId, subjectId -> navController.navigate(Routes.teacherRollCall(groupId, subjectId)) },
                onVerAlumnos = { groupId -> navController.navigate(Routes.teacherGroupDetail(groupId)) },
            )
        }
        composable(Routes.TEACHER_GROUP_DETAIL) { entry ->
            // The group id comes from the route, so the ViewModel is keyed by it:
            // two groups in the back stack must not share a state.
            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val groupDetailViewModel: TeacherGroupDetailViewModel = viewModel(
                key = "group-detail-$groupId",
                factory = rememberAppContainer().viewModelFactory,
            )
            val detail by groupDetailViewModel.state.collectAsState()

            LaunchedEffect(groupId) { groupDetailViewModel.open(groupId) }

            TeacherGroupDetailScreen(
                ui = detail,
                onBack = { navController.popBackStack() },
                onPasarLista = { subjectId -> navController.navigate(Routes.teacherRollCall(groupId, subjectId)) },
                onNuevaTarea = { navController.navigate(Routes.teacherTaskNewRoute(groupId)) },
                onNuevoAviso = { navController.navigate(Routes.teacherNoticeNewRoute(groupId)) },
                // El formulario elige el alumno y de ahí sale el grupo: no hay
                // ruta con grupo fijo, así que se abre el mismo formulario real.
                onRegistrarIncidente = { navController.navigate(Routes.TEACHER_INCIDENT_NEW) },
                onMonthChange = groupDetailViewModel::changeMonth,
                onToggleIncident = groupDetailViewModel::toggleIncident,
            )
        }
        // GPS-first: entrar a "Pase de lista" dispara la verificación real de
        // ubicación. La lista de alumnos sólo se alcanza dentro del radio.
        composable(Routes.TEACHER_ROLL_CALL) { entry ->
            // De lunes a viernes. En fin de semana no se abre una sesión nueva:
            // una lista del domingo sería una asistencia que no corresponde a un
            // día de clases. Las listas ya guardadas no se tocan.
            if (!SchoolCalendar.isClassDay(System.currentTimeMillis())) {
                TeacherNoClassScreen(
                    onBack = { navController.popBackStack() },
                    onGoHome = {
                        navController.popBackStack(Routes.TEACHER_HOME, inclusive = false)
                    },
                )
                return@composable
            }

            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
            val school = rememberSchoolSite()
            val site = school.site
            if (site != null && !site.isWithinSchedule(LocalTime.now())) {
                TeacherNoClassScreen(
                    onBack = { navController.popBackStack() },
                    onGoHome = {
                        navController.popBackStack(Routes.TEACHER_HOME, inclusive = false)
                    },
                    title = "Fuera del horario de pase de lista",
                    message = "El horario configurado es de " +
                        "${formatTime12Hour(site.entryTime.orEmpty())} a " +
                        "${formatTime12Hour(site.exitTime.orEmpty())}. " +
                        "Las listas ya guardadas siguen en el historial.",
                )
                return@composable
            }
            TeacherLocationVerifyingScreen(
                groupId = groupId,
                school = school.site,
                schoolLoaded = school.loaded,
                onBack = { navController.popBackStack() },
                onInsideRadius = {
                    navController.navigate(Routes.teacherRollCallInside(groupId, subjectId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
                onOutsideRadius = {
                    navController.navigate(Routes.teacherRollCallOutside(groupId, subjectId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
                onContinueUnverified = {
                    navController.navigate(Routes.teacherRollCallInside(groupId, subjectId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_INSIDE) { entry ->
            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
            val container = rememberAppContainer()
            val rollCallViewModel: RollCallViewModel = viewModel(
                key = "roll-call-$groupId-$subjectId",
                factory = RollCallViewModelFactory(
                    groupId = groupId,
                    subjectId = subjectId,
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
                // When the school opted out of GPS, even a fix inside the radius
                // must not produce a verified banner or a verified session.
                evaluateGeofence(location.fix, school.site).honouring(school.site)
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
                onOpenMap = { navController.navigate(Routes.teacherRollCallMap(groupId, subjectId)) },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_MAP) { entry ->
            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
            val school = rememberSchoolSite()
            TeacherRollCallMapScreen(
                groupId = groupId,
                school = school.site,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                onOpenOfflineMode = {
                    navController.navigate(Routes.teacherRollCallOffline(groupId, subjectId))
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_OUTSIDE) { entry ->
            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
            val school = rememberSchoolSite()
            TeacherOutsideRadiusScreen(
                groupId = groupId,
                school = school.site,
                onBack = { navController.popBackStack() },
                onRetry = {
                    navController.navigate(Routes.teacherRollCall(groupId, subjectId)) {
                        popUpTo(Routes.TEACHER_ROLL_CALL_OUTSIDE) { inclusive = true }
                    }
                },
                onBackToGroups = {
                    navController.popBackStack(Routes.TEACHER_GROUPS, inclusive = false)
                },
            )
        }
        composable(Routes.TEACHER_ROLL_CALL_OFFLINE) { entry ->
            val groupId = entry.arguments?.getString("groupId").orEmpty()
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
                onCategoryChange = incidentViewModel::setCategory,
                onDescriptionChange = incidentViewModel::setDescription,
                onPhotoTaken = incidentViewModel::addPhoto,
                onRemoveEvidence = incidentViewModel::removeEvidence,
            )
        }
        composable(
            route = Routes.TEACHER_TASK_NEW,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val container = rememberAppContainer()
            val taskViewModel: TeacherTaskNewViewModel =
                viewModel(factory = container.viewModelFactory)
            val initialGroupId = entry.arguments?.getString("groupId")
            LaunchedEffect(initialGroupId) {
                if (!initialGroupId.isNullOrBlank()) taskViewModel.preselectGroup(initialGroupId)
            }
            val taskUi by taskViewModel.state.collectAsState()
            TeacherTaskNewScreen(
                ui = taskUi,
                onBack = { navController.popBackStack() },
                onSelectGroup = taskViewModel::selectGroup,
                onSelectSubject = taskViewModel::selectSubject,
                onTitleChange = taskViewModel::setTitle,
                onDescriptionChange = taskViewModel::setDescription,
                onDueDateChange = taskViewModel::setDueDate,
                onDueTimeChange = taskViewModel::setDueTime,
                onAttachPdf = taskViewModel::attachPdf,
                onRemoveAttachment = taskViewModel::removeAttachment,
                onPublish = taskViewModel::publish,
            )
        }
        composable(
            route = Routes.TEACHER_NOTICE_NEW,
            arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val container = rememberAppContainer()
            val noticeViewModel: TeacherNoticeNewViewModel =
                viewModel(factory = container.viewModelFactory)
            val initialGroupId = entry.arguments?.getString("groupId")
            LaunchedEffect(initialGroupId) {
                if (!initialGroupId.isNullOrBlank()) noticeViewModel.preselectGroup(initialGroupId)
            }
            val noticeUi by noticeViewModel.state.collectAsState()
            TeacherNoticeNewScreen(
                ui = noticeUi,
                onBack = { navController.popBackStack() },
                onToggleGroup = noticeViewModel::toggleGroup,
                onToggleAllGroups = noticeViewModel::toggleAllGroups,
                onTitleChange = noticeViewModel::setTitle,
                onBodyChange = noticeViewModel::setBody,
                onUrgentChange = noticeViewModel::setUrgent,
                onPublish = noticeViewModel::publish,
            )
        }
        composable(Routes.TEACHER_CHAT) {
            val container = rememberAppContainer()
            val chatViewModel: TeacherChatViewModel =
                viewModel(factory = container.viewModelFactory)
            val chatUi by chatViewModel.state.collectAsState()
            val openedConversation by chatViewModel.openedConversation.collectAsState()

            // A new chat is created in the ViewModel; navigating is the screen's
            // job, so the id is consumed here and cleared right after.
            LaunchedEffect(openedConversation) {
                openedConversation?.let { conversationId ->
                    navController.navigate(Routes.teacherConversation(conversationId))
                    chatViewModel.openedHandled()
                }
            }

            TeacherChatScreen(
                ui = chatUi,
                onOpenConversation = { conversationId ->
                    navController.navigate(Routes.teacherConversation(conversationId))
                },
                onSelectTab = { navController.openTeacherTab(it) },
                onNuevoMensaje = chatViewModel::openContacts,
                onQueryChange = chatViewModel::setQuery,
                onFilterChange = chatViewModel::setFilter,
                onContactsDismiss = chatViewModel::dismissContacts,
                onStartConversation = chatViewModel::startConversation,
            )
        }
        composable(Routes.TEACHER_CONVERSATION) { entry ->
            val conversationId = entry.arguments?.getString("conversationId").orEmpty()
            val container = rememberAppContainer()
            val conversationViewModel: TeacherConversationViewModel =
                viewModel(factory = container.viewModelFactory)
            val conversationUi by conversationViewModel.state.collectAsState()

            LaunchedEffect(conversationId) {
                if (conversationId.isNotBlank()) {
                    conversationViewModel.open(conversationId)
                }
            }

            TeacherConversationScreen(
                ui = conversationUi,
                onBack = { navController.popBackStack() },
                onDraftChange = conversationViewModel::setDraft,
                onSend = conversationViewModel::send,
            )
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
                onToggleIncident = historyViewModel::toggleIncident,
                onMonthChange = historyViewModel::changeMonth,
            )
        }
        composable(Routes.TEACHER_JUSTIFICATIONS) {
            val justificationsViewModel: TeacherJustificationsViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val justificationsUi by justificationsViewModel.state.collectAsState()

            TeacherJustificationsScreen(
                ui = justificationsUi,
                onBack = { navController.popBackStack() },
                onRefresh = justificationsViewModel::refresh,
                onDecide = justificationsViewModel::decide,
                onNoticeShown = justificationsViewModel::noticeShown,
                onErrorShown = justificationsViewModel::errorShown,
            )
        }
        composable(Routes.TEACHER_NOTIFICATIONS) {
            val container = rememberAppContainer()
            val notificationsViewModel: TeacherNotificationsViewModel =
                viewModel(factory = container.viewModelFactory)
            val notificationsUi by notificationsViewModel.state.collectAsState()
            val notificationDeepLink by notificationsViewModel.pendingDeepLink.collectAsState()

            LaunchedEffect(notificationDeepLink) {
                val route = resolveDeepLink(notificationDeepLink) ?: return@LaunchedEffect
                navController.navigate(route) { launchSingleTop = true }
                notificationsViewModel.deepLinkHandled()
            }

            TeacherNotificationsScreen(
                ui = notificationsUi,
                onBack = { navController.popBackStack() },
                onFilterChange = notificationsViewModel::setFilter,
                onMarkAllRead = notificationsViewModel::markAllRead,
                onOpen = notificationsViewModel::open,
            )
        }
        composable(Routes.TEACHER_PROFILE) {
            val profileViewModel: TeacherProfileViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val profileUi by profileViewModel.ui.collectAsState()
            LaunchedEffect(Unit) { profileViewModel.refresh() }
            TeacherProfileScreen(
                ui = profileUi,
                onSelectTab = { navController.openTeacherTab(it) },
                onLogout = { sessionViewModel.signOut() },
                onChangePhoto = profileViewModel::changePhoto,
                onDismissMessage = profileViewModel::clearMessage,
                onEditProfile = profileViewModel::openEditProfile,
                onChangePassword = profileViewModel::openChangePassword,
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onEditNameChange = profileViewModel::setEditName,
                onEditSubjectsChange = profileViewModel::setEditSubjects,
                onSaveProfile = profileViewModel::saveProfile,
                onCurrentPasswordChange = profileViewModel::setCurrentPassword,
                onNewPasswordChange = profileViewModel::setNewPassword,
                onConfirmPasswordChange = profileViewModel::setConfirmPassword,
                onSavePassword = profileViewModel::savePassword,
                onCloseDialog = profileViewModel::closeSection,
            )
        }

        // ---- Administrator ----
        composable(Routes.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onNotificaciones = { navController.navigate(Routes.ADMIN_NOTIFICATIONS) },
                onSolicitudes = { navController.navigate(Routes.ADMIN_TEACHER_REQUESTS) },
                onAlumnosSinGrupo = { navController.navigate(Routes.ADMIN_UNGROUPED_STUDENTS) },
                onEscanear = { navController.navigate(Routes.ADMIN_SCAN_STUDENT) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_SCAN_STUDENT) {
            ScanStudentScreen(
                onBack = { navController.popBackStack() },
                onFound = { qr ->
                    navController.navigate(
                        Routes.reviewStudentRoute(uid = qr.uid, alumno = qr.alumno),
                    )
                },
            )
        }
        composable(
            route = Routes.ADMIN_REVIEW_STUDENT,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("alumno") { type = NavType.StringType },
            ),
        ) { entry ->
            ReviewStudentScreen(
                uid = entry.arguments?.getString("uid").orEmpty(),
                alumno = entry.arguments?.getString("alumno").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ADMIN_TEACHER_REQUESTS) {
            TeacherRequestsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_UNGROUPED_STUDENTS) {
            UngroupedStudentsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_USERS) {
            AdminUsersScreen(
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
                onAddStudent = { navController.navigate(Routes.ADMIN_NEW_STUDENT) },
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
                onGrupo = { groupId ->
                    // El id del documento no tiene espacios ni "°".
                    navController.navigate(Routes.adminGroupDetail(groupId))
                },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onReportes = { navController.navigate(Routes.ADMIN_REPORTS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_GROUP_DETAIL) { entry ->
            val groupId = entry.arguments?.getString("groupId").orEmpty()
            val groupLabel = if (groupId.length == 2) "${groupId.first()}° ${groupId.last()}" else groupId
            AdminGroupDetailScreen(
                groupId = groupId,
                groupLabel = groupLabel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.ADMIN_REPORTS) {
            // The screen owns its ViewModel; pass its current state to the exporter
            // so no second ViewModel (and no stale snapshot) is created here.
            val context = LocalContext.current
            AdminReportsScreen(
                onDescargar = { state -> ReportPdfExporter.export(context, state) },
                onPanel = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                onUsuarios = { navController.navigate(Routes.ADMIN_USERS) },
                onGrupos = { navController.navigate(Routes.ADMIN_GROUPS) },
                onMas = { navController.navigate(Routes.ADMIN_MORE) },
            )
        }
        composable(Routes.ADMIN_JUSTIFICATIONS) {
            val justificationsViewModel: AdminJustificationsViewModel =
                viewModel(factory = rememberAppContainer().viewModelFactory)
            val justificationsUi by justificationsViewModel.state.collectAsState()

            AdminJustificationsScreen(
                ui = justificationsUi,
                onBack = { navController.popBackStack() },
                onRefresh = justificationsViewModel::refresh,
                onNoticeShown = justificationsViewModel::noticeShown,
                onErrorShown = justificationsViewModel::errorShown,
            )
        }
        composable(Routes.ADMIN_INCIDENTS) {
            AdminIncidentsScreen(
                onBack = { navController.popBackStack() },
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
                onDeepLink = { route -> navController.navigate(route) { launchSingleTop = true } },
            )
        }
        composable(Routes.ADMIN_PROFILE) {
            AdminProfileScreen(
                onLogout = { sessionViewModel.signOut() },
                onOpenAppPermissions = { navController.navigate(Routes.PERMISSIONS) },
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
