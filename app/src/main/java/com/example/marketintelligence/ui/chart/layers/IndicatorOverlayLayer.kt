package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.marketintelligence.domain.chart.*

import com.example.tradeengine.models.Candle

/**
 * Draws overlay-type indicators directly on the price chart.
 * Handles SMA, EMA, Bollinger Bands, VWAP, Supertrend, Ichimoku.
 */
fun DrawScope.drawIndicatorOverlayLayer(
    indicators: List<IndicatorConfig>,
    indicatorResults: Map<IndicatorType, Any>,
    candles: List<Candle> = emptyList(),
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

    for (config in indicators) {
        if (!config.enabled || !config.type.isOverlay) continue
        val result = indicatorResults[config.type]
            ?: computeOverlayOnTheFly(config, candles)
            ?: continue

        val baseColor = Color((config.color and 0xFFFFFFFFL).toInt())

        when (config.type) {
            IndicatorType.SMA, IndicatorType.EMA, IndicatorType.VWAP -> {
                @Suppress("UNCHECKED_CAST")
                val values = result as? List<Double?> ?: continue
                drawIndicatorLine(values, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, baseColor, strokeWidth = 1.8f)
            }
            IndicatorType.BOLLINGER_BANDS -> {
                val bbResult = result as? BollingerResult ?: continue
                val bandColor = baseColor
                drawIndicatorLine(bbResult.upper, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, bandColor.copy(alpha = 0.7f), strokeWidth = 1.2f)
                drawIndicatorLine(bbResult.middle, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, bandColor, strokeWidth = 1.6f)
                drawIndicatorLine(bbResult.lower, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, bandColor.copy(alpha = 0.7f), strokeWidth = 1.2f)
                // Fill between bands
                drawBandFill(bbResult.upper, bbResult.lower, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, bandColor.copy(alpha = 0.08f))
            }
            IndicatorType.SUPERTREND -> {
                val stResult = result as? SupertrendResult ?: continue
                drawSupertrendLine(stResult, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight)
            }
            IndicatorType.ICHIMOKU -> {
                val ichResult = result as? IchimokuResult ?: continue
                val teal = baseColor
                drawIndicatorLine(ichResult.tenkanSen, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, Color(0xFF2196F3), strokeWidth = 1.2f)
                drawIndicatorLine(ichResult.kijunSen, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, Color(0xFFFF5722), strokeWidth = 1.2f)
                drawIndicatorLine(ichResult.chikouSpan, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, Color(0xFF4CAF50).copy(alpha = 0.6f), strokeWidth = 1.2f)
                // Cloud fill
                drawBandFill(ichResult.senkouSpanA, ichResult.senkouSpanB, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, teal.copy(alpha = 0.12f))
                drawIndicatorLine(ichResult.senkouSpanA, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, Color(0xFF00E676).copy(alpha = 0.6f), strokeWidth = 1.2f)
                drawIndicatorLine(ichResult.senkouSpanB, visibleStartIndex, visibleEndIndex, candleWidth, priceMin, priceRange, chartAreaHeight, Color(0xFFFF1744).copy(alpha = 0.6f), strokeWidth = 1.2f)
            }
            else -> {}
        }
    }
}

private fun computeOverlayOnTheFly(config: IndicatorConfig, candles: List<Candle>): Any? {
    if (candles.isEmpty()) return null
    return when (config.type) {
        IndicatorType.SMA -> IndicatorCalculator.calculateSMA(candles, config.period)
        IndicatorType.EMA -> IndicatorCalculator.calculateEMA(candles, config.period)
        IndicatorType.BOLLINGER_BANDS -> IndicatorCalculator.calculateBollingerBands(candles, config.period, config.multiplier)
        IndicatorType.VWAP -> IndicatorCalculator.calculateVWAP(candles)
        IndicatorType.SUPERTREND -> IndicatorCalculator.calculateSupertrend(candles, config.period, config.multiplier)
        IndicatorType.ICHIMOKU -> IndicatorCalculator.calculateIchimoku(candles, config.period, config.secondaryPeriod)
        else -> null
    }
}

/**
 * Draws a simple line for a series of indicator values.
 */
private fun DrawScope.drawIndicatorLine(
    values: List<Double?>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    color: Color,
    strokeWidth: Float = 1.5f
) {
    val path = Path()
    var started = false

    for (i in startIndex until minOf(endIndex, values.size)) {
        val value = values[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = chartHeight - ((value - priceMin) / priceRange * chartHeight).toFloat()

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    if (started) {
        drawPath(path, color, style = Stroke(width = strokeWidth.dp.toPx()))
    }
}

/**
 * Draws the Supertrend line with color changes based on direction.
 */
private fun DrawScope.drawSupertrendLine(
    result: SupertrendResult,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float
) {
    val bullColor = Color(0xFF00E676)
    val bearColor = Color(0xFFFF1744)

    var currentPath = Path()
    var currentIsBullish: Boolean? = null
    var pathStarted = false

    for (i in startIndex until minOf(endIndex, result.values.size)) {
        val value = result.values[i] ?: continue
        val direction = result.directions[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = chartHeight - ((value - priceMin) / priceRange * chartHeight).toFloat()

        if (currentIsBullish != null && currentIsBullish != direction) {
            // Direction changed — draw previous segment
            val color = if (currentIsBullish == true) bullColor else bearColor
            drawPath(currentPath, color, style = Stroke(width = 2.dp.toPx()))
            currentPath = Path()
            currentPath.moveTo(x, y)
        } else if (!pathStarted) {
            currentPath.moveTo(x, y)
            pathStarted = true
        } else {
            currentPath.lineTo(x, y)
        }
        currentIsBullish = direction
    }

    // Draw final segment
    if (pathStarted) {
        val color = if (currentIsBullish == true) bullColor else bearColor
        drawPath(currentPath, color, style = Stroke(width = 2.dp.toPx()))
    }
}

/**
 * Fills the area between two indicator bands (Bollinger, Ichimoku cloud).
 */
private fun DrawScope.drawBandFill(
    upper: List<Double?>,
    lower: List<Double?>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    fillColor: Color
) {
    val fillPath = Path()
    val upperPoints = mutableListOf<Offset>()
    val lowerPoints = mutableListOf<Offset>()

    for (i in startIndex until minOf(endIndex, minOf(upper.size, lower.size))) {
        val u = upper[i] ?: continue
        val l = lower[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val uy = chartHeight - ((u - priceMin) / priceRange * chartHeight).toFloat()
        val ly = chartHeight - ((l - priceMin) / priceRange * chartHeight).toFloat()
        upperPoints.add(Offset(x, uy))
        lowerPoints.add(Offset(x, ly))
    }

    if (upperPoints.size < 2) return

    fillPath.moveTo(upperPoints.first().x, upperPoints.first().y)
    for (point in upperPoints) fillPath.lineTo(point.x, point.y)
    for (point in lowerPoints.reversed()) fillPath.lineTo(point.x, point.y)
    fillPath.close()

    drawPath(fillPath, fillColor, style = Fill)
}
