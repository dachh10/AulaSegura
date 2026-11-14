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
import com.example.aulasegura.data.remote.cloudinary.CloudinaryConfig
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import com.example.aulasegura.data.remote.ai.AiAssistantDataSource
import com.example.aulasegura.data.remote.cloudinary.OkHttpCloudinaryUploader
import com.example.aulasegura.data.remote.remoteconfig.FirebaseRemoteConfigSource
import com.example.aulasegura.data.remote.remoteconfig.RemoteConfigRepository
import com.example.aulasegura.data.repository.AuthRepositoryImpl
import com.example.aulasegura.data.repository.FirestoreRosterRepository
import com.example.aulasegura.data.telemetry.FirebaseAnalyticsLogger
import com.example.aulasegura.domain.repository.AiAssistant
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.usecase.AskAssistantUseCase
import com.example.aulasegura.sync.AttendanceSyncHandler
import com.example.aulasegura.sync.CrashlyticsSyncReporter
import com.example.aulasegura.sync.OutboxRepository
import com.example.aulasegura.sync.SyncFailureReporter
import com.example.aulasegura.sync.SyncHandlerRegistry
import com.example.aulasegura.sync.SyncOperation
import com.example.aulasegura.sync.SyncScheduler
import com.example.aulasegura.ui.screens.access.ForgotPasswordViewModel
import com.example.aulasegura.ui.screens.access.LoginViewModel
import com.example.aulasegura.ui.screens.access.SessionViewModel
import com.example.aulasegura.ui.screens.teacher.TeacherGroupsViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
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

    val outboxRepository: OutboxRepository by lazy {
        OutboxRepository(
            database = database,
            scheduling = syncScheduler,
            analytics = analyticsLogger,
        )
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
    val viewModelFactory: ViewModelProvider.Factory by lazy { AppViewModelFactory(this) }
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(container.authRepository)

        modelClass.isAssignableFrom(SessionViewModel::class.java) ->
            SessionViewModel(container.authRepository)

        modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
            ForgotPasswordViewModel(container.authRepository)

        modelClass.isAssignableFrom(TeacherGroupsViewModel::class.java) ->
            TeacherGroupsViewModel(
                groups = container.groupRepository,
                auth = container.authRepository,
            )

        else -> error("No ViewModel registered for ${modelClass.name}")
    } as T
}

/** The container of the running application, for Compose call sites. */
@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as AulaSeguraApp).container
