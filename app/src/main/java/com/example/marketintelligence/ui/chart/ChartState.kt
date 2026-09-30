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
    val selectedDrawingId: String? = null,
    val cursorMode: CursorMode = CursorMode.HAND,
    val showVolume: Boolean = true,
    val showGrid: Boolean = true,

    // Institutional Microstructure & Derivatives Overlays
    val fnoLevels: FnoOverlayLevels? = null,
    val strategyOverlay: StrategyPayoffOverlay? = null,
    val volumeProfile: VolumeProfileData? = null,
    val smcAnalysis: SmcAnalysis? = null,
    val showVolumeProfile: Boolean = true,
    val showFnoOverlay: Boolean = false,
    val showSmcOverlay: Boolean = true,
    val showStrategyOverlay: Boolean = false,

    // Computed indicator data (cached for performance)
    val indicatorResults: Map<IndicatorType, Any> = emptyMap(),
    
    val drawingHistory: MutableList<List<DrawingData>> = mutableListOf(),
    val redoStack: MutableList<List<DrawingData>> = mutableListOf()
) {
    val hasOverlayIndicators: Boolean
        get() = activeIndicators.any { it.type.isOverlay && it.enabled }

    val hasPanelIndicators: Boolean
        get() = activeIndicators.any { !it.type.isOverlay && it.enabled }

    val panelIndicators: List<IndicatorConfig>
        get() = activeIndicators.filter { !it.type.isOverlay && it.enabled }

    val overlayIndicators: List<IndicatorConfig>
        get() = activeIndicators.filter { it.type.isOverlay && it.enabled }
        
    fun pushDrawingState() {
        drawingHistory.add(drawings)
        redoStack.clear()
    }
    
    fun undo(): ChartState {
        if (drawingHistory.isNotEmpty()) {
            val current = drawings
            redoStack.add(current)
            val previous = drawingHistory.removeAt(drawingHistory.size - 1)
            return copy(drawings = previous)
        }
        return this
    }
    
    fun redo(): ChartState {
        if (redoStack.isNotEmpty()) {
            val current = drawings
            drawingHistory.add(current)
            val next = redoStack.removeAt(redoStack.size - 1)
            return copy(drawings = next)
        }
        return this
    }

    companion object {
        private const val PREFS_NAME = "ChartDrawingsPrefs"
        
        fun saveDrawings(context: android.content.Context, symbol: String, drawings: List<DrawingData>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val json = com.google.gson.Gson().toJson(drawings)
            prefs.edit().putString(symbol, json).apply()
        }

        fun loadDrawings(context: android.content.Context, symbol: String): List<DrawingData> {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val json = prefs.getString(symbol, null) ?: return emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<DrawingData>>() {}.type
            return try {
                com.google.gson.Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
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
