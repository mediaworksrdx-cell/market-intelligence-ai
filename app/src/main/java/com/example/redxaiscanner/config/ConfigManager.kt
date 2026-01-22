package com.example.redxaiscanner.config

import com.example.marketintelligence.BuildConfig

object ConfigManager {

    private val productionConfig = AppConfig(
        environment = "production",
        apiBaseUrl = "https://api.production.com/data",
        isAiValidationEnabled = true,
        signalScoreThreshold = 70,
        isVolumeSpikeEngineEnabled = true,
        supportedSymbols = listOf("NIFTY", "BANKNIFTY", "RELIANCE"),
        maxStopLossPercentage = 5.0
    )

    private val debugConfig = AppConfig(
        environment = "debug",
        apiBaseUrl = "https://api.staging.com/data",
        isAiValidationEnabled = false,
        signalScoreThreshold = 50,
        isVolumeSpikeEngineEnabled = false,
        supportedSymbols = listOf("NIFTY"),
        maxStopLossPercentage = 10.0
    )

    fun getConfig(): AppConfig {
        return if (BuildConfig.DEBUG) {
            debugConfig
        } else {
            productionConfig
        }
    }
}
