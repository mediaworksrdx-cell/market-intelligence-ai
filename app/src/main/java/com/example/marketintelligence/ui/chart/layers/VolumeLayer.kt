package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
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

    // Find max volume in visible range for scaling
    var maxVolume = 0.0
    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        if (candles[i].volume > maxVolume) maxVolume = candles[i].volume
    }
    if (maxVolume <= 0) return

    val volumeTop = chartAreaHeight - volumeAreaHeight

    for (i in visibleStartIndex until minOf(visibleEndIndex, candles.size)) {
        val localIndex = i - visibleStartIndex
        val x = localIndex * candleWidth + (candleWidth - bodyWidth) / 2
        val candle = candles[i]
        val isBullish = candle.close >= candle.open

        val barHeight = (candle.volume / maxVolume * volumeAreaHeight).toFloat()
        val barTop = chartAreaHeight - barHeight

        val color = if (isBullish) {
            Color(0xFF00E676).copy(alpha = 0.25f)
        } else {
            Color(0xFFFF1744).copy(alpha = 0.25f)
        }

        drawRect(
            color = color,
            topLeft = Offset(x, barTop),
            size = Size(bodyWidth, barHeight)
        )
    }
}
