package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tradeengine.models.Candle
import java.text.SimpleDateFormat
import java.util.*

/**
 * Draws an enhanced crosshair with OHLCV tooltip and price/time labels on the axes.
 */
fun DrawScope.drawCrosshairLayer(
    position: Offset?,
    candles: List<Candle>,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer
) {
    if (position == null) return
    val chartWidth = size.width - rightMargin
    val priceRange = priceMax - priceMin
    if (priceRange <= 0) return

    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0) return

    val crosshairColor = Color(0xFFAAAAAA).copy(alpha = 0.5f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))

    // Clamp position to chart area
    val clampedX = position.x.coerceIn(0f, chartWidth)
    val clampedY = position.y.coerceIn(0f, chartAreaHeight)

    // ── Crosshair lines ──
    drawLine(crosshairColor, Offset(clampedX, 0f), Offset(clampedX, chartAreaHeight), strokeWidth = 0.8.dp.toPx(), pathEffect = dashEffect)
    drawLine(crosshairColor, Offset(0f, clampedY), Offset(chartWidth, clampedY), strokeWidth = 0.8.dp.toPx(), pathEffect = dashEffect)

    // ── Price label on right axis ──
    val price = priceMax - (clampedY / chartAreaHeight) * priceRange
    val priceText = formatCrosshairPrice(price)
    val priceLabelResult = textMeasurer.measure(
        priceText,
        TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    )
    val priceLabelBgWidth = priceLabelResult.size.width + 8.dp.toPx()
    val priceLabelBgHeight = priceLabelResult.size.height + 4.dp.toPx()

    drawRect(
        Color(0xFF333333),
        topLeft = Offset(chartWidth, clampedY - priceLabelBgHeight / 2),
        size = Size(priceLabelBgWidth, priceLabelBgHeight)
    )
    drawText(
        priceLabelResult,
        topLeft = Offset(chartWidth + 4.dp.toPx(), clampedY - priceLabelResult.size.height / 2f)
    )

    // ── OHLCV Tooltip ──
    val candleWidth = chartWidth / visibleCount
    val candleIndex = visibleStartIndex + (clampedX / candleWidth).toInt()

    if (candleIndex in candles.indices) {
        val candle = candles[candleIndex]
        val isBullish = candle.close >= candle.open
        val tooltipColor = if (isBullish) Color(0xFF00E676) else Color(0xFFFF1744)

        val timeFormat = SimpleDateFormat("dd MMM HH:mm", Locale.US)
        val timeStr = timeFormat.format(Date(candle.openTime))

        val tooltipLines = listOf(
            timeStr,
            "O: ${formatCrosshairPrice(candle.open)}",
            "H: ${formatCrosshairPrice(candle.high)}",
            "L: ${formatCrosshairPrice(candle.low)}",
            "C: ${formatCrosshairPrice(candle.close)}",
            "V: ${formatVolume(candle.volume)}"
        )

        val tooltipStyle = TextStyle(
            color = Color.White,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        val measuredLines = tooltipLines.map { textMeasurer.measure(it, tooltipStyle) }
        val maxWidth = measuredLines.maxOf { it.size.width }
        val totalHeight = measuredLines.sumOf { it.size.height }

        val padding = 6.dp.toPx()
        val tooltipWidth = maxWidth + padding * 2
        val tooltipHeight = totalHeight + padding * 2 + (tooltipLines.size - 1) * 2.dp.toPx()

        // Position tooltip near crosshair but avoid edge clipping
        val tooltipX = if (clampedX + tooltipWidth + 16.dp.toPx() > chartWidth) {
            clampedX - tooltipWidth - 8.dp.toPx()
        } else {
            clampedX + 8.dp.toPx()
        }
        val tooltipY = maxOf(0f, minOf(clampedY - tooltipHeight / 2, chartAreaHeight - tooltipHeight))

        // Background
        drawRect(
            Color(0xDD111111),
            topLeft = Offset(tooltipX, tooltipY),
            size = Size(tooltipWidth, tooltipHeight)
        )

        // Left accent bar
        drawRect(
            tooltipColor,
            topLeft = Offset(tooltipX, tooltipY),
            size = Size(2.dp.toPx(), tooltipHeight)
        )

        // Text lines
        var currentY = tooltipY + padding
        for (line in measuredLines) {
            drawText(line, topLeft = Offset(tooltipX + padding, currentY))
            currentY += line.size.height + 2.dp.toPx()
        }
    }

    // ── Time label on bottom axis ──
    if (candleIndex in candles.indices) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val timeStr = timeFormat.format(Date(candles[candleIndex].openTime))
        val timeLabelResult = textMeasurer.measure(
            timeStr,
            TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
        val timeLabelBgWidth = timeLabelResult.size.width + 8.dp.toPx()
        val timeLabelBgHeight = timeLabelResult.size.height + 4.dp.toPx()
        val timeLabelX = (clampedX - timeLabelBgWidth / 2).coerceIn(0f, chartWidth - timeLabelBgWidth)

        drawRect(
            Color(0xFF333333),
            topLeft = Offset(timeLabelX, chartAreaHeight),
            size = Size(timeLabelBgWidth, timeLabelBgHeight)
        )
        drawText(
            timeLabelResult,
            topLeft = Offset(timeLabelX + 4.dp.toPx(), chartAreaHeight + 2.dp.toPx())
        )
    }
}

private fun formatCrosshairPrice(price: Double): String {
    return when {
        price >= 10000 -> "%.0f".format(price)
        price >= 100 -> "%.1f".format(price)
        price >= 1 -> "%.2f".format(price)
        else -> "%.4f".format(price)
    }
}

private fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
        volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
        volume >= 1_000 -> "%.1fK".format(volume / 1_000)
        else -> "%.0f".format(volume)
    }
}
