package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import com.example.tradeengine.models.Candle
import java.text.SimpleDateFormat
import java.util.*

/**
 * Draws the background grid lines and axis labels.
 */
fun DrawScope.drawGridLayer(
    candles: List<Candle>,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    bottomMargin: Float,
    textMeasurer: TextMeasurer,
    timeframe: String = "1D"
) {
    val gridColor = Color(0xFF1A1A1A)
    val textColor = Color(0xFF666666)
    val priceRange = priceMax - priceMin
    if (priceRange <= 0) return

    val chartWidth = size.width - rightMargin
    val chartHeight = chartAreaHeight

    // ── Horizontal grid lines (price levels) ──
    val numHorizontalLines = 5
    val priceStep = priceRange / numHorizontalLines

    for (i in 0..numHorizontalLines) {
        val price = priceMin + i * priceStep
        val y = chartHeight - ((price - priceMin) / priceRange * chartHeight).toFloat()

        // Grid line
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 0.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        )

        // Price label on right axis
        val priceText = formatPrice(price)
        val textResult = textMeasurer.measure(
            priceText,
            TextStyle(
                color = textColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        )
        drawText(
            textResult,
            topLeft = Offset(chartWidth + 4.dp.toPx(), y - textResult.size.height / 2f)
        )
    }

    // ── Vertical grid lines (time / date labels) ──
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0 || candles.isEmpty()) return

    val candleWidth = chartWidth / visibleCount
    val timeStep = maxOf(1, visibleCount / 5)

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

    val calFirst = Calendar.getInstance().apply { timeInMillis = firstMs }
    val calLast = Calendar.getInstance().apply { timeInMillis = lastMs }
    val spansMultipleYears = calFirst.get(Calendar.YEAR) != calLast.get(Calendar.YEAR)

    val dateFormat = if (spansMultipleYears) {
        SimpleDateFormat("dd MMM ''yy", Locale.US)
    } else {
        SimpleDateFormat("dd MMM", Locale.US)
    }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    val totalSpanMs = kotlin.math.abs(lastMs - firstMs)
    val isMultiDayIntraday = !isDailyOrHigher && totalSpanMs > 24 * 3600 * 1000L

    var prevDayOfYear = -1

    for (i in visibleStartIndex until visibleEndIndex step timeStep) {
        val localIndex = i - visibleStartIndex
        val x = localIndex * candleWidth + candleWidth / 2

        // Grid line
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = 0.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        )

        // Time / Date label at bottom
        if (i < candles.size) {
            val rawTime = candles[i].openTime
            val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
            val date = Date(timeMs)
            val currentCal = Calendar.getInstance().apply { time = date }
            val currentDayOfYear = currentCal.get(Calendar.DAY_OF_YEAR)

            val label = when {
                isDailyOrHigher -> {
                    dateFormat.format(date)
                }
                isMultiDayIntraday -> {
                    if (prevDayOfYear != currentDayOfYear) {
                        dateFormat.format(date)
                    } else {
                        timeFormat.format(date)
                    }
                }
                else -> {
                    timeFormat.format(date)
                }
            }
            prevDayOfYear = currentDayOfYear

            val textResult = textMeasurer.measure(
                label,
                TextStyle(
                    color = textColor,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            drawText(
                textResult,
                topLeft = Offset(x - textResult.size.width / 2f, chartHeight + 4.dp.toPx())
            )
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 10000 -> "%.0f".format(price)
        price >= 100 -> "%.1f".format(price)
        price >= 1 -> "%.2f".format(price)
        else -> "%.4f".format(price)
    }
}
