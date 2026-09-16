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
    textMeasurer: TextMeasurer,
    timeframe: String = "1D"
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

    // ── Time / Date label on bottom axis ──
    val candleWidth = chartWidth / visibleCount
    val candleIndex = visibleStartIndex + (clampedX / candleWidth).toInt()

    if (candleIndex in candles.indices) {
        val rawTime = candles[candleIndex].openTime
        val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
        val date = Date(timeMs)

        val firstRaw = candles.firstOrNull()?.openTime ?: 0L
        val lastRaw = candles.lastOrNull()?.openTime ?: 0L
        val firstMs = if (firstRaw in 1..99_999_999_999L) firstRaw * 1000L else firstRaw
        val lastMs = if (lastRaw in 1..99_999_999_999L) lastRaw * 1000L else lastRaw
        val avgCandleDurationMs = if (candles.size > 1) {
            kotlin.math.abs(lastMs - firstMs) / (candles.size - 1).coerceAtLeast(1)
        } else 86_400_000L

        val isDailyOrHigher = timeframe.equals("1D", ignoreCase = true) ||
            timeframe.equals("D", ignoreCase = true) ||
            timeframe.equals("1W", ignoreCase = true) ||
            timeframe.equals("W", ignoreCase = true) ||
            timeframe.equals("1M", ignoreCase = true) ||
            timeframe.equals("M", ignoreCase = true) ||
            timeframe.contains("day", ignoreCase = true) ||
            timeframe.contains("week", ignoreCase = true) ||
            timeframe.contains("month", ignoreCase = true) ||
            avgCandleDurationMs >= 20 * 3600 * 1000L

        val timeStr = if (isDailyOrHigher) {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
        } else {
            SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(date)
        }

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

internal fun formatCrosshairPrice(price: Double): String {
    return when {
        price >= 10000 -> "%.0f".format(price)
        price >= 100 -> "%.1f".format(price)
        price >= 1 -> "%.2f".format(price)
        else -> "%.4f".format(price)
    }
}

internal fun formatVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
        volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
        volume >= 1_000 -> "%.1fK".format(volume / 1_000)
        volume >= 10 -> "%.1f".format(volume)
        volume >= 1 -> "%.2f".format(volume)
        volume > 0 -> "%.3f".format(volume)
        else -> "0"
    }
}

