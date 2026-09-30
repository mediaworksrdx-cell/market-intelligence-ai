package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import java.util.Locale

/**
 * Draws the real-time horizontal price guideline and high-visibility right-axis badge.
 */
fun DrawScope.drawCurrentPriceLineLayer(
    candles: List<Candle>,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer,
    @Suppress("UNUSED_PARAMETER") timeframe: String = "1D",
    currentPriceOverride: Double? = null
) {
    if (candles.isEmpty()) return
    val currentCandle = candles.last()
    val currentPrice = if (currentPriceOverride != null && currentPriceOverride > 0.0) currentPriceOverride else currentCandle.close
    val priceRange = priceMax - priceMin
    if (priceRange <= 0.0 || chartAreaHeight < 4f) return

    val chartWidth = size.width - rightMargin
    if (chartWidth <= 0f) return

    val isBullish = currentPrice >= currentCandle.open
    val accentColor = if (isBullish) Color(0xFF00E676) else Color(0xFFFF1744)

    // Calculate Y on canvas (inverted: higher price = lower y)
    val rawY = chartAreaHeight - ((currentPrice - priceMin) / priceRange * chartAreaHeight).toFloat()
    val clampedY = rawY.coerceIn(2f, maxOf(2f, chartAreaHeight - 2f))

    // 1. Soft glow halo line across the chart
    drawLine(
        color = accentColor.copy(alpha = 0.28f),
        start = Offset(0f, clampedY),
        end = Offset(chartWidth, clampedY),
        strokeWidth = 3.5.dp.toPx()
    )

    // 2. Core horizontal dashed guideline across the chart
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    drawLine(
        color = accentColor,
        start = Offset(0f, clampedY),
        end = Offset(chartWidth, clampedY),
        strokeWidth = 1.6.dp.toPx(),
        pathEffect = dashEffect
    )

    // 3. Right-Axis High-Visibility Pill Badge
    val priceStr = formatPrice(currentPrice)
    val textColor = if (isBullish) Color.Black else Color.White
    val priceTextStyle = TextStyle(
        color = textColor,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace
    )
    val priceMeasured = textMeasurer.measure(priceStr, priceTextStyle)

    val badgePaddingH = 5.dp.toPx()
    val badgePaddingV = 3.dp.toPx()
    val badgeWidth = priceMeasured.size.width + (badgePaddingH * 2) + 8.dp.toPx() // extra for live dot
    val badgeHeight = priceMeasured.size.height + (badgePaddingV * 2)

    val badgeLeft = chartWidth + 2.dp.toPx()
    val badgeTop = clampedY - badgeHeight / 2f

    // Draw badge background pill in vibrant accent color
    val pillPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = badgeLeft,
                top = badgeTop,
                right = badgeLeft + badgeWidth,
                bottom = badgeTop + badgeHeight,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        )
    }
    drawPath(pillPath, accentColor)

    // Live beacon dot
    val dotRadius = 2.dp.toPx()
    val dotCenter = Offset(badgeLeft + badgePaddingH + dotRadius, clampedY)
    drawCircle(color = textColor, radius = dotRadius, center = dotCenter)

    // Draw Price Text
    drawText(
        textLayoutResult = priceMeasured,
        topLeft = Offset(badgeLeft + badgePaddingH + dotRadius * 2 + 3.dp.toPx(), badgeTop + badgePaddingV)
    )
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        price >= 0.01 -> String.format(Locale.US, "%.4f", price)
        price < 0.001 -> String.format(Locale.US, "%.7f", price)
        else -> String.format(Locale.US, "%.6f", price)
    }
}
