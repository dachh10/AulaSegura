package com.example.aulasegura

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.aulasegura.di.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point and owner of the [AppContainer].
 *
 * Responsibilities at startup:
 * - Initialize Firebase.
 * - Install the App Check provider: debug provider on debuggable builds,
 *   Play Integrity on release builds.
 * - Disable Firestore's persistent cache. Room is the single local source of
 *   truth; two caches competing for the same data is a bug factory.
 * - Build the composition root, then ask for one outbox drain.
 *   `enqueueUniqueWork(KEEP)` makes that idempotent: starting the app ten times
 *   still leaves one pending worker, and WorkManager holds it until the device
 *   is online.
 * - Refresh Remote Config in the background. Nothing waits for it: the defaults
 *   installed with the source are the contract for the first launch, and a slow
 *   network must never delay the first frame.
 */
class AulaSeguraApp : Application() {

    /**
     * The app's composition root.
     *
     * `lateinit` because it needs a `Context`; created in [onCreate] before any
     * activity, service or worker can ask for it.
     */
    lateinit var container: AppContainer
        private set

    /**
     * Scope for work that belongs to the process, not to a screen.
     *
     * `SupervisorJob` so one failed refresh never cancels the rest, and `IO`
     * because nothing here touches the main thread.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheckProvider()
        configureFirestore()

        container = AppContainer(this)
        container.syncScheduler.schedule()
        refreshRemoteConfig()
    }

    /**
     * Pulls Remote Config once per app start.
     *
     * Without a caller the console values would never reach the device, and the
     * keys would only exist in tests. Fire and forget on purpose: the repository
     * falls back to the documented defaults, so a failure here costs nothing.
     */
    private fun refreshRemoteConfig() {
        appScope.launch { container.remoteConfigRepository.refresh() }
    }

    private fun installAppCheckProvider() {
        val factory = if (isDebuggable()) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }

    private fun configureFirestore() {
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
