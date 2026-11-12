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

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheckProvider()
        configureFirestore()

        container = AppContainer(this)
        container.syncScheduler.schedule()
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
