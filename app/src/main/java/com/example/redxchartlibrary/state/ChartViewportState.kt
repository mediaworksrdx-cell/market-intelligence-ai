package com.example.redxchartlibrary.state

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import com.example.redxchartlibrary.model.Candle

@Stable
class ChartViewportState(
    val candles: List<Candle>,
    canvasSize: Size
) {
    var panX by mutableStateOf(0f)
    var zoom by mutableStateOf(1f)

    private val candleWidth: Float
        get() = 20f * zoom

    val visibleCandleRange: IntRange by derivedStateOf {
        val startIndex = ((-panX) / candleWidth).toInt().coerceAtLeast(0)
        val visibleCount = (canvasSize.width / candleWidth).toInt() + 2
        val endIndex = (startIndex + visibleCount).coerceAtMost(candles.size)
        IntRange(startIndex, endIndex)
    }

    val visibleCandles: List<Candle> by derivedStateOf {
        if (visibleCandleRange.first >= 0 && visibleCandleRange.last > visibleCandleRange.first) {
            candles.subList(visibleCandleRange.first, visibleCandleRange.last)
        } else {
            emptyList()
        }
    }

    val priceRange: ClosedFloatingPointRange<Float> by derivedStateOf {
        val minPrice = (visibleCandles.minOfOrNull { it.low } ?: 0.0).toFloat()
        val maxPrice = (visibleCandles.maxOfOrNull { it.high } ?: 1.0).toFloat()
        minPrice..maxPrice
    }

    fun toScreenX(index: Int): Float {
        return panX + index * candleWidth
    }

    fun toScreenY(price: Float): Float {
        val priceDiff = priceRange.endInclusive - priceRange.start
        if (priceDiff == 0f) return 0f
        return (1 - (price - priceRange.start) / priceDiff)
    }
}

@Composable
fun rememberChartViewportState(candles: List<Candle>, canvasSize: Size): ChartViewportState {
    return remember(candles, canvasSize) {
        ChartViewportState(candles, canvasSize)
    }
}
