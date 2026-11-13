package com.example.aulasegura.data.telemetry

import android.os.Bundle
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Sends [AnalyticsEvent]s to Firebase Analytics.
 *
 * Wrapped in `runCatching` on purpose: telemetry is the least important thing the
 * app does, and a misconfigured Analytics session must never break a roll call or
 * a sign-in.
 */
class FirebaseAnalyticsLogger(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsLogger {

    override fun log(event: AnalyticsEvent) {
        runCatching {
            firebaseAnalytics.logEvent(event.name, event.params.toEventParamsBundle())
        }
    }
}

/**
 * Translates the event parameters into the types Analytics understands.
 *
 * Booleans become `1L`/`0L`: Firebase has no boolean parameter type, and passing
 * one produces a warning and a dropped value. Visible to the instrumentation
 * test because that drop is silent — the app keeps working while the dashboard
 * quietly loses a field.
 */
internal fun Map<String, Any>.toEventParamsBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            is String -> putString(key, value)
            is Int -> putLong(key, value.toLong())
            is Long -> putLong(key, value)
            is Double -> putDouble(key, value)
            is Boolean -> putLong(key, if (value) 1L else 0L)
            else -> putString(key, value.toString())
        }
    }
}
