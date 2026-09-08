package com.example.marketintelligence.ui.chart

import com.example.marketintelligence.domain.chart.*
import com.example.tradeengine.models.Candle

/**
 * Immutable state holder for the advanced chart composable.
 * Combines raw data with all user-configurable chart options.
 */
data class ChartState(
    val candles: List<Candle> = emptyList(),
    val chartType: ChartType = ChartType.CANDLESTICK,
    val activeIndicators: List<IndicatorConfig> = emptyList(),
    val drawings: List<DrawingData> = emptyList(),
    val activeDrawingTool: DrawingToolType = DrawingToolType.NONE,
    val currentDrawing: DrawingData? = null,
    val showVolume: Boolean = true,
    val showGrid: Boolean = true,

    // Institutional Microstructure & Derivatives Overlays
    val fnoLevels: FnoOverlayLevels? = null,
    val strategyOverlay: StrategyPayoffOverlay? = null,
    val volumeProfile: VolumeProfileData? = null,
    val smcAnalysis: SmcAnalysis? = null,
    val showVolumeProfile: Boolean = true,
    val showFnoOverlay: Boolean = true,
    val showSmcOverlay: Boolean = true,
    val showStrategyOverlay: Boolean = true,

    // Computed indicator data (cached for performance)
    val indicatorResults: Map<IndicatorType, Any> = emptyMap()
) {
    val hasOverlayIndicators: Boolean
        get() = activeIndicators.any { it.type.isOverlay && it.enabled }

    val hasPanelIndicators: Boolean
        get() = activeIndicators.any { !it.type.isOverlay && it.enabled }

    val panelIndicators: List<IndicatorConfig>
        get() = activeIndicators.filter { !it.type.isOverlay && it.enabled }

    val overlayIndicators: List<IndicatorConfig>
        get() = activeIndicators.filter { it.type.isOverlay && it.enabled }
}

/**
 * Represents a Heikin-Ashi candle derived from regular candles.
 */
data class HeikinAshiCandle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val openTime: Long,
    val closeTime: Long,
    val volume: Double
)

/**
 * Converts regular candles to Heikin-Ashi candles.
 */
fun List<Candle>.toHeikinAshi(): List<HeikinAshiCandle> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<HeikinAshiCandle>()

    for (i in indices) {
        val candle = this[i]
        val haClose = (candle.open + candle.high + candle.low + candle.close) / 4.0
        val haOpen = if (i == 0) {
            (candle.open + candle.close) / 2.0
        } else {
            (result[i - 1].open + result[i - 1].close) / 2.0
        }
        val haHigh = maxOf(candle.high, haOpen, haClose)
        val haLow = minOf(candle.low, haOpen, haClose)

        result.add(
            HeikinAshiCandle(
                open = haOpen,
                high = haHigh,
                low = haLow,
                close = haClose,
                openTime = candle.openTime,
                closeTime = candle.closeTime,
                volume = candle.volume
            )
        )
    }
    return result
}
