package com.example.tradeengine.engine

import kotlinx.serialization.Serializable
import com.example.tradeengine.util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class FVGResult(
    val direction: FVGDireciton,
    @Serializable(with = BigDecimalSerializer::class) val top: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val bottom: BigDecimal,
    val strength: Double,
    val isMitigated: Boolean,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val explanation: ExplanationComponent
)

@Serializable
data class SMCAnalysisResult(
    val bias: MarketBias,
    val swingPoints: List<SwingPoint>,
    val structureEvents: List<MarketStructureEvent>,
    val orderBlocks: List<OrderBlock>,
    val liquidityZones: List<LiquidityZone>,
    val premiumDiscountZone: PremiumDiscountZone?,
    val explanation: ExplanationComponent
)

@Serializable
enum class MarketBias {
    BULLISH, BEARISH, RANGING
}

@Serializable
data class SwingPoint(
    val type: SwingType,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    val timestamp: Long
)

@Serializable
enum class SwingType { HIGH, LOW }

@Serializable
data class MarketStructureEvent(
    val type: EventType,
    val timestamp: Long,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    val brokenSwingPointTimestamp: Long
)

@Serializable
enum class EventType { BOS, CHoCH }

@Serializable
data class OrderBlock(
    val direction: FVGDireciton,
    @Serializable(with = BigDecimalSerializer::class) val top: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val bottom: BigDecimal,
    val timestamp: Long,
    var isMitigated: Boolean = false
)

@Serializable
data class LiquidityZone(
    val type: LiquidityType,
    @Serializable(with = BigDecimalSerializer::class) val priceLevel: BigDecimal,
    val startTimestamp: Long,
    val endTimestamp: Long
)

@Serializable
enum class LiquidityType { EQUAL_HIGHS, EQUAL_LOWS }

@Serializable
data class PremiumDiscountZone(
    @Serializable(with = BigDecimalSerializer::class) val legHigh: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val legLow: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val equilibrium: BigDecimal
)

@Serializable
enum class FVGDireciton { BULLISH, BEARISH }
