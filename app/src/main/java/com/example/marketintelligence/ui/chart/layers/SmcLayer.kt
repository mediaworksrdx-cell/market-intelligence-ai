package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.SmcAnalysis
import kotlin.math.abs

/**
 * Draws Smart Money Concepts (SMC) elements:
 * - Fair Value Gaps (FVG) as shaded imbalance boxes across the relevant candle range.
 * - Liquidity Sweeps (BSL/SSL) with markers at false breakout swing points.
 */
fun DrawScope.drawSmcLayer(
    smcAnalysis: SmcAnalysis,
    startIndex: Int,
    endIndex: Int,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer
) {
    val visibleCount = endIndex - startIndex
    if (visibleCount <= 0) return
    val chartWidth = size.width - rightMargin
    val candleWidth = chartWidth / visibleCount
    val priceRange = priceMax - priceMin
    if (priceRange <= 0.0) return

    fun priceToY(price: Double): Float {
        return (chartAreaHeight * (1.0 - (price - priceMin) / priceRange)).toFloat()
    }

    // ── 1. Draw Fair Value Gaps (FVGs) ──
    for (fvg in smcAnalysis.fvgs) {
        // Check if FVG overlaps visible candle range
        if (fvg.endIndex < startIndex || fvg.startIndex > endIndex) continue

        val localStart = (fvg.startIndex - startIndex).coerceAtLeast(0)
        val localEnd = (fvg.endIndex - startIndex).coerceAtMost(visibleCount)

        val leftX = localStart * candleWidth
        val rightX = localEnd * candleWidth + candleWidth
        val boxWidth = (rightX - leftX).coerceAtLeast(candleWidth)

        val yTop = priceToY(fvg.topPrice)
        val yBottom = priceToY(fvg.bottomPrice)
        val boxTop = minOf(yTop, yBottom)
        val boxHeight = maxOf(abs(yTop - yBottom), 2f)

        val baseColor = if (fvg.isBullish) Color(0xFF00E676) else Color(0xFFFF5252)
        val alpha = if (fvg.isMitigated) 0.05f else 0.18f

        // Shaded FVG box
        drawRect(
            color = baseColor.copy(alpha = alpha),
            topLeft = Offset(leftX, boxTop),
            size = Size(boxWidth, boxHeight)
        )

        // FVG border
        drawRect(
            color = baseColor.copy(alpha = if (fvg.isMitigated) 0.15f else 0.45f),
            topLeft = Offset(leftX, boxTop),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = 0.8.dp.toPx())
        )

        // FVG badge
        if (!fvg.isMitigated && boxWidth > 20.dp.toPx()) {
            val label = if (fvg.isBullish) "+FVG" else "-FVG"
            val textLayout = textMeasurer.measure(
                label,
                TextStyle(color = baseColor.copy(alpha = 0.8f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(textLayout, topLeft = Offset(leftX + 2.dp.toPx(), boxTop + 1.dp.toPx()))
        }
    }

    // ── 2. Draw Liquidity Sweeps (BSL / SSL) ──
    for (sweep in smcAnalysis.sweeps) {
        if (sweep.candleIndex in startIndex until endIndex) {
            val localIndex = sweep.candleIndex - startIndex
            val x = localIndex * candleWidth + candleWidth / 2f
            val y = priceToY(sweep.price)

            val sweepColor = if (sweep.isHighSweep) Color(0xFFFF1744) else Color(0xFF00E676)
            val sweepText = textMeasurer.measure(
                if (sweep.isHighSweep) "▼ BSL" else "▲ SSL",
                TextStyle(color = sweepColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
            )

            val textX = x - sweepText.size.width / 2f
            val textY = if (sweep.isHighSweep) y - sweepText.size.height - 3.dp.toPx() else y + 2.dp.toPx()

            drawText(sweepText, topLeft = Offset(textX, textY))
        }
    }
}
