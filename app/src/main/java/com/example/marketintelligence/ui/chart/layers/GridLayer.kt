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
    textMeasurer: TextMeasurer
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

    // ── Vertical grid lines (time labels) ──
    val visibleCount = visibleEndIndex - visibleStartIndex
    if (visibleCount <= 0 || candles.isEmpty()) return

    val candleWidth = chartWidth / visibleCount
    val timeStep = maxOf(1, visibleCount / 5)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val dateFormat = SimpleDateFormat("dd MMM", Locale.US)

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

        // Time label at bottom
        if (i < candles.size) {
            val time = Date(candles[i].openTime)
            val label = timeFormat.format(time)
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
