package com.example.marketintelligence.domain.chart

import androidx.compose.ui.graphics.Color

/**
 * Types of chart visualizations available.
 */
enum class ChartType(val label: String) {
    CANDLESTICK("Candle"),
    LINE("Line"),
    AREA("Area"),
    HEIKIN_ASHI("Heikin-Ashi"),
    HOLLOW_CANDLE("Hollow")
}

/**
 * Technical indicators that can be overlaid on the chart.
 * Indicators are categorized as either overlay (drawn on the price chart)
 * or panel (drawn in a separate sub-panel).
 */
enum class IndicatorType(val label: String, val isOverlay: Boolean) {
    SMA("SMA", true),
    EMA("EMA", true),
    BOLLINGER_BANDS("Bollinger", true),
    VWAP("VWAP", true),
    SUPERTREND("Supertrend", true),
    ICHIMOKU("Ichimoku", true),
    RSI("RSI", false),
    MACD("MACD", false),
    STOCHASTIC("Stochastic", false),
    ATR("ATR", false),
    CVD("CVD", false)
}

/**
 * Cursor navigation mode for the chart.
 */
enum class CursorMode(val label: String) {
    CROSSHAIR("Crosshair"),
    HAND("Hand")
}

/**
 * Drawing tool types for chart annotations.
 */
enum class DrawingToolType(val label: String) {
    NONE("None"),
    TRENDLINE("Trendline"),
    HORIZONTAL_LINE("H-Line"),
    FIBONACCI("Fibonacci"),
    RECTANGLE("Rectangle"),
    CHANNEL("Channel")
}

/**
 * Configuration for a single technical indicator instance.
 */
data class IndicatorConfig(
    val type: IndicatorType,
    val period: Int = defaultPeriod(type),
    val secondaryPeriod: Int = defaultSecondaryPeriod(type),
    val color: Long = defaultColor(type),
    val secondaryColor: Long = 0xFF42A5F5,
    val tertiaryColor: Long = 0xFFEF5350,
    val enabled: Boolean = true,
    val multiplier: Double = defaultMultiplier(type)
) {
    companion object {
        fun defaultPeriod(type: IndicatorType): Int = when (type) {
            IndicatorType.SMA -> 20
            IndicatorType.EMA -> 21
            IndicatorType.BOLLINGER_BANDS -> 20
            IndicatorType.VWAP -> 1
            IndicatorType.SUPERTREND -> 10
            IndicatorType.ICHIMOKU -> 9
            IndicatorType.RSI -> 14
            IndicatorType.MACD -> 12
            IndicatorType.STOCHASTIC -> 14
            IndicatorType.ATR -> 14
            IndicatorType.CVD -> 14
        }

        fun defaultSecondaryPeriod(type: IndicatorType): Int = when (type) {
            IndicatorType.MACD -> 26
            IndicatorType.STOCHASTIC -> 3
            IndicatorType.ICHIMOKU -> 26
            else -> 0
        }

        fun defaultMultiplier(type: IndicatorType): Double = when (type) {
            IndicatorType.BOLLINGER_BANDS -> 2.0
            IndicatorType.SUPERTREND -> 3.0
            else -> 1.0
        }

        fun defaultColor(type: IndicatorType): Long = when (type) {
            IndicatorType.SMA -> 0xFFFF9800       // Orange
            IndicatorType.EMA -> 0xFF2196F3        // Blue
            IndicatorType.BOLLINGER_BANDS -> 0xFF9C27B0  // Purple
            IndicatorType.VWAP -> 0xFFFFEB3B        // Yellow
            IndicatorType.SUPERTREND -> 0xFF00E676  // Green
            IndicatorType.ICHIMOKU -> 0xFF26A69A     // Teal
            IndicatorType.RSI -> 0xFFAB47BC          // Light purple
            IndicatorType.MACD -> 0xFF29B6F6         // Light blue
            IndicatorType.STOCHASTIC -> 0xFFFF7043   // Deep orange
            IndicatorType.ATR -> 0xFF78909C           // Blue grey
            IndicatorType.CVD -> 0xFF00E5FF           // Cyan
        }
    }
}

/**
 * A user-created chart drawing.
 */
data class DrawingData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolType: DrawingToolType,
    val points: List<ChartPoint>,
    val color: Long = 0xFFFFFFFF,
    val lineWidth: Float = 2f,
    val isComplete: Boolean = false
)

/**
 * A point on the chart in data coordinates (time, price).
 */
data class ChartPoint(
    val timestamp: Long,
    val price: Double
)

/**
 * Viewport state for the chart canvas.
 */
data class ChartViewportState(
    val visibleStartIndex: Int = 0,
    val visibleEndIndex: Int = 0,
    val priceMin: Double = 0.0,
    val priceMax: Double = 0.0,
    val panOffset: Float = 0f,
    val zoomLevel: Float = 1f
)

// ── Indicator Result Models ──

data class MACDResult(
    val macdLine: List<Double?>,
    val signalLine: List<Double?>,
    val histogram: List<Double?>
)

data class BollingerResult(
    val upper: List<Double?>,
    val middle: List<Double?>,
    val lower: List<Double?>
)

data class SupertrendResult(
    val values: List<Double?>,
    val directions: List<Boolean?> // true = bullish, false = bearish
)

data class StochasticResult(
    val kLine: List<Double?>,
    val dLine: List<Double?>
)

data class IchimokuResult(
    val tenkanSen: List<Double?>,     // Conversion Line (9)
    val kijunSen: List<Double?>,      // Base Line (26)
    val senkouSpanA: List<Double?>,   // Leading Span A
    val senkouSpanB: List<Double?>,   // Leading Span B
    val chikouSpan: List<Double?>     // Lagging Span
)

// ── Institutional Microstructure & F&O Models ──

data class CvdResult(
    val cvdLine: List<Double?>,
    val deltaBars: List<Double?>
)

data class FnoOverlayLevels(
    val callWall: Double? = null,
    val putWall: Double? = null,
    val gammaFlip: Double? = null,
    val maxPain: Double? = null,
    val callWallGex: Double? = null,
    val putWallGex: Double? = null,
    val totalNetGex: Double? = null,
    val enabled: Boolean = false
)

data class StrategyPayoffOverlay(
    val strategyName: String = "",
    val breakevenPoints: List<Double> = emptyList(),
    val maxProfitZone: ClosedFloatingPointRange<Double>? = null,
    val maxLossZone: ClosedFloatingPointRange<Double>? = null,
    val maxProfit: Double? = null,
    val maxLoss: Double? = null,
    val targetPrice: Double? = null,
    val enabled: Boolean = false
)

data class VolumeProfileBucket(
    val priceLow: Double,
    val priceHigh: Double,
    val buyVolume: Double,
    val sellVolume: Double,
    val totalVolume: Double
)

data class VolumeProfileData(
    val buckets: List<VolumeProfileBucket> = emptyList(),
    val pocPrice: Double = 0.0,
    val vahPrice: Double = 0.0,
    val valPrice: Double = 0.0,
    val maxBucketVolume: Double = 0.0
)

data class SmcFvg(
    val startIndex: Int,
    var endIndex: Int,
    val topPrice: Double,
    val bottomPrice: Double,
    val isBullish: Boolean,
    var isMitigated: Boolean = false
)

data class SmcLiquiditySweep(
    val candleIndex: Int,
    val price: Double,
    val isHighSweep: Boolean,
    val label: String
)

data class SmcAnalysis(
    val fvgs: List<SmcFvg> = emptyList(),
    val sweeps: List<SmcLiquiditySweep> = emptyList()
)

