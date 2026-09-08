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
import com.example.marketintelligence.domain.chart.VolumeProfileData
import kotlin.math.abs

/**
 * Draws the Visible Range Volume Profile (VPVR) horizontally on the chart canvas.
 * Highlights the 70% Value Area (VAH to VAL) and marks the Point of Control (POC).
 */
fun DrawScope.drawVolumeProfileLayer(
    data: VolumeProfileData,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    textMeasurer: TextMeasurer,
    profileWidthFraction: Float = 0.22f
) {
    if (data.buckets.isEmpty() || data.maxBucketVolume <= 0.0) return
    val priceRange = priceMax - priceMin
    if (priceRange <= 0.0) return

    val chartWidth = size.width - rightMargin
    val maxBarWidth = chartWidth * profileWidthFraction

    fun priceToY(price: Double): Float {
        return (chartAreaHeight * (1.0 - (price - priceMin) / priceRange)).toFloat()
    }

    // 1. Draw Profile Histogram Bars
    for (bucket in data.buckets) {
        val yTop = priceToY(bucket.priceHigh)
        val yBottom = priceToY(bucket.priceLow)
        val barHeight = maxOf(abs(yBottom - yTop) - 0.5f, 1f)
        val y = minOf(yTop, yBottom)

        val inValueArea = bucket.priceLow >= data.valPrice && bucket.priceHigh <= data.vahPrice
        val alphaMultiplier = if (inValueArea) 1.0f else 0.4f

        val totalVolFrac = (bucket.totalVolume / data.maxBucketVolume).toFloat().coerceIn(0f, 1f)
        val barTotalWidth = maxBarWidth * totalVolFrac

        val buyRatio = if (bucket.totalVolume > 0) (bucket.buyVolume / bucket.totalVolume).toFloat() else 0.5f
        val buyWidth = barTotalWidth * buyRatio
        val sellWidth = barTotalWidth - buyWidth

        // Buy volume bar (green)
        drawRect(
            color = Color(0xFF00E676).copy(alpha = 0.45f * alphaMultiplier),
            topLeft = Offset(0f, y),
            size = Size(buyWidth, barHeight)
        )
        // Sell volume bar (red)
        drawRect(
            color = Color(0xFFFF1744).copy(alpha = 0.45f * alphaMultiplier),
            topLeft = Offset(buyWidth, y),
            size = Size(sellWidth, barHeight)
        )
    }

    // 2. Draw Value Area High (VAH) dashed line
    if (data.vahPrice in priceMin..priceMax) {
        val vahY = priceToY(data.vahPrice)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(
            color = Color(0xFF42A5F5).copy(alpha = 0.7f),
            start = Offset(0f, vahY),
            end = Offset(chartWidth, vahY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash
        )
        val vahText = textMeasurer.measure(
            "VAH: %.2f".format(data.vahPrice),
            TextStyle(color = Color(0xFF42A5F5), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        drawText(vahText, topLeft = Offset(chartWidth - vahText.size.width - 4.dp.toPx(), vahY - vahText.size.height))
    }

    // 3. Draw Value Area Low (VAL) dashed line
    if (data.valPrice in priceMin..priceMax) {
        val valY = priceToY(data.valPrice)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(
            color = Color(0xFF42A5F5).copy(alpha = 0.7f),
            start = Offset(0f, valY),
            end = Offset(chartWidth, valY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = dash
        )
        val valText = textMeasurer.measure(
            "VAL: %.2f".format(data.valPrice),
            TextStyle(color = Color(0xFF42A5F5), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        drawText(valText, topLeft = Offset(chartWidth - valText.size.width - 4.dp.toPx(), valY))
    }

    // 4. Draw Point of Control (POC) solid vibrant line
    if (data.pocPrice in priceMin..priceMax) {
        val pocY = priceToY(data.pocPrice)
        drawLine(
            color = Color(0xFFFF5252),
            start = Offset(0f, pocY),
            end = Offset(chartWidth, pocY),
            strokeWidth = 1.5.dp.toPx()
        )
        val pocText = textMeasurer.measure(
            "POC: %.2f".format(data.pocPrice),
            TextStyle(color = Color(0xFFFF5252), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
        )
        drawText(pocText, topLeft = Offset(4.dp.toPx(), pocY - pocText.size.height - 2.dp.toPx()))
    }
}
