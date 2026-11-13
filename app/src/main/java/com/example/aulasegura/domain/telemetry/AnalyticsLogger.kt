package com.example.aulasegura.domain.telemetry

/**
 * Sink for [AnalyticsEvent]s.
 *
 * An interface rather than a direct Firebase call so a ViewModel, a repository
 * or a test can assert an event was logged without standing up Analytics.
 */
fun interface AnalyticsLogger {

    fun log(event: AnalyticsEvent)
}

/**
 * Logger for tests and for any code path where telemetry must stay out of the
 * way. Business code defaults to this so a missing injection degrades to silence
 * instead of a crash.
 */
object NoOpAnalyticsLogger : AnalyticsLogger {

    override fun log(event: AnalyticsEvent) = Unit
}
