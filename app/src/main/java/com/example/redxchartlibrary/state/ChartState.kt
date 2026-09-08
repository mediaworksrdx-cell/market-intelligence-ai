package com.example.redxchartlibrary.state

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.example.redxchartlibrary.data.indicators.Indicator
import com.example.redxchartlibrary.data.indicators.IndicatorFactory
import com.example.redxchartlibrary.data.indicators.IndicatorOutput
import com.example.redxchartlibrary.data.local.AnchorPoint
import com.example.redxchartlibrary.data.local.Drawing
import com.example.redxchartlibrary.data.local.DrawingTool
import com.example.redxchartlibrary.model.Candle

class ChartState {
    // Indicator State
    val indicators = mutableStateOf<List<Indicator>>(emptyList())
    val indicatorOutputs = mutableStateMapOf<Indicator, IndicatorOutput>()

    // Drawing State
    val activeDrawingTool = mutableStateOf<DrawingTool?>(null)
    val drawings = mutableStateOf<List<Drawing>>(emptyList())
    val currentDrawing = mutableStateOf<Drawing?>(null)

    fun addIndicator(indicator: Indicator, candles: List<Candle>) {
        indicators.value = indicators.value + indicator
        recalculateIndicator(indicator, candles)
    }

    fun removeIndicator(indicator: Indicator) {
        indicators.value = indicators.value - indicator
        indicatorOutputs.remove(indicator)
    }

    fun recalculateIndicators(candles: List<Candle>) {
        indicators.value.forEach { indicator ->
            recalculateIndicator(indicator, candles)
        }
    }

    private fun recalculateIndicator(indicator: Indicator, candles: List<Candle>) {
        indicatorOutputs[indicator] = IndicatorFactory.calculate(indicator, candles)
    }

    fun startDrawing(anchor: AnchorPoint, symbol: String, timeframe: String) {
        val newTool = when (activeDrawingTool.value) {
            is DrawingTool.Trendline -> DrawingTool.Trendline(start = anchor, end = anchor)
            is DrawingTool.HorizontalLine -> DrawingTool.HorizontalLine(anchor = anchor)
            else -> null
        }
        newTool?.let {
            currentDrawing.value = Drawing(symbol = symbol, timeframe = timeframe, tool = it)
        }
    }

    fun updateDrawing(anchor: AnchorPoint) {
        currentDrawing.value?.let {
            val updatedTool = when (val tool = it.tool) {
                is DrawingTool.Trendline -> tool.copy(end = anchor)
                else -> tool // No update needed for other tools
            }
            currentDrawing.value = it.copy(tool = updatedTool)
        }
    }

    fun finalizeDrawing(): Drawing? {
        val finalized = currentDrawing.value
        currentDrawing.value = null
        activeDrawingTool.value = null // Deactivate tool after use
        finalized?.let {
            drawings.value = drawings.value + it
        }
        return finalized
    }
}
