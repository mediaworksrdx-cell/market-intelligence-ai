package com.marketintelligence.cryptotracker.engine

/**
 * Directional bias of the Fair Value Gap.
 */
enum class FVGDirection { BULLISH, BEARISH }

/**
 * The lifecycle state of a Fair Value Gap.
 */
enum class FVGStatus {
    NEW, ACTIVE, TOUCHED, PARTIALLY_FILLED, MITIGATED, INVALIDATED
}

/**
 * A comprehensively analyzed Fair Value Gap (FVG) or imbalance.
 */
data class EnrichedFVG(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val direction: FVGDirection,
    val createdTime: Long,
    val top: Double,
    val bottom: Double,
    val sizePercent: Double,
    val age: Int,                    // candles since creation
    val filledPercent: Double,       // 0.0–100.0
    val displacement: Boolean,       // created by displacement candle?
    val associatedBOS: Boolean,      // BOS within N candles?
    val associatedSweep: Boolean,    // liquidity sweep before it?
    val htfAlignment: Boolean,       // aligns with HTF bias?
    val volumeRatio: Double,         // C2 volume vs 20-period MA
    val status: FVGStatus,
    val qualityScore: FVGQualityScore
)

/**
 * Breakdown of the quality score for a given FVG, useful for filtering strong setups.
 */
data class FVGQualityScore(
    val htfAlignment: Int,       // 0–20
    val liquiditySweep: Int,     // 0–20
    val bosMss: Int,             // 0–20
    val displacement: Int,       // 0–15
    val freshness: Int,          // 0–10
    val size: Int,               // 0–5
    val volume: Int,             // 0–5
    val rsiConfirmation: Int,    // 0–5
    val total: Int               // 0–100
)
