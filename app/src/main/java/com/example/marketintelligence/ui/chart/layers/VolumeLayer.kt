package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradeengine.models.Candle

/**
 * Draws volume bars at the bottom portion of the chart area.
 * Volume bars use a semi-transparent gradient based on candle direction.
 */
fun DrawScope.drawVolumeLayer(
    candles: List<Candle>,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    chartAreaHeight: Float,
    rightMargin: Float,
    volumeAreaHeight: Float
) {
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0) return

    val chartWidth = size.width - rightMargin
    val candleWidth = chartWidth / visibleCount
    val bodyWidth = candleWidth * 0.7f

    // Find min and max volume in visible range for scaling
    var minVolume = Double.MAX_VALUE
    var maxVolume = 0.0
    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        val v = candles[i].volume
        if (v > maxVolume) maxVolume = v
        if (v < minVolume) minVolume = v
    }
    if (maxVolume <= 0) return

    val volumeRange = maxVolume - minVolume
    val hasZeroVariance = volumeRange < 1e-4
    val effectiveMaxBarHeight = volumeAreaHeight * 0.90f

    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        val localIndex = i - visibleStartIndex
        val x = localIndex * candleWidth + (candleWidth - bodyWidth) / 2
        val candle = candles[i]
        val isBullish = candle.close >= candle.open

        val normalizedRatio = if (hasZeroVariance) {
            0.30f
        } else {
            (candle.volume / maxVolume).toFloat().coerceIn(0.08f, 1.0f)
        }

        val barHeight = normalizedRatio * effectiveMaxBarHeight
        val barTop = chartAreaHeight - barHeight

        val color = if (isBullish) {
            Color(0xFF00E676).copy(alpha = 0.35f)
        } else {
            Color(0xFFFF1744).copy(alpha = 0.35f)
        }

        drawRect(
            color = color,
            topLeft = Offset(x, barTop),
            size = Size(bodyWidth, barHeight)
        )
    }
}

/**
 * Draws volume bars in a dedicated separate panel region below the candle chart.
 * Features its own top separator line, "Vol" label with current candle volume,
 * distinct green/red histogram bars, and right-axis max/0 scale.
 */
fun DrawScope.drawVolumePanelLayer(
    candles: List<Candle>,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    panelTop: Float,
    panelHeight: Float,
    chartWidth: Float,
    rightMargin: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0 || panelHeight <= 0f) return

    val candleWidth = chartWidth / visibleCount
    val bodyWidth = (candleWidth * 0.72f).coerceAtLeast(1f)

    // 0. Panel Background (prevents upper chart layers from leaking into the volume panel)
    drawRect(
        color = Color(0xFF0F1117),
        topLeft = Offset(0f, panelTop),
        size = Size(chartWidth + rightMargin, panelHeight)
    )

    // 1. Panel Separator Line
    drawLine(
        color = Color(0xFF2A2E39),
        start = Offset(0f, panelTop),
        end = Offset(chartWidth + rightMargin, panelTop),
        strokeWidth = 1.dp.toPx()
    )

    // 2. Find max volume in visible range
    var maxVolume = 0.0
    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        val v = candles[i].volume
        if (v > maxVolume) maxVolume = v
    }
    val hasVolume = maxVolume > 0.0
    val effectiveMaxVolume = if (hasVolume) maxVolume else 1.0

    // 3. Panel Label at top-left
    val lastCandle = candles.getOrNull(minOf(visibleEndIndex - 1, candles.size - 1))
    val volText = if (lastCandle != null && lastCandle.volume > 0.0) "Vol ${formatVolume(lastCandle.volume)}" else "Vol"
    val labelResult = textMeasurer.measure(
        volText,
        androidx.compose.ui.text.TextStyle(
            color = Color(0xFF00E5FF),
            fontSize = 8.5.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    )
    drawText(labelResult, topLeft = Offset(4.dp.toPx(), panelTop + 2.dp.toPx()))

    // 4. Right Y-Axis Scale
    if (hasVolume) {
        val maxVolStr = formatVolume(maxVolume)
        val maxVolResult = textMeasurer.measure(
            maxVolStr,
            androidx.compose.ui.text.TextStyle(color = Color(0xFF78909C), fontSize = 7.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        )
        drawText(maxVolResult, topLeft = Offset(chartWidth + 3.dp.toPx(), panelTop + 2.dp.toPx()))

        val zeroResult = textMeasurer.measure(
            "0",
            androidx.compose.ui.text.TextStyle(color = Color(0xFF78909C), fontSize = 7.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        )
        drawText(zeroResult, topLeft = Offset(chartWidth + 3.dp.toPx(), panelTop + panelHeight - 11.dp.toPx()))
    }

    // 5. Volume Histogram Bars
    if (!hasVolume) return
    val topPadding = 12.dp.toPx()
    val availableBarHeight = (panelHeight - topPadding - 2.dp.toPx()).coerceAtLeast(1f)

    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        val localIndex = i - visibleStartIndex
        val x = localIndex * candleWidth + (candleWidth - bodyWidth) / 2f
        val candle = candles[i]
        val isBullish = candle.close >= candle.open

        val barHeight = ((candle.volume / effectiveMaxVolume) * availableBarHeight).toFloat().coerceIn(1.5f, availableBarHeight)
        val barTop = panelTop + panelHeight - barHeight

        val color = if (isBullish) {
            Color(0xFF00E676).copy(alpha = 0.70f)
        } else {
            Color(0xFFFF1744).copy(alpha = 0.70f)
        }

        drawRect(
            color = color,
            topLeft = Offset(x, barTop),
            size = Size(bodyWidth, barHeight)
        )
    }
}
