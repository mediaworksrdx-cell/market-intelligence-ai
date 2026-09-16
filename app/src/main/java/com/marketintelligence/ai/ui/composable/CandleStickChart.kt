package com.marketintelligence.ai.ui.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.marketintelligence.tradeengine.models.Candle
import kotlin.math.roundToInt

@Composable
fun CandleStickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var scrollOffsetFromRight by remember { mutableFloatStateOf(0f) }
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(modifier = modifier
        .pointerInput(candles.size) {
            detectTransformGestures { _, panAmount, zoomAmount, _ ->
                zoom = (zoom * zoomAmount).coerceIn(0.15f, 6f)
                val candleWidth = 10.dp.toPx() * zoom
                val vCount = (size.width / candleWidth).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
                val mScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
                if (candleWidth > 0f) {
                    scrollOffsetFromRight = (scrollOffsetFromRight + panAmount.x / candleWidth).coerceIn(0f, mScroll)
                }
                if (panAmount.getDistance() > 1.5f || kotlin.math.abs(zoomAmount - 1f) > 0.02f) {
                    crosshairPosition = null
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset -> crosshairPosition = if (crosshairPosition != null) null else offset },
                onLongPress = { offset -> crosshairPosition = offset }
            )
        }
    ) { 
        if (candles.isEmpty()) return@Canvas

        val candleWidthWithZoom = 10.dp.toPx() * zoom
        val visibleCandleCount = (size.width / candleWidthWithZoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
        val maxScroll = (candles.size - visibleCandleCount).coerceAtLeast(0).toFloat()
        val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

        val endIndex = (candles.size - clampedScroll.roundToInt()).coerceIn(visibleCandleCount.coerceAtMost(candles.size), candles.size)
        val startIndex = (endIndex - visibleCandleCount).coerceAtLeast(0)
        
        val visibleCandles = candles.subList(startIndex, endIndex)

        if (visibleCandles.isEmpty()) return@Canvas

        val minPrice = visibleCandles.minOf { it.low }
        val maxPrice = visibleCandles.maxOf { it.high }
        val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

        val candleWidth = size.width / visibleCandleCount

        visibleCandles.forEachIndexed { index, candle ->
            val xOffset = index * candleWidth

            drawCandle(
                candle = candle,
                xOffset = xOffset,
                candleWidth = candleWidth,
                minPrice = minPrice,
                priceRange = priceRange.toFloat()
            )
        }

        crosshairPosition?.let {
            drawCrosshair(it)
        }
    }
}

private fun DrawScope.drawCandle(
    candle: Candle,
    xOffset: Float,
    candleWidth: Float,
    minPrice: Double,
    priceRange: Float
) {
    val highY = size.height - ((candle.high - minPrice) / priceRange * size.height).toFloat()
    val lowY = size.height - ((candle.low - minPrice) / priceRange * size.height).toFloat()
    val openY = size.height - ((candle.open - minPrice) / priceRange * size.height).toFloat()
    val closeY = size.height - ((candle.close - minPrice) / priceRange * size.height).toFloat()

    val color = if (candle.close > candle.open) Color.Green else Color.Red

    // Draw wick
    drawLine(
        color = color,
        start = Offset(xOffset + candleWidth / 2, highY),
        end = Offset(xOffset + candleWidth / 2, lowY),
        strokeWidth = 1.dp.toPx()
    )

    // Draw body
    drawRect(
        color = color,
        topLeft = Offset(xOffset, if (candle.close > candle.open) closeY else openY),
        size = androidx.compose.ui.geometry.Size(
            width = candleWidth,
            height = (openY - closeY).let { if (it == 0f) 1f else if (it > 0) it else -it } 
        )
    )
}

private fun DrawScope.drawCrosshair(position: Offset) {
    // Vertical line
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(position.x, 0f),
        end = Offset(position.x, size.height),
        strokeWidth = 1.dp.toPx()
    )

    // Horizontal line
    drawLine(
        color = Color.Gray.copy(alpha = 0.5f),
        start = Offset(0f, position.y),
        end = Offset(size.width, position.y),
        strokeWidth = 1.dp.toPx()
    )
}
