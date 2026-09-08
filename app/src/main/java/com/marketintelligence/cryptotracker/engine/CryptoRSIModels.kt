package com.marketintelligence.cryptotracker.engine

/**
 * RSI oscillator zones.
 */
enum class RSIZone {
    OVERSOLD,          // < 30
    WEAK_BEARISH,      // 30–50
    WEAK_BULLISH,      // 50–70
    OVERBOUGHT         // > 70
}

/**
 * Directional momentum of the RSI indicator.
 */
enum class MomentumDirection { BULLISH, BEARISH, NEUTRAL }

/**
 * A historical or specific timeframe snapshot of the RSI.
 */
data class RSISnapshot(
    val value: Double,
    val zone: RSIZone,
    val timestamp: Long,
    val timeframe: String
)

/**
 * Divergence types between price and RSI.
 */
enum class DivergenceType { BULLISH, BEARISH }

/**
 * Represents a detected divergence between price swings and RSI swings.
 */
data class RSIDivergence(
    val type: DivergenceType,
    val priceSwingPrice: Double,
    val priceSwingTimestamp: Long,
    val rsiSwingValue: Double,
    val rsiSwingTimestamp: Long
)

/**
 * Details on how RSI confirms the current bias.
 */
data class RSIConfirmation(
    val isConfirming: Boolean,
    val zone: RSIZone,
    val momentumDirection: MomentumDirection,
    val value: Double
)

/**
 * The consolidated Relative Strength Index (RSI) analysis.
 */
data class CryptoRSIAnalysis(
    val currentRSI: Double,
    val zone: RSIZone,
    val momentumDirection: MomentumDirection,
    val snapshots: Map<String, RSISnapshot>,  // timeframe -> snapshot
    val divergences: List<RSIDivergence>,
    val confirmation: RSIConfirmation
)
