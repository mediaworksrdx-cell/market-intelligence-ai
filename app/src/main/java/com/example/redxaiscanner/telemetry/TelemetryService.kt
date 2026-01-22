package com.example.redxaiscanner.telemetry

/**
 * An abstraction for telemetry and crash reporting services.
 * This allows the underlying implementation (e.g., Firebase, Sentry) to be swapped out easily.
 */
interface TelemetryService {
    /**
     * Logs a custom event.
     * @param name The name of the event (e.g., "scan_started").
     * @param params A map of associated parameters.
     */
    fun logEvent(name: String, params: Map<String, String>)

    /**
     * Records a non-fatal exception.
     * @param throwable The exception to record.
     */
    fun recordException(throwable: Throwable)
}

/**
 * A placeholder implementation. In a real app, this would be backed by Firebase Analytics, Sentry, etc.
 */
class AnalyticsPlaceholder : TelemetryService {
    override fun logEvent(name: String, params: Map<String, String>) {
        println("TELEMETRY EVENT: $name | PARAMS: $params")
    }

    override fun recordException(throwable: Throwable) {
         println("TELEMETRY ERROR: ${throwable.message}")
    }
}