package com.example.redxaiscanner.audit

import com.example.redxaiscanner.engine.ScoredSignal

/**
 * Provides a structured logging mechanism for audit trails and compliance.
 * This creates a verifiable record of the engine's decision-making process.
 */
interface AuditLogger {
    /**
     * Logs the result of a signal evaluation.
     * @param signal The signal that was scored.
     * @param passedThreshold Whether the signal met the configured score threshold.
     */
    fun logSignalEvaluation(signal: ScoredSignal, passedThreshold: Boolean)
}

/**
 * A placeholder implementation. In a real app, this would write to a secure,
 * remote logging service (e.g., Datadog, Logz.io) or an on-disk log file.
 */
class AuditLoggerPlaceholder : AuditLogger {
    override fun logSignalEvaluation(signal: ScoredSignal, passedThreshold: Boolean) {
        val setup = signal.underlyingSignal
        val outcome = if (passedThreshold) "PASSED" else "FILTERED"
        
        println("AUDIT LOG | Outcome: $outcome | Symbol: ${setup.symbol} | Bias: ${setup.higherTimeframeBias} | Score: ${signal.confidenceScore} | EngineVersions: ${setup.versions}")
    }
}