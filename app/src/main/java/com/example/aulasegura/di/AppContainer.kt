package com.example.aulasegura.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.BuildConfig
import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.DatabaseProvider
import com.example.aulasegura.data.local.media.FileIncidentMediaStore
import com.example.aulasegura.data.local.media.FileJustificationAttachmentStore
import com.example.aulasegura.data.local.media.FileTaskAttachmentStore
import com.example.aulasegura.data.local.media.IncidentMediaStore
import com.example.aulasegura.data.local.media.JustificationAttachmentStore
import com.example.aulasegura.data.local.media.TaskAttachmentStore
import com.example.aulasegura.data.remote.cloudinary.CloudinaryConfig
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import com.example.aulasegura.data.remote.ai.AiAssistantDataSource
import com.example.aulasegura.data.remote.cloudinary.OkHttpCloudinaryUploader
import com.example.aulasegura.data.remote.remoteconfig.FirebaseRemoteConfigSource
import com.example.aulasegura.data.remote.remoteconfig.RemoteConfigRepository
import com.example.aulasegura.data.repository.AuthRepositoryImpl
import com.example.aulasegura.data.repository.FirestoreAdminDashboardRepository
import com.example.aulasegura.data.repository.FirestoreAdminGroupRepository
import com.example.aulasegura.data.repository.FirestoreAdminIncidentRepository
import com.example.aulasegura.data.repository.FirestoreAdminReportRepository
import com.example.aulasegura.data.repository.FirestoreAdminSchoolRepository
import com.example.aulasegura.data.repository.FirestoreAdminStudentRepository
import com.example.aulasegura.data.repository.FirestoreAdminUserRepository
import com.example.aulasegura.data.repository.FirestoreIncidentHistoryRepository
import com.example.aulasegura.domain.repository.IncidentHistoryRepository
import com.example.aulasegura.data.repository.FirestoreAttendanceHistoryRepository
import com.example.aulasegura.data.repository.FirestoreRosterRepository
import com.example.aulasegura.data.repository.FirestoreScheduleRepository
import com.example.aulasegura.data.repository.FirestoreSchoolRepository
import com.example.aulasegura.data.repository.RoomSyncQueueRepository
import com.example.aulasegura.data.repository.RoomAttendanceRepository
import com.example.aulasegura.data.repository.RoomIncidentRepository
import com.example.aulasegura.data.repository.FirestoreChatRepository
import com.example.aulasegura.data.repository.FirestoreNotificationRepository
import com.example.aulasegura.data.repository.FirestoreParentRepository
import com.example.aulasegura.data.repository.FirestorePushTokenRepository
import com.example.aulasegura.data.repository.QueuedJustificationRepository
import com.example.aulasegura.data.repository.RoomNoticeRepository
import com.example.aulasegura.data.repository.RoomTaskRepository
import com.example.aulasegura.data.telemetry.FirebaseAnalyticsLogger
import com.example.aulasegura.domain.repository.AiAssistant
import com.example.aulasegura.domain.repository.AdminDashboardRepository
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminIncidentRepository
import com.example.aulasegura.domain.repository.AdminReportRepository
import com.example.aulasegura.domain.repository.AdminSchoolRepository
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ChatRepository
import com.example.aulasegura.domain.repository.NotificationRepository
import com.example.aulasegura.domain.repository.JustificationRepository
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.PushTokenRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import com.example.aulasegura.domain.repository.ScheduleRepository
import com.example.aulasegura.domain.repository.SchoolRepository
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.domain.repository.SyncQueueRepository
import com.example.aulasegura.domain.repository.TaskRepository
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.usecase.AskAssistantUseCase
import com.example.aulasegura.domain.usecase.CreateIncidentUseCase
import com.example.aulasegura.domain.usecase.SaveRollCallUseCase
import com.example.aulasegura.domain.usecase.StartRollCallUseCase
import com.example.aulasegura.sync.AttendanceSyncHandler
import com.example.aulasegura.sync.IncidentSyncHandler
import com.example.aulasegura.sync.JustificationSyncHandler
import com.example.aulasegura.sync.NoticeSyncHandler
import com.example.aulasegura.sync.TaskSyncHandler
import com.example.aulasegura.sync.CrashlyticsSyncReporter
import com.example.aulasegura.sync.OutboxRepository
import com.example.aulasegura.sync.SyncFailureReporter
import com.example.aulasegura.sync.SyncHandlerRegistry
import com.example.aulasegura.sync.SyncOperation
import com.example.aulasegura.sync.SyncScheduler
import com.example.aulasegura.sync.UploadIncidentMediaHandler
import com.example.aulasegura.sync.UploadTaskAttachmentHandler
import com.example.aulasegura.ui.screens.access.ForgotPasswordViewModel
import com.example.aulasegura.ui.screens.access.LoginViewModel
import com.example.aulasegura.ui.screens.access.RegisterViewModel
import com.example.aulasegura.ui.screens.access.SessionViewModel
import com.example.aulasegura.ui.screens.admin.AdminCampusViewModel
import com.example.aulasegura.ui.screens.admin.AdminDashboardViewModel
import com.example.aulasegura.ui.screens.admin.AdminGroupDetailViewModel
import com.example.aulasegura.ui.screens.admin.AdminGroupsViewModel
import com.example.aulasegura.ui.screens.admin.AdminIncidentsViewModel
import com.example.aulasegura.ui.screens.admin.AdminNewStudentViewModel
import com.example.aulasegura.ui.screens.admin.AdminNotificationsViewModel
import com.example.aulasegura.ui.screens.admin.AdminProfileViewModel
import com.example.aulasegura.ui.screens.admin.AdminReportsViewModel
import com.example.aulasegura.ui.screens.admin.AdminUsersViewModel
import com.example.aulasegura.ui.screens.parent.ParentAttendanceViewModel
import com.example.aulasegura.ui.screens.parent.ParentHomeViewModel
import com.example.aulasegura.ui.screens.parent.ParentIncidentDetailViewModel
import com.example.aulasegura.ui.screens.parent.ParentIncidentsViewModel
import com.example.aulasegura.ui.screens.parent.ParentJustifyViewModel
import com.example.aulasegura.ui.screens.parent.ParentNoticesViewModel
import com.example.aulasegura.ui.screens.parent.ParentTaskDetailViewModel
import com.example.aulasegura.ui.screens.parent.ParentTasksViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherGroupDetailViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHistoryViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherHomeViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherIncidentViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherChatViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherConversationViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherNoticeNewViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherNotificationsViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherProfileViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherTaskNewViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * Composition root, built by hand.
 *
 * Chosen over Hilt on purpose: the graph is small, and one file the team can read
 * from top to bottom beats a code generator plus annotation magic. When a second
 * module or a second developer needs independent wiring, this class is the one
 * place to split.
 *
 * Rules this container keeps:
 * - One instance per dependency. Two `AuthRepository` instances would mean two
 *   in-memory session states, and two databases would corrupt the outbox.
 * - Firebase is created here and nowhere else; domain and UI code only see the
 *   contracts in `domain/repository`.
 * - Everything is lazy, so building the container in `Application.onCreate` does
 *   not touch the disk, the network or the Firebase SDK.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // ---- Firebase -----------------------------------------------------------
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Realtime Database: chat messages and presence. Firestore only keeps summaries. */
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // ---- Storage ------------------------------------------------------------
    val database: AulaSeguraDatabase by lazy { DatabaseProvider.get(appContext) }

    // ---- Telemetry ----------------------------------------------------------
    /**
     * One logger for the whole app.
     *
     * Built from the application context so it survives every Activity
     * recreation, and injected into the classes that emit business events. The
     * sink swallows its own failures: Analytics being down must never cost a
     * sign-in or a roll call.
     */
    val analyticsLogger: AnalyticsLogger by lazy {
        FirebaseAnalyticsLogger(FirebaseAnalytics.getInstance(appContext))
    }

    // ---- Repositories -------------------------------------------------------
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            auth = firebaseAuth,
            firestore = firestore,
            profileDao = database.profileDao(),
            analytics = analyticsLogger,
        )
    }

    /**
     * The roster cache. One instance behind both contracts because the two share
     * the same Firestore, the same Room cache and the same session; two instances
     * would mean two copies of the query filters.
     */
    private val rosterRepository: FirestoreRosterRepository by lazy {
        FirestoreRosterRepository(
            firestore = firestore,
            groupDao = database.groupDao(),
            studentDao = database.studentDao(),
            auth = authRepository,
        )
    }

    val groupRepository: GroupRepository get() = rosterRepository
    val studentRepository: StudentRepository get() = rosterRepository

    /**
     * The school site, cached so the geofence works without a network.
     *
     * The fallback radius comes from Remote Config, read at mapping time and not
     * cached here: the console value can be refreshed while the app is running,
     * and a stale radius is still a valid radius only until the next evaluation.
     */
    /**
     * The timetable of the teacher's groups.
     *
     * Read live and not cached: the documents are the `directivo` role's to write
     * and there are none yet, so a Room table would be a migration for data that
     * may never arrive.
     */
    val scheduleRepository: ScheduleRepository by lazy {
        FirestoreScheduleRepository(
            firestore = firestore,
            auth = authRepository,
            groups = groupRepository,
        )
    }

    /** Outbox rows waiting for Firestore, for the home's "Por sincronizar". */
    val syncQueueRepository: SyncQueueRepository by lazy {
        RoomSyncQueueRepository(database.outboxDao())
    }

    val schoolRepository: SchoolRepository by lazy {
        FirestoreSchoolRepository(
            firestore = firestore,
            schoolDao = database.schoolDao(),
            auth = authRepository,
            defaultRadiusMeters = { remoteConfigRepository.defaultRadiusMeters().toDouble() },
        )
    }

    val outboxRepository: OutboxRepository by lazy {
        OutboxRepository(
            database = database,
            scheduling = syncScheduler,
            analytics = analyticsLogger,
        )
    }

    /**
     * The roll call write path. One instance: it owns nothing mutable, but the
     * outbox and the database it writes through must be the same ones the sync
     * worker reads.
     */
    val attendanceRepository: AttendanceRepository by lazy {
        RoomAttendanceRepository(
            attendanceDao = database.attendanceDao(),
            outbox = outboxRepository,
            groups = groupRepository,
            auth = authRepository,
        )
    }

    /**
     * The read side of attendance. Separate instance from [attendanceRepository]
     * because the two answer different questions: one writes the session the
     * teacher is taking, this one reads the sessions already closed.
     */
    val attendanceHistoryRepository: AttendanceHistoryRepository by lazy {
        FirestoreAttendanceHistoryRepository(
            firestore = firestore,
            attendanceDao = database.attendanceDao(),
            groupDao = database.groupDao(),
            studentDao = database.studentDao(),
            auth = authRepository,
        )
    }

    /**
     * The teacher's own incident history. Read-only and cache-first like the roll
     * call history, but under the `reportedBy == uid` clause the rules already
     * allow for a docente.
     */
    val incidentHistoryRepository: IncidentHistoryRepository by lazy {
        FirestoreIncidentHistoryRepository(
            firestore = firestore,
            incidentDao = database.incidentDao(),
            auth = authRepository,
        )
    }

    /**
     * The tutor's read path: the children linked to `students.tutorIds` and what
     * the school already published for them.
     *
     * Online-first on purpose. There is no Room mirror: a family opens the app a
     * few times a day, and a cached empty month would read as a lost month.
     */
    val parentRepository: ParentRepository by lazy {
        FirestoreParentRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The tutor's only write path: a receipt filed from the phone and queued for
     * the worker.
     *
     * Offline-first because the form promises it, and the document is written by
     * `JustificationSyncHandler` only after Cloudinary accepted the file: the
     * tutor's rule allows `create` and never `update`.
     */
    val justificationRepository: JustificationRepository by lazy {
        QueuedJustificationRepository(
            database = database,
            attachments = justificationAttachmentStore,
            auth = authRepository,
            scheduling = syncScheduler,
            analytics = analyticsLogger,
        )
    }

    /**
     * The incident write path, offline-first like the roll call: Room draft plus
     * outbox row, and `IncidentSyncHandler` is what reaches Firestore.
     */
    val incidentRepository: IncidentRepository by lazy {
        RoomIncidentRepository(
            incidentDao = database.incidentDao(),
            outbox = outboxRepository,
            groups = groupRepository,
            auth = authRepository,
        )
    }

    val createIncidentUseCase: CreateIncidentUseCase by lazy {
        CreateIncidentUseCase(incidents = incidentRepository, analytics = analyticsLogger)
    }

    /**
     * Publications, offline-first like the incidents: a cached row plus an outbox
     * row, and `TaskSyncHandler`/`NoticeSyncHandler` are what reach Firestore.
     */
    val taskRepository: TaskRepository by lazy {
        RoomTaskRepository(
            taskDao = database.taskDao(),
            outboxDao = database.outboxDao(),
            outbox = outboxRepository,
            groups = groupRepository,
            auth = authRepository,
            analytics = analyticsLogger,
        )
    }

    /**
     * Chat, online-only on purpose: RTDB already caches its own messages, and a
     * Room copy would be a second truth for the same bubble.
     */
    val chatRepository: ChatRepository by lazy {
        FirestoreChatRepository(
            firestore = firestore,
            database = firebaseDatabase,
            groups = groupRepository,
            auth = authRepository,
            analytics = analyticsLogger,
        )
    }

    /**
     * This device's FCM token, under `users/{uid}/fcmTokens`.
     *
     * In Spark nothing sends automatically, but the token is what the console's
     * *Send test message* needs and what the tray's notifications point to.
     */
    val pushTokenRepository: PushTokenRepository by lazy {
        FirestorePushTokenRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /** The teacher's notification tray: read-only except for the `read` flag. */
    val notificationRepository: NotificationRepository by lazy {
        FirestoreNotificationRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The principal's campus numbers. Aggregate queries over four collections,
     * scoped to the school of the open session; a single shared instance keeps the
     * dashboard and its refresh on the same Firestore and session.
     */
    val adminDashboardRepository: AdminDashboardRepository by lazy {
        FirestoreAdminDashboardRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The principal's attendance reports. One-shot range queries over
     * `attendanceSessions`, plus the single-day summary the dashboard card shows;
     * a shared instance keeps both consumers on the same session and Firestore.
     */
    val adminReportRepository: AdminReportRepository by lazy {
        FirestoreAdminReportRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The principal's incident inbox. One-shot school-filtered reads plus the
     * status-only edit; a shared instance keeps the inbox list and its refresh on
     * the same Firestore and session. Incidents are evidence and are never deleted.
     */
    val adminIncidentRepository: AdminIncidentRepository by lazy {
        FirestoreAdminIncidentRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /** The principal's one-shot user list and whitelisted profile edit. */
    val adminUserRepository: AdminUserRepository by lazy {
        FirestoreAdminUserRepository(
            firestore = firestore,
            auth = authRepository,
            context = appContext,
        )
    }

    /**
     * The principal's groups. The only path that assigns teachers, and therefore
     * the only writer of the two-side `teacherIds`/`groupIds` link.
     */
    val adminGroupRepository: AdminGroupRepository by lazy {
        FirestoreAdminGroupRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The principal's students. The only path that creates a student and keeps
     * `groups/{id}.studentCount` in sync with the teacher's roll-call query.
     */
    val adminStudentRepository: AdminStudentRepository by lazy {
        FirestoreAdminStudentRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    /**
     * The principal's campus form. Online-only and cache-free, unlike the
     * teacher's geofence read: a save must be confirmed by the server, while the
     * teacher keeps using the last cached centre until their next online refresh.
     */
    val adminSchoolRepository: AdminSchoolRepository by lazy {
        FirestoreAdminSchoolRepository(
            firestore = firestore,
            auth = authRepository,
        )
    }

    val noticeRepository: NoticeRepository by lazy {
        RoomNoticeRepository(
            noticeDao = database.noticeDao(),
            outboxDao = database.outboxDao(),
            outbox = outboxRepository,
            groups = groupRepository,
            auth = authRepository,
            analytics = analyticsLogger,
        )
    }

    val startRollCallUseCase: StartRollCallUseCase by lazy {
        StartRollCallUseCase(attendance = attendanceRepository, analytics = analyticsLogger)
    }

    val saveRollCallUseCase: SaveRollCallUseCase by lazy {
        SaveRollCallUseCase(attendance = attendanceRepository)
    }

    // ---- Sync ---------------------------------------------------------------
    val syncScheduler: SyncScheduler by lazy { SyncScheduler(appContext) }

    val syncFailureReporter: SyncFailureReporter = CrashlyticsSyncReporter

    val syncHandlers: SyncHandlerRegistry by lazy {
        SyncHandlerRegistry(
            mapOf(
                SyncOperation.UPSERT_ATTENDANCE_SESSION to AttendanceSyncHandler(
                    attendanceDao = database.attendanceDao(),
                    firestore = firestore,
                    analytics = analyticsLogger,
                ),
                SyncOperation.UPSERT_INCIDENT to IncidentSyncHandler(
                    incidentDao = database.incidentDao(),
                    firestore = firestore,
                ),
                SyncOperation.UPLOAD_INCIDENT_MEDIA to UploadIncidentMediaHandler(
                    incidentDao = database.incidentDao(),
                    uploader = cloudinaryUploader,
                ),
                SyncOperation.UPLOAD_TASK_ATTACHMENT to UploadTaskAttachmentHandler(
                    taskDao = database.taskDao(),
                    uploader = cloudinaryUploader,
                ),
                SyncOperation.UPSERT_TASK to TaskSyncHandler(
                    taskDao = database.taskDao(),
                    firestore = firestore,
                ),
                SyncOperation.UPSERT_NOTICE to NoticeSyncHandler(
                    noticeDao = database.noticeDao(),
                    firestore = firestore,
                ),
                SyncOperation.UPSERT_JUSTIFICATION to JustificationSyncHandler(
                    firestore = firestore,
                    uploader = cloudinaryUploader,
                    outboxDao = database.outboxDao(),
                ),
            ),
        )
    }

    // ---- Network ------------------------------------------------------------
    /**
     * The one HTTP client of the app.
     *
     * The write timeout is generous on purpose: a photo over school Wi-Fi can
     * take a while, and the 10 s default would kill a perfectly valid upload.
     */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ---- Cloudinary ---------------------------------------------------------
    val cloudinaryConfig: CloudinaryConfig by lazy {
        CloudinaryConfig(
            cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME,
            uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET,
        )
    }

    val cloudinaryUploader: CloudinaryUploader by lazy {
        OkHttpCloudinaryUploader(client = httpClient, config = cloudinaryConfig)
    }

    val imageCompressor: ImageCompressor by lazy { ImageCompressor(appContext) }

    /**
     * Where a captured photo lives between the form and the upload.
     *
     * Exposed from the container so the form and the worker agree on the same
     * directory: a file the screen wrote and the worker cannot find would be an
     * evidence photo the incident never carries.
     */
    val incidentMediaStore: IncidentMediaStore by lazy {
        FileIncidentMediaStore(appContext, imageCompressor)
    }

    /**
     * Where the task's PDF lives between the form and the upload.
     *
     * Same reasoning as [incidentMediaStore]: one directory the form writes and
     * the worker reads, so a file cannot be "attached" and then not found.
     */
    val taskAttachmentStore: TaskAttachmentStore by lazy {
        FileTaskAttachmentStore(appContext)
    }

    /**
     * Where the justification's receipt lives between the form and the upload.
     *
     * `filesDir` and not the cache, for the same reason as the task PDF: a
     * justification filed on Sunday can reach Cloudinary on Tuesday, and the
     * system is free to delete cached files long before that.
     */
    val justificationAttachmentStore: JustificationAttachmentStore by lazy {
        FileJustificationAttachmentStore(appContext, imageCompressor)
    }

    // ---- Remote Config ------------------------------------------------------
    /**
     * Behaviour the school can change without shipping a build.
     *
     * Exposed from the container even though Fase 0 has no consumer yet: the
     * defaults and the validated reads are infrastructure the later phases depend
     * on, and wiring it here is what keeps `FirebaseRemoteConfig` out of them.
     */
    val remoteConfigRepository: RemoteConfigRepository by lazy {
        RemoteConfigRepository(FirebaseRemoteConfigSource())
    }

    // ---- AI -----------------------------------------------------------------
    /**
     * Firebase AI Logic behind a domain contract.
     *
     * Built lazily like everything else: the SDK instance is only created when
     * the first question is asked, and the Remote Config gate is read from the
     * same repository the rest of the app uses — one source of truth for
     * `ai_assistant_enabled`.
     */
    val aiAssistant: AiAssistant by lazy {
        AiAssistantDataSource(remoteConfig = remoteConfigRepository)
    }

    val askAssistantUseCase: AskAssistantUseCase by lazy {
        AskAssistantUseCase(assistant = aiAssistant, analytics = analyticsLogger)
    }

    // ---- ViewModels ---------------------------------------------------------
    val viewModelFactory: AppViewModelFactory by lazy { AppViewModelFactory(this) }
}

/**
 * Builds every ViewModel from the container.
 *
 * One factory instead of a factory per screen: a missing `when` branch fails
 * loudly here at runtime, which is the manual-DI equivalent of a missing Hilt
 * binding.
 */
class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    /**
     * The new-student form sometimes arrives with a preselected group (from a group
     * card or the group detail screen). Building it here keeps the repositories in
     * the composition root instead of leaking them into the screen.
     */
    fun adminNewStudentViewModel(presetGroupId: String): AdminNewStudentViewModel =
        AdminNewStudentViewModel(
            students = container.adminStudentRepository,
            groups = container.adminGroupRepository,
            presetGroupId = presetGroupId,
        )

    /**
     * The tutor's justification form. Both arguments come from the route and are
     * optional: without them the form points at the newest absence the school
     * registered, never at an invented date.
     */
    fun parentJustifyViewModel(studentId: String?, date: String?): ParentJustifyViewModel =
        ParentJustifyViewModel(
            parents = container.parentRepository,
            justifications = container.justificationRepository,
            studentId = studentId,
            absenceIso = date,
        )

    /** The task detail is read by document id, which arrives in the route. */
    fun parentTaskDetailViewModel(taskId: String): ParentTaskDetailViewModel =
        ParentTaskDetailViewModel(
            parents = container.parentRepository,
            taskId = taskId,
        )

    /**
     * The incident detail and its map share this factory: both routes carry the
     * incident id and read the same document.
     */
    fun parentIncidentDetailViewModel(incidentId: String): ParentIncidentDetailViewModel =
        ParentIncidentDetailViewModel(
            parents = container.parentRepository,
            incidentId = incidentId,
        )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(container.authRepository)

        modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
            RegisterViewModel(container.authRepository)

        modelClass.isAssignableFrom(SessionViewModel::class.java) ->
            SessionViewModel(
                authRepository = container.authRepository,
                pushTokens = container.pushTokenRepository,
            )

        modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
            ForgotPasswordViewModel(container.authRepository)

        modelClass.isAssignableFrom(TeacherGroupsViewModel::class.java) ->
            TeacherGroupsViewModel(
                groups = container.groupRepository,
                auth = container.authRepository,
            )

        modelClass.isAssignableFrom(TeacherGroupDetailViewModel::class.java) ->
            TeacherGroupDetailViewModel(
                groups = container.groupRepository,
                students = container.studentRepository,
                history = container.attendanceHistoryRepository,
                incidents = container.incidentHistoryRepository,
                schedule = container.scheduleRepository,
                auth = container.authRepository,
            )

        modelClass.isAssignableFrom(TeacherHomeViewModel::class.java) ->
            TeacherHomeViewModel(
                groups = container.groupRepository,
                schedule = container.scheduleRepository,
                history = container.attendanceHistoryRepository,
                incidents = container.incidentHistoryRepository,
                tasks = container.taskRepository,
                notices = container.noticeRepository,
                notifications = container.notificationRepository,
                pending = container.syncQueueRepository,
            )

        modelClass.isAssignableFrom(TeacherHistoryViewModel::class.java) ->
            TeacherHistoryViewModel(
                history = container.attendanceHistoryRepository,
                incidents = container.incidentHistoryRepository,
                tasks = container.taskRepository,
                notices = container.noticeRepository,
            )

        modelClass.isAssignableFrom(TeacherIncidentViewModel::class.java) ->
            TeacherIncidentViewModel(
                groups = container.groupRepository,
                students = container.studentRepository,
                createIncident = container.createIncidentUseCase,
                incidents = container.incidentRepository,
                mediaStore = container.incidentMediaStore,
                readMediaLimit = { container.remoteConfigRepository.incidentMediaMax() },
            )

        modelClass.isAssignableFrom(TeacherTaskNewViewModel::class.java) ->
            TeacherTaskNewViewModel(
                groups = container.groupRepository,
                auth = container.authRepository,
                tasks = container.taskRepository,
                attachmentStore = container.taskAttachmentStore,
            )

        modelClass.isAssignableFrom(TeacherNotificationsViewModel::class.java) ->
            TeacherNotificationsViewModel(
                notifications = container.notificationRepository,
            )

        modelClass.isAssignableFrom(TeacherChatViewModel::class.java) ->
            TeacherChatViewModel(
                chat = container.chatRepository,
            )

        modelClass.isAssignableFrom(TeacherConversationViewModel::class.java) ->
            TeacherConversationViewModel(
                chat = container.chatRepository,
                auth = container.authRepository,
            )

        modelClass.isAssignableFrom(TeacherNoticeNewViewModel::class.java) ->
            TeacherNoticeNewViewModel(
                groups = container.groupRepository,
                auth = container.authRepository,
                notices = container.noticeRepository,
            )

        modelClass.isAssignableFrom(ParentHomeViewModel::class.java) ->
            ParentHomeViewModel(parents = container.parentRepository)

        modelClass.isAssignableFrom(ParentAttendanceViewModel::class.java) ->
            ParentAttendanceViewModel(parents = container.parentRepository)

        modelClass.isAssignableFrom(ParentTasksViewModel::class.java) ->
            ParentTasksViewModel(parents = container.parentRepository)

        modelClass.isAssignableFrom(ParentNoticesViewModel::class.java) ->
            ParentNoticesViewModel(parents = container.parentRepository)

        modelClass.isAssignableFrom(ParentIncidentsViewModel::class.java) ->
            ParentIncidentsViewModel(parents = container.parentRepository)

        modelClass.isAssignableFrom(AdminDashboardViewModel::class.java) ->
            AdminDashboardViewModel(
                dashboard = container.adminDashboardRepository,
                reports = container.adminReportRepository,
            )

        modelClass.isAssignableFrom(AdminReportsViewModel::class.java) ->
            AdminReportsViewModel(reports = container.adminReportRepository)

        modelClass.isAssignableFrom(AdminIncidentsViewModel::class.java) ->
            AdminIncidentsViewModel(incidents = container.adminIncidentRepository)

        modelClass.isAssignableFrom(AdminUsersViewModel::class.java) ->
            AdminUsersViewModel(
                users = container.adminUserRepository,
                students = container.adminStudentRepository,
                groups = container.adminGroupRepository,
            )

        modelClass.isAssignableFrom(AdminNewStudentViewModel::class.java) ->
            AdminNewStudentViewModel(
                students = container.adminStudentRepository,
                groups = container.adminGroupRepository,
            )

        modelClass.isAssignableFrom(AdminGroupsViewModel::class.java) ->
            AdminGroupsViewModel(groups = container.adminGroupRepository)

        modelClass.isAssignableFrom(AdminGroupDetailViewModel::class.java) ->
            AdminGroupDetailViewModel(
                groups = container.adminGroupRepository,
                users = container.adminUserRepository,
                students = container.adminStudentRepository,
            )

        modelClass.isAssignableFrom(AdminCampusViewModel::class.java) ->
            AdminCampusViewModel(schools = container.adminSchoolRepository)

        modelClass.isAssignableFrom(AdminNotificationsViewModel::class.java) ->
            AdminNotificationsViewModel(notifications = container.notificationRepository)

        modelClass.isAssignableFrom(AdminProfileViewModel::class.java) ->
            AdminProfileViewModel(
                users = container.adminUserRepository,
                auth = container.authRepository,
                compressor = container.imageCompressor,
                uploader = container.cloudinaryUploader,
            )

        modelClass.isAssignableFrom(TeacherProfileViewModel::class.java) ->
            TeacherProfileViewModel(
                auth = container.authRepository,
                groups = container.groupRepository,
                compressor = container.imageCompressor,
                uploader = container.cloudinaryUploader,
            )

        else -> error("No ViewModel registered for ${modelClass.name}")
    } as T
}

/** The container of the running application, for Compose call sites. */
@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as AulaSeguraApp).container
