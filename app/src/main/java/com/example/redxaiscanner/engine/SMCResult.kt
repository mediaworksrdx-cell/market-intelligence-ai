package com.example.redxaiscanner.engine

import java.math.BigDecimal

// Update FVGResult to carry its own explanation
data class FVGResult(
    val direction: FVGDireciton,
    val top: BigDecimal,
    val bottom: BigDecimal,
    val strength: Double,
    val isMitigated: Boolean,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val explanation: ExplanationComponent // Added for self-documentation
)

// Update the primary SMC result to carry the main bias explanation
data class SMCAnalysisResult(
    val bias: MarketBias,
    val swingPoints: List<SwingPoint>,
    val structureEvents: List<MarketStructureEvent>,
    val orderBlocks: List<OrderBlock>,
    val liquidityZones: List<LiquidityZone>,
    val premiumDiscountZone: PremiumDiscountZone?,
    val explanation: ExplanationComponent // Added for self-documentation
)

// ... (Other SMC data classes remain the same)
enum class MarketBias {
    BULLISH, BEARISH, RANGING
}
data class SwingPoint(val type: SwingType, val price: BigDecimal, val timestamp: Long)
enum class SwingType { HIGH, LOW }
data class MarketStructureEvent(val type: EventType, val timestamp: Long, val price: BigDecimal, val brokenSwingPointTimestamp: Long)
enum class EventType { BOS, CHoCH }
data class OrderBlock(val direction: FVGDireciton, val top: BigDecimal, val bottom: BigDecimal, val timestamp: Long, var isMitigated: Boolean = false)
data class LiquidityZone(val type: LiquidityType, val priceLevel: BigDecimal, val startTimestamp: Long, val endTimestamp: Long)
enum class LiquidityType { EQUAL_HIGHS, EQUAL_LOWS }
data class PremiumDiscountZone(val legHigh: BigDecimal, val legLow: BigDecimal, val equilibrium: BigDecimal)
enum class FVGDireciton { BULLISH, BEARISH }