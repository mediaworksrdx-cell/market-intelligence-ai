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
import com.example.marketintelligence.domain.chart.FnoOverlayLevels
import com.example.marketintelligence.domain.chart.StrategyPayoffOverlay

/**
 * Draws institutional F&O levels (Call Wall, Put Wall, Gamma Flip, Max Pain)
 * and active option strategy payoff bands & breakevens directly on the price chart.
 */
fun DrawScope.drawFnoOverlayLayer(
    fnoLevels: FnoOverlayLevels?,
    strategyOverlay: StrategyPayoffOverlay?,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer
) {
    val priceRange = priceMax - priceMin
    if (priceRange <= 0.0) return
    val chartWidth = size.width - rightMargin

    fun priceToY(price: Double): Float {
        return (chartAreaHeight * (1.0 - (price - priceMin) / priceRange)).toFloat()
    }

    // ── 1. Strategy Payoff Bands ──
    if (strategyOverlay != null && strategyOverlay.enabled) {
        // Profit Zone fill
        strategyOverlay.maxProfitZone?.let { range ->
            val startP = range.start.coerceIn(priceMin, priceMax)
            val endP = range.endInclusive.coerceIn(priceMin, priceMax)
            val y1 = priceToY(startP)
            val y2 = priceToY(endP)
            val topY = minOf(y1, y2)
            val bandHeight = maxOf(kotlin.math.abs(y1 - y2), 2f)

            drawRect(
                color = Color(0xFF00E676).copy(alpha = 0.12f),
                topLeft = Offset(0f, topY),
                size = Size(chartWidth, bandHeight)
            )
            val pText = textMeasurer.measure(
                "MAX PROFIT ZONE",
                TextStyle(color = Color(0xFF00E676).copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(pText, topLeft = Offset(chartWidth - pText.size.width - 8.dp.toPx(), topY + 4.dp.toPx()))
        }

        // Loss Zone fill
        strategyOverlay.maxLossZone?.let { range ->
            val startP = range.start.coerceIn(priceMin, priceMax)
            val endP = range.endInclusive.coerceIn(priceMin, priceMax)
            val y1 = priceToY(startP)
            val y2 = priceToY(endP)
            val topY = minOf(y1, y2)
            val bandHeight = maxOf(kotlin.math.abs(y1 - y2), 2f)

            drawRect(
                color = Color(0xFFFF1744).copy(alpha = 0.12f),
                topLeft = Offset(0f, topY),
                size = Size(chartWidth, bandHeight)
            )
            val lText = textMeasurer.measure(
                "MAX LOSS ZONE",
                TextStyle(color = Color(0xFFFF1744).copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(lText, topLeft = Offset(chartWidth - lText.size.width - 8.dp.toPx(), topY + 4.dp.toPx()))
        }

        // Breakeven Dotted Lines
        val dotEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        for (be in strategyOverlay.breakevenPoints) {
            if (be in priceMin..priceMax) {
                val beY = priceToY(be)
                drawLine(
                    color = Color(0xFFFFEB3B),
                    start = Offset(0f, beY),
                    end = Offset(chartWidth, beY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dotEffect
                )
                val beText = textMeasurer.measure(
                    "BE: %.1f".format(be),
                    TextStyle(color = Color(0xFFFFEB3B), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                )
                // Draw badge background
                drawRect(
                    color = Color(0xFF1E1E1E),
                    topLeft = Offset(chartWidth - beText.size.width - 6.dp.toPx(), beY - beText.size.height / 2f),
                    size = Size(beText.size.width.toFloat() + 4.dp.toPx(), beText.size.height.toFloat())
                )
                drawText(beText, topLeft = Offset(chartWidth - beText.size.width - 4.dp.toPx(), beY - beText.size.height / 2f))
            }
        }

        // Strategy Watermark
        if (strategyOverlay.strategyName.isNotBlank()) {
            val stratText = textMeasurer.measure(
                "STRATEGY: ${strategyOverlay.strategyName.uppercase()}",
                TextStyle(color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(stratText, topLeft = Offset(8.dp.toPx(), 24.dp.toPx()))
        }
    }

    // ── 2. Institutional F&O Levels ──
    if (fnoLevels == null || !fnoLevels.enabled) return

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))

    // Helper for drawing tagged level
    fun drawLevel(price: Double?, color: Color, label: String, gexVal: Double? = null) {
        if (price == null || price !in priceMin..priceMax) return
        val y = priceToY(price)

        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = dashEffect
        )

        val fullLabel = if (gexVal != null) {
            val gexFormatted = if (kotlin.math.abs(gexVal) >= 1e9) {
                "%.1fB".format(gexVal / 1e9)
            } else if (kotlin.math.abs(gexVal) >= 1e6) {
                "%.1fM".format(gexVal / 1e6)
            } else {
                "%.0f".format(gexVal)
            }
            "$label: %.0f ($gexFormatted GEX)".format(price)
        } else {
            "$label: %.0f".format(price)
        }

        val textLayout = textMeasurer.measure(
            fullLabel,
            TextStyle(color = color, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
        )
        val badgeX = chartWidth - textLayout.size.width - 8.dp.toPx()
        val badgeY = y - textLayout.size.height - 2.dp.toPx()

        drawRect(
            color = Color(0xFF12141A).copy(alpha = 0.9f),
            topLeft = Offset(badgeX - 2.dp.toPx(), badgeY),
            size = Size(textLayout.size.width.toFloat() + 4.dp.toPx(), textLayout.size.height.toFloat() + 2.dp.toPx())
        )
        drawText(textLayout, topLeft = Offset(badgeX, badgeY + 1.dp.toPx()))
    }

    // Call Wall (Red)
    drawLevel(fnoLevels.callWall, Color(0xFFFF1744), "CW", fnoLevels.callWallGex)
    // Put Wall (Green)
    drawLevel(fnoLevels.putWall, Color(0xFF00E676), "PW", fnoLevels.putWallGex)
    // Gamma Flip (Gold)
    drawLevel(fnoLevels.gammaFlip, Color(0xFFFFD600), "γ-Flip")
    // Max Pain (Purple)
    drawLevel(fnoLevels.maxPain, Color(0xFFAB47BC), "Max Pain")
}
