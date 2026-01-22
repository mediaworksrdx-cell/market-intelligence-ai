package com.example.redxaiscanner.config

/**
 * A data class representing all configurable aspects of the application.
 * This provides a single source of truth for environment settings and feature flags.
 */
data class AppConfig(
    val environment: String,
    val apiBaseUrl: String,
    val isAiValidationEnabled: Boolean,
    val signalScoreThreshold: Int,
    val isVolumeSpikeEngineEnabled: Boolean,
    val supportedSymbols: List<String>,
    // --- Governance and Risk Controls ---
    val maxStopLossPercentage: Double
)