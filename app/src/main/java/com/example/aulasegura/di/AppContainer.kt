package com.example.aulasegura.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aulasegura.AulaSeguraApp
import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.DatabaseProvider
import com.example.aulasegura.data.repository.AuthRepositoryImpl
import com.example.aulasegura.domain.repository.AuthRepository
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    // ---- Repositories -------------------------------------------------------
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(auth = firebaseAuth, firestore = firestore)
    }

    val outboxRepository: OutboxRepository by lazy {
        OutboxRepository(database = database, scheduling = syncScheduler)
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
                ),
            ),
        )
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

        else -> error("No ViewModel registered for ${modelClass.name}")
    } as T
}

/** The container of the running application, for Compose call sites. */
@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as AulaSeguraApp).container
