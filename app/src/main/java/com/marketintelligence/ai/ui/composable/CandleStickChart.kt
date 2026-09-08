package com.marketintelligence.ai.ui.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
    var pan by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
        pan = (pan + panChange.x)
    }

    Canvas(modifier = modifier
        .transformable(state = transformableState)
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    pan += dragAmount.x
                    change.consume()
                }
            )
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset -> crosshairPosition = offset },
                onTap = { crosshairPosition = null }
            )
        }
    ) { 
        if (candles.isEmpty()) return@Canvas

        val candleWidthWithZoom = 10.dp.toPx() * zoom

        // Adjust pan to stay within bounds
        val maxPan = (candles.size * candleWidthWithZoom - size.width).coerceAtLeast(0f)
        pan = pan.coerceIn(-maxPan, 0f)

        val visibleCandleCount = (size.width / candleWidthWithZoom).roundToInt()
        val startIndex = ((pan / candleWidthWithZoom) * -1).toInt().coerceIn(0, candles.size - 1)
        val endIndex = (startIndex + visibleCandleCount).coerceAtMost(candles.size)
        
        val visibleCandles = candles.subList(startIndex, endIndex)

        if (visibleCandles.isEmpty()) return@Canvas

        val minPrice = visibleCandles.minOf { it.low }
        val maxPrice = visibleCandles.maxOf { it.high }
        val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

        val candleWidth = size.width / visibleCandleCount

        visibleCandles.forEachIndexed { index, candle ->
            val xOffset = index * candleWidth + (pan % candleWidth)

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
