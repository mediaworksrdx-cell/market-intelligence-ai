package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.marketintelligence.domain.chart.ChartType
import com.example.marketintelligence.ui.chart.HeikinAshiCandle
import com.example.tradeengine.models.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val BullColor = Color(0xFF00E676)
private val BearColor = Color(0xFFFF1744)
private val LineColor = Color(0xFF42A5F5)
private val AreaFill = Color(0xFF42A5F5).copy(alpha = 0.12f)

/**
 * Draws the main price chart in the selected chart type.
 */
fun DrawScope.drawCandleLayer(
    candles: List<Candle>,
    heikinAshiCandles: List<HeikinAshiCandle>,
    chartType: ChartType,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float
) {
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0) return

    val chartWidth = size.width - rightMargin
    val candleWidth = chartWidth / visibleCount
    val priceRange = priceMax - priceMin
    if (priceRange <= 0) return

    when (chartType) {
        ChartType.CANDLESTICK -> drawCandlesticks(candles, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, filled = true)
        ChartType.HOLLOW_CANDLE -> drawCandlesticks(candles, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, filled = false)
        ChartType.LINE -> drawLineChart(candles, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight)
        ChartType.AREA -> drawAreaChart(candles, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight)
        ChartType.HEIKIN_ASHI -> drawHeikinAshi(heikinAshiCandles, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight)
    }
}

private fun DrawScope.drawCandlesticks(
    candles: List<Candle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    filled: Boolean
) {
    val bodyWidth = candleWidth * 0.65f
    val wickWidth = 1.dp.toPx()

    for (i in startIndex until minOf(endIndex, candles.size)) {
        val localIndex = i - startIndex
        val candle = candles[i]
        val isBullish = candle.close >= candle.open
        val color = if (isBullish) BullColor else BearColor

        val centerX = localIndex * candleWidth + candleWidth / 2f

        // Y coordinates (inverted: higher price = lower y)
        val highY = chartHeight - ((candle.high - priceMin) / priceRange * chartHeight).toFloat()
        val lowY = chartHeight - ((candle.low - priceMin) / priceRange * chartHeight).toFloat()
        val openY = chartHeight - ((candle.open - priceMin) / priceRange * chartHeight).toFloat()
        val closeY = chartHeight - ((candle.close - priceMin) / priceRange * chartHeight).toFloat()

        val bodyTop = min(openY, closeY)
        val bodyBottom = max(openY, closeY)
        val bodyHeight = max(bodyBottom - bodyTop, 1f)

        // Draw wick
        drawLine(
            color = color,
            start = Offset(centerX, highY),
            end = Offset(centerX, lowY),
            strokeWidth = wickWidth
        )

        // Draw body
        val bodyLeft = centerX - bodyWidth / 2f
        if (filled || !isBullish) {
            drawRect(
                color = color,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight)
            )
        } else {
            // Hollow candle (bullish in hollow mode)
            drawRect(
                color = color,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawHeikinAshi(
    haCandles: List<HeikinAshiCandle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float
) {
    val bodyWidth = candleWidth * 0.65f
    val wickWidth = 1.dp.toPx()

    for (i in startIndex until minOf(endIndex, haCandles.size)) {
        val localIndex = i - startIndex
        val candle = haCandles[i]
        val isBullish = candle.close >= candle.open
        val color = if (isBullish) BullColor else BearColor
        val centerX = localIndex * candleWidth + candleWidth / 2f

        val highY = chartHeight - ((candle.high - priceMin) / priceRange * chartHeight).toFloat()
        val lowY = chartHeight - ((candle.low - priceMin) / priceRange * chartHeight).toFloat()
        val openY = chartHeight - ((candle.open - priceMin) / priceRange * chartHeight).toFloat()
        val closeY = chartHeight - ((candle.close - priceMin) / priceRange * chartHeight).toFloat()

        val bodyTop = min(openY, closeY)
        val bodyBottom = max(openY, closeY)
        val bodyHeight = max(bodyBottom - bodyTop, 1f)

        drawLine(color = color, start = Offset(centerX, highY), end = Offset(centerX, lowY), strokeWidth = wickWidth)
        drawRect(color = color, topLeft = Offset(centerX - bodyWidth / 2f, bodyTop), size = Size(bodyWidth, bodyHeight))
    }
}

private fun DrawScope.drawLineChart(
    candles: List<Candle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float
) {
    val path = Path()
    var started = false

    for (i in startIndex until minOf(endIndex, candles.size)) {
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = chartHeight - ((candles[i].close - priceMin) / priceRange * chartHeight).toFloat()

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(path, LineColor, style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawAreaChart(
    candles: List<Candle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float
) {
    val linePath = Path()
    val fillPath = Path()
    var started = false
    var firstX = 0f
    var lastX = 0f

    for (i in startIndex until minOf(endIndex, candles.size)) {
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = chartHeight - ((candles[i].close - priceMin) / priceRange * chartHeight).toFloat()

        if (!started) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, chartHeight)
            fillPath.lineTo(x, y)
            firstX = x
            started = true
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        lastX = x
    }

    // Close fill path
    fillPath.lineTo(lastX, chartHeight)
    fillPath.lineTo(firstX, chartHeight)
    fillPath.close()

    drawPath(fillPath, AreaFill, style = Fill)
    drawPath(linePath, LineColor, style = Stroke(width = 1.5.dp.toPx()))
}
