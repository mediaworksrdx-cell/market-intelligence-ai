package com.marketintelligence.cryptotracker.engine

/**
 * Represents the overall structural state of the market.
 */
enum class StructureState { BULLISH, BEARISH, RANGING, TRANSITIONING }

/**
 * Classification of swing points based on their relationship to previous swings.
 */
enum class SwingClassification { HH, HL, LH, LL, UNCLASSIFIED }

/**
 * A swing point in the market, acting as a structural high or low.
 */
data class CryptoSwingPoint(
    val type: SwingType,
    val price: Double,
    val timestamp: Long,
    val classification: SwingClassification = SwingClassification.UNCLASSIFIED
)

/**
 * Type of swing point.
 */
enum class SwingType { HIGH, LOW }

/**
 * Type of market structure event.
 * BOS: Break of Structure (trend continuation)
 * CHoCH: Change of Character (early trend reversal)
 * MSS: Market Structure Shift (confirmed trend reversal)
 */
enum class StructureEventType { BOS, CHoCH, MSS }

/**
 * Direction of the market structure.
 */
enum class StructureDirection { BULLISH, BEARISH }

/**
 * Type of confirmation for structural breaks.
 */
enum class ConfirmationType { CLOSE, WICK }

/**
 * Represents an event that breaks market structure.
 */
data class StructureEvent(
    val type: StructureEventType,
    val price: Double,
    val timestamp: Long,
    val direction: StructureDirection,
    val confirmationType: ConfirmationType,
    val brokenLevelTimestamp: Long
)

/**
 * Origin source of the liquidity pool.
 */
enum class LiquiditySource {
    SWING_HIGH, SWING_LOW,
    EQUAL_HIGHS, EQUAL_LOWS,
    SESSION_HIGH, SESSION_LOW,
    PREV_DAY_HIGH, PREV_DAY_LOW,
    PREV_WEEK_HIGH, PREV_WEEK_LOW
}

/**
 * Side of liquidity pool. BUY exists above price (short stop losses). SELL exists below price (long stop losses).
 */
enum class LiquiditySide { BUY, SELL }

/**
 * Represents a key liquidity level in the market.
 */
data class LiquidityLevel(
    val side: LiquiditySide,
    val source: LiquiditySource,
    val price: Double,
    val timestamp: Long
)

/**
 * A candle demonstrating significant institutional momentum/displacement.
 */
data class DisplacementCandle(
    val direction: StructureDirection,
    val bodySize: Double,
    val atrMultiple: Double,
    val volume: Double,
    val timestamp: Long
)

/**
 * Represents an event where price runs through a liquidity level and rejects (sweeps it).
 */
data class LiquiditySweep(
    val level: LiquidityLevel,
    val sweepPrice: Double,
    val sweepTimestamp: Long,
    val reclaimed: Boolean,
    val displacement: DisplacementCandle?
)

/**
 * Premium and discount zones.
 */
data class PremiumDiscountZone(
    val high: Double,
    val low: Double,
    val equilibrium: Double
)

/**
 * The consolidated Smart Money Concepts (SMC) analysis.
 */
data class CryptoSMCAnalysis(
    val structureState: StructureState,
    val swingPoints: List<CryptoSwingPoint>,
    val structureEvents: List<StructureEvent>,
    val liquidityLevels: List<LiquidityLevel>,
    val sweeps: List<LiquiditySweep>,
    val displacements: List<DisplacementCandle>,
    val atrMap: Map<Long, Double>,
    val premiumDiscountZone: PremiumDiscountZone?
)
