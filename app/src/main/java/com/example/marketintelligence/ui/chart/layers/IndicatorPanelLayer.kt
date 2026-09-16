package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.*

/**
 * Draws sub-panel indicators (RSI, MACD, Stochastic, ATR) below the main chart.
 * Each panel gets its own fixed-height region with its own Y-axis scaling.
 */
fun DrawScope.drawIndicatorPanelLayer(
    indicators: List<IndicatorConfig>,
    indicatorResults: Map<IndicatorType, Any>,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    panelTop: Float,
    panelHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer
) {
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0) return
    val chartWidth = size.width - rightMargin
    val candleWidth = chartWidth / visibleCount

    val panelIndicators = indicators.filter { !it.type.isOverlay && it.enabled }
    if (panelIndicators.isEmpty()) return

    val singlePanelHeight = panelHeight / panelIndicators.size

    panelIndicators.forEachIndexed { index, config ->
        val currentPanelTop = panelTop + index * singlePanelHeight
        val result = indicatorResults[config.type]

        // Panel separator
        drawLine(
            color = Color(0xFF1A1A1A),
            start = Offset(0f, currentPanelTop),
            end = Offset(chartWidth, currentPanelTop),
            strokeWidth = 1.dp.toPx()
        )

        // Panel label
        val label = config.type.label
        val labelResult = textMeasurer.measure(
            label,
            TextStyle(
                color = Color((config.color and 0xFFFFFFFFL).toInt()),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        drawText(labelResult, topLeft = Offset(4.dp.toPx(), currentPanelTop + 2.dp.toPx()))

        when (config.type) {
            IndicatorType.RSI -> {
                @Suppress("UNCHECKED_CAST")
                val values = result as? List<Double?> ?: return@forEachIndexed
                drawRSIPanel(values, visibleStartIndex, visibleEndIndex, candleWidth, currentPanelTop, singlePanelHeight, chartWidth, Color((config.color and 0xFFFFFFFFL).toInt()), textMeasurer)
            }
            IndicatorType.MACD -> {
                val macdResult = result as? MACDResult ?: return@forEachIndexed
                drawMACDPanel(macdResult, visibleStartIndex, visibleEndIndex, candleWidth, currentPanelTop, singlePanelHeight, chartWidth, config)
            }
            IndicatorType.STOCHASTIC -> {
                val stochResult = result as? StochasticResult ?: return@forEachIndexed
                drawStochasticPanel(stochResult, visibleStartIndex, visibleEndIndex, candleWidth, currentPanelTop, singlePanelHeight, chartWidth, config, textMeasurer)
            }
            IndicatorType.ATR -> {
                @Suppress("UNCHECKED_CAST")
                val values = result as? List<Double?> ?: return@forEachIndexed
                drawGenericLinePanel(values, visibleStartIndex, visibleEndIndex, candleWidth, currentPanelTop, singlePanelHeight, chartWidth, Color((config.color and 0xFFFFFFFFL).toInt()))
            }
            IndicatorType.CVD -> {
                val cvdResult = result as? CvdResult ?: return@forEachIndexed
                drawCvdPanel(cvdResult, visibleStartIndex, visibleEndIndex, candleWidth, currentPanelTop, singlePanelHeight, chartWidth, config, textMeasurer)
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawCvdPanel(
    result: CvdResult,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    config: IndicatorConfig,
    textMeasurer: TextMeasurer
) {
    val padding = 4.dp.toPx()
    val drawHeight = panelHeight - padding * 2
    val drawTop = panelTop + padding

    // Find min and max for visible CVD line
    var minVal = Double.MAX_VALUE
    var maxVal = Double.MIN_VALUE
    for (i in startIndex until minOf(endIndex, result.cvdLine.size)) {
        result.cvdLine[i]?.let {
            minVal = minOf(minVal, it)
            maxVal = maxOf(maxVal, it)
        }
    }
    if (minVal >= maxVal) return
    minVal = minOf(minVal, 0.0)
    maxVal = maxOf(maxVal, 0.0)
    val range = maxVal - minVal
    if (range <= 0.0) return

    // Zero line
    val zeroY = drawTop + drawHeight * (1 - (0.0 - minVal) / range).toFloat()
    drawLine(Color(0xFF37474F), Offset(0f, zeroY), Offset(chartWidth, zeroY), strokeWidth = 0.8.dp.toPx())

    // Zero label
    val zeroText = textMeasurer.measure("0", TextStyle(color = Color(0xFF78909C), fontSize = 7.sp, fontFamily = FontFamily.Monospace))
    drawText(zeroText, topLeft = Offset(chartWidth + 2.dp.toPx(), zeroY - zeroText.size.height / 2f))

    // Delta bars (histogram around zero line)
    val barWidth = candleWidth * 0.45f
    var maxDelta = 1.0
    for (i in startIndex until minOf(endIndex, result.deltaBars.size)) {
        result.deltaBars[i]?.let { maxDelta = maxOf(maxDelta, kotlin.math.abs(it)) }
    }
    val maxBarHeight = drawHeight * 0.25f

    for (i in startIndex until minOf(endIndex, result.deltaBars.size)) {
        val delta = result.deltaBars[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + (candleWidth - barWidth) / 2f
        val barH = (kotlin.math.abs(delta) / maxDelta * maxBarHeight).toFloat().coerceAtLeast(1f)
        val color = if (delta >= 0) Color(0xFF00E676).copy(alpha = 0.4f) else Color(0xFFFF1744).copy(alpha = 0.4f)
        val top = if (delta >= 0) zeroY - barH else zeroY
        drawRect(color, topLeft = Offset(x, top), size = Size(barWidth, barH))
    }

    // CVD Cumulative Line
    val cvdColor = Color((config.color and 0xFFFFFFFFL).toInt())
    val path = Path()
    var started = false
    for (i in startIndex until minOf(endIndex, result.cvdLine.size)) {
        val value = result.cvdLine[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = drawTop + drawHeight * (1 - (value - minVal) / range).toFloat()
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    if (started) {
        drawPath(path, cvdColor, style = Stroke(width = 1.5.dp.toPx()))
    }

    // Current CVD Value badge
    val lastIdx = minOf(endIndex - 1, result.cvdLine.size - 1)
    if (lastIdx >= 0) {
        result.cvdLine[lastIdx]?.let { curVal ->
            val curFormatted = if (kotlin.math.abs(curVal) >= 1e6) {
                "%.1fM".format(curVal / 1e6)
            } else if (kotlin.math.abs(curVal) >= 1e3) {
                "%.1fK".format(curVal / 1e3)
            } else {
                "%.0f".format(curVal)
            }
            val badge = textMeasurer.measure(
                "CVD: $curFormatted",
                TextStyle(color = cvdColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(badge, topLeft = Offset(chartWidth - badge.size.width - 4.dp.toPx(), panelTop + 2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawRSIPanel(
    values: List<Double?>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    color: Color,
    textMeasurer: TextMeasurer
) {
    val padding = 4.dp.toPx()
    val drawHeight = panelHeight - padding * 2
    val drawTop = panelTop + padding

    // Overbought (70) and Oversold (30) lines
    val ob70Y = drawTop + drawHeight * (1 - 70.0 / 100.0).toFloat()
    val os30Y = drawTop + drawHeight * (1 - 30.0 / 100.0).toFloat()

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
    drawLine(Color(0xFFFF1744).copy(alpha = 0.4f), Offset(0f, ob70Y), Offset(chartWidth, ob70Y), strokeWidth = 0.5.dp.toPx(), pathEffect = dashEffect)
    drawLine(Color(0xFF00E676).copy(alpha = 0.4f), Offset(0f, os30Y), Offset(chartWidth, os30Y), strokeWidth = 0.5.dp.toPx(), pathEffect = dashEffect)

    // RSI 50 midline
    val mid50Y = drawTop + drawHeight * 0.5f
    drawLine(Color(0xFF333333), Offset(0f, mid50Y), Offset(chartWidth, mid50Y), strokeWidth = 0.5.dp.toPx(), pathEffect = dashEffect)

    // Labels
    for ((level, y) in listOf("70" to ob70Y, "30" to os30Y)) {
        val text = textMeasurer.measure(level, TextStyle(color = Color(0xFF555555), fontSize = 7.sp, fontFamily = FontFamily.Monospace))
        drawText(text, topLeft = Offset(chartWidth + 2.dp.toPx(), y - text.size.height / 2f))
    }

    // RSI line
    val path = Path()
    var started = false
    for (i in startIndex until minOf(endIndex, values.size)) {
        val value = values[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = drawTop + drawHeight * (1 - value / 100.0).toFloat()
        if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
    }
    if (started) drawPath(path, color, style = Stroke(width = 1.dp.toPx()))
}

private fun DrawScope.drawMACDPanel(
    result: MACDResult,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    config: IndicatorConfig
) {
    val padding = 4.dp.toPx()
    val drawHeight = panelHeight - padding * 2
    val drawTop = panelTop + padding

    // Find min/max for visible range
    var minVal = Double.MAX_VALUE
    var maxVal = Double.MIN_VALUE
    for (i in startIndex until minOf(endIndex, result.macdLine.size)) {
        result.macdLine[i]?.let { minVal = minOf(minVal, it); maxVal = maxOf(maxVal, it) }
        result.signalLine[i]?.let { minVal = minOf(minVal, it); maxVal = maxOf(maxVal, it) }
        result.histogram[i]?.let { minVal = minOf(minVal, it); maxVal = maxOf(maxVal, it) }
    }
    if (minVal >= maxVal) return
    val range = maxVal - minVal

    // Zero line
    val zeroY = drawTop + drawHeight * (1 - (0 - minVal) / range).toFloat()
    drawLine(Color(0xFF333333), Offset(0f, zeroY), Offset(chartWidth, zeroY), strokeWidth = 0.5.dp.toPx())

    // Histogram bars
    val barWidth = candleWidth * 0.5f
    for (i in startIndex until minOf(endIndex, result.histogram.size)) {
        val value = result.histogram[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + (candleWidth - barWidth) / 2f
        val barY = drawTop + drawHeight * (1 - (value - minVal) / range).toFloat()

        val color = if (value >= 0) Color(0xFF00E676).copy(alpha = 0.5f) else Color(0xFFFF1744).copy(alpha = 0.5f)
        val top = minOf(barY, zeroY)
        val height = kotlin.math.abs(barY - zeroY)
        drawRect(color, topLeft = Offset(x, top), size = Size(barWidth, height))
    }

    // MACD and Signal lines
    drawPanelLine(result.macdLine, startIndex, endIndex, candleWidth, drawTop, drawHeight, minVal, range, Color((config.color and 0xFFFFFFFFL).toInt()))
    drawPanelLine(result.signalLine, startIndex, endIndex, candleWidth, drawTop, drawHeight, minVal, range, Color((config.tertiaryColor and 0xFFFFFFFFL).toInt()))
}

private fun DrawScope.drawStochasticPanel(
    result: StochasticResult,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    config: IndicatorConfig,
    textMeasurer: TextMeasurer
) {
    val padding = 4.dp.toPx()
    val drawHeight = panelHeight - padding * 2
    val drawTop = panelTop + padding

    // 80/20 levels
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
    val y80 = drawTop + drawHeight * (1 - 80.0 / 100.0).toFloat()
    val y20 = drawTop + drawHeight * (1 - 20.0 / 100.0).toFloat()
    drawLine(Color(0xFFFF1744).copy(alpha = 0.3f), Offset(0f, y80), Offset(chartWidth, y80), strokeWidth = 0.5.dp.toPx(), pathEffect = dashEffect)
    drawLine(Color(0xFF00E676).copy(alpha = 0.3f), Offset(0f, y20), Offset(chartWidth, y20), strokeWidth = 0.5.dp.toPx(), pathEffect = dashEffect)

    // %K and %D lines
    drawPanelLine(result.kLine, startIndex, endIndex, candleWidth, drawTop, drawHeight, 0.0, 100.0, Color((config.color and 0xFFFFFFFFL).toInt()))
    drawPanelLine(result.dLine, startIndex, endIndex, candleWidth, drawTop, drawHeight, 0.0, 100.0, Color((config.secondaryColor and 0xFFFFFFFFL).toInt()))
}

private fun DrawScope.drawGenericLinePanel(
    values: List<Double?>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    color: Color
) {
    val padding = 4.dp.toPx()
    val drawHeight = panelHeight - padding * 2
    val drawTop = panelTop + padding

    var minVal = Double.MAX_VALUE
    var maxVal = Double.MIN_VALUE
    for (i in startIndex until minOf(endIndex, values.size)) {
        values[i]?.let { minVal = minOf(minVal, it); maxVal = maxOf(maxVal, it) }
    }
    if (minVal >= maxVal) return
    drawPanelLine(values, startIndex, endIndex, candleWidth, drawTop, drawHeight, minVal, maxVal - minVal, color)
}

private fun DrawScope.drawPanelLine(
    values: List<Double?>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    drawTop: Float,
    drawHeight: Float,
    minVal: Double,
    range: Double,
    color: Color
) {
    val path = Path()
    var started = false
    for (i in startIndex until minOf(endIndex, values.size)) {
        val value = values[i] ?: continue
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        val y = drawTop + drawHeight * (1 - (value - minVal) / range).toFloat()
        if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
    }
    if (started) drawPath(path, color, style = Stroke(width = 1.dp.toPx()))
}
