package com.example.redxaiscanner.telemetry

/**
 * An abstraction for performance monitoring.
 * This allows you to measure and log the duration of critical operations.
 */
interface PerformanceProfiler {
    fun start(operationName: String): Long
    fun end(operationName: String, startTime: Long, wasSuccessful: Boolean)
}

/**
 * A placeholder implementation. In a real app, this would send metrics
 * to a service like Firebase Performance Monitoring or Datadog.
 */
class PerformanceProfilerPlaceholder : PerformanceProfiler {
    override fun start(operationName: String): Long {
        println("PERF_MONITOR: Starting '$operationName'")
        return System.currentTimeMillis()
    }

    override fun end(operationName: String, startTime: Long, wasSuccessful: Boolean) {
        val duration = System.currentTimeMillis() - startTime
        println("PERF_MONITOR: Finished '$operationName' in ${duration}ms | Success: $wasSuccessful")
    }
}