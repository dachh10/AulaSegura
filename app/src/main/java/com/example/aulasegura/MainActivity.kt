package com.example.aulasegura

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.aulasegura.navigation.AulaSeguraNavHost
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaSeguraTheme
import com.example.aulasegura.ui.theme.PrimaryContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crashForCrashlyticsVerificationIfRequested(intent)
        enableEdgeToEdge()
        setContent {
            AulaSeguraTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AulaSeguraNavHost()
                    // App-wide shield watermark, so every screen of every role carries
                    // the same premium background texture.
                    ShieldPattern(
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.035f,
                        color = PrimaryContainer,
                    )
                }
            }
        }
    }

    /**
     * Throws on demand so Crashlytics can be verified against the real dashboard.
     *
     * Only reachable in a debug build and only with an explicit Intent extra, so
     * there is no button, no menu entry and no accidental path to it in release:
     *
     * ```
     * adb shell am start -n com.example.aulasegura/.MainActivity \
     *   --ez crashlytics_test_crash true
     * ```
     *
     * A crash that fires in `onCreate` is the right shape: it happens before the
     * first frame, so a report appears even when the failure is in startup wiring.
     */
    private fun crashForCrashlyticsVerificationIfRequested(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        if (intent?.getBooleanExtra(EXTRA_TEST_CRASH, false) != true) return
        throw IllegalStateException("Crashlytics test crash requested through Intent extra")
    }

    private companion object {
        const val EXTRA_TEST_CRASH = "crashlytics_test_crash"
    }
}
