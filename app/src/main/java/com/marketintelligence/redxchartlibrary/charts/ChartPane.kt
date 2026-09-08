package com.marketintelligence.redxchartlibrary.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import com.marketintelligence.redxchartlibrary.charts.renderer.ChartLayer
import com.marketintelligence.redxchartlibrary.data.local.AnchorPoint
import com.marketintelligence.redxchartlibrary.model.Candle
import com.marketintelligence.redxchartlibrary.state.ChartViewportState
import com.marketintelligence.redxchartlibrary.state.rememberChartViewportState

enum class DrawEvent { START, DRAG, END }

@OptIn(ExperimentalTextApi::class)
@Composable
fun ChartPane(
    modifier: Modifier = Modifier,
    candles: List<Candle>,
    layers: List<ChartLayer>,
    onCrosshairMove: (Offset?) -> Unit,
    onDraw: (DrawEvent, AnchorPoint) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val viewportState = rememberChartViewportState(candles, canvasSize)

    fun toAnchorPoint(offset: Offset): AnchorPoint {
        val candleIndex = ((offset.x - viewportState.panX) / (20f * viewportState.zoom)).toInt()
        val price = viewportState.priceRange.start + ((canvasSize.height - offset.y) / canvasSize.height) * (viewportState.priceRange.endInclusive - viewportState.priceRange.start)
        val candle = candles.getOrNull(candleIndex)
        return AnchorPoint(candle?.openTime ?: 0, price)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewportState.zoom = (viewportState.zoom * zoom).coerceIn(0.1f, 10f)
                    viewportState.panX += pan.x
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        onCrosshairMove(it)
                        onDraw(DrawEvent.START, toAnchorPoint(it))
                    },
                    onDrag = { change, _ -> 
                        onCrosshairMove(change.position)
                        onDraw(DrawEvent.DRAG, toAnchorPoint(change.position))
                    },
                    onDragEnd = { 
                        onCrosshairMove(null)
                        onDraw(DrawEvent.END, toAnchorPoint(Offset.Zero)) // AnchorPoint is not used on END
                    }
                )
            }
    ) {
        canvasSize = size
        if (candles.isEmpty()) return@Canvas

        layers.forEach { layer ->
            layer.draw(this, viewportState, textMeasurer)
        }
    }
}
