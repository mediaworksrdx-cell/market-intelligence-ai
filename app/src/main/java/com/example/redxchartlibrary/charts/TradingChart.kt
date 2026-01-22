package com.example.redxchartlibrary.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.redxchartlibrary.data.local.AnchorPoint
import com.example.redxchartlibrary.data.local.Drawing
import com.example.redxchartlibrary.data.local.DrawingTool
import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.state.ChartState
import com.example.redxchartlibrary.state.DrawingMode

@OptIn(ExperimentalTextApi::class)
@Composable
fun TradingChart(
    modifier: Modifier = Modifier,
    candles: List<Candle>,
    chartState: ChartState,
    onSaveDrawing: (Drawing) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()

    // We need the canvas size for calculations, which is only available inside onDraw.
    // To use it in gesture handlers, we store it in a state variable.
    var canvasSize by remember { mutableStateOf(Offset(0f, 0f)) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (chartState.drawingMode == DrawingMode.NONE) {
                        chartState.zoom = (chartState.zoom * zoom).coerceIn(0.1f, 10f)
                        chartState.pan += pan.x
                    }
                }
            }
            .pointerInput(chartState.drawingMode) {
                if (chartState.drawingMode != DrawingMode.NONE) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            handleDragStart(offset, candles, chartState, canvasSize)
                        },
                        onDrag = { change, _ ->
                            handleDrag(change.position, candles, chartState, canvasSize)
                        },
                        onDragEnd = {
                            handleDragEnd(chartState, onSaveDrawing)
                        }
                    )
                }
            }
    ) {
        canvasSize = Offset(size.width, size.height)
        if (candles.isEmpty()) return@Canvas

        val (visibleCandles, startIndex, candleWidth, minPrice, priceRange) = getChartData(candles, chartState)
        if (visibleCandles.isEmpty()) return@Canvas
        
        // --- Main Drawing Logic ---
        chartState.drawings.forEach { drawing ->
            drawTool(drawing.tool, candles, chartState, minPrice, priceRange, candleWidth, textMeasurer)
        }
        
        chartState.currentDrawing?.let {
            drawTool(it, candles, chartState, minPrice, priceRange, candleWidth, textMeasurer)
        }
    }
}

private fun DrawScope.drawTool(
    tool: DrawingTool,
    candles: List<Candle>,
    chartState: ChartState,
    minPrice: Float,
    priceRange: Float,
    candleWidth: Float,
    textMeasurer: TextMeasurer
) {
    when (tool) {
        is DrawingTool.Trendline -> drawTrendline(tool, candles, chartState, minPrice, priceRange, candleWidth)
        is DrawingTool.FibonacciRetracement -> drawFibonacci(tool, candles, chartState, minPrice, priceRange, candleWidth, textMeasurer)
    }
}


// --- GESTURE HANDLING ---

private fun handleDragStart(offset: Offset, candles: List<Candle>, chartState: ChartState, canvasSize: Offset) {
    val (visibleCandles, startIndex, candleWidth, minPrice, priceRange) = getChartDataForInteraction(candles, chartState, canvasSize)
    if(visibleCandles.isEmpty()) return

    val startAnchor = screenToChartCoordinates(offset, candles, chartState, minPrice, priceRange, candleWidth, startIndex, canvasSize)
    
    chartState.currentDrawing = when (chartState.drawingMode) {
        DrawingMode.TRENDLINE -> DrawingTool.Trendline(startAnchor, startAnchor)
        DrawingMode.FIBONACCI -> DrawingTool.FibonacciRetracement(startAnchor, startAnchor)
        else -> null
    }
}

private fun handleDrag(position: Offset, candles: List<Candle>, chartState: ChartState, canvasSize: Offset) {
    val (visibleCandles, startIndex, candleWidth, minPrice, priceRange) = getChartDataForInteraction(candles, chartState, canvasSize)
    if(visibleCandles.isEmpty()) return
    
    val currentAnchor = screenToChartCoordinates(position, candles, chartState, minPrice, priceRange, candleWidth, startIndex, canvasSize)
    
    chartState.currentDrawing = when (val tool = chartState.currentDrawing) {
        is DrawingTool.Trendline -> tool.copy(end = currentAnchor)
        is DrawingTool.FibonacciRetracement -> tool.copy(end = currentAnchor)
        else -> null
    }
}

private fun handleDragEnd(chartState: ChartState, onSaveDrawing: (Drawing) -> Unit) {
    chartState.currentDrawing?.let {
        onSaveDrawing(Drawing(tool = it))
    }
    chartState.currentDrawing = null
    chartState.drawingMode = DrawingMode.NONE
}

// Simplified data calculation for interaction handlers.
private fun getChartDataForInteraction(candles: List<Candle>, chartState: ChartState, canvasSize: Offset): ChartRenderData {
    val candleWidth = 20f * chartState.zoom
    val startIndex = ((-chartState.pan) / candleWidth).toInt().coerceAtLeast(0)
    val visibleCount = (canvasSize.x / candleWidth).toInt() + 2
    val endIndex = (startIndex + visibleCount).coerceAtMost(candles.size)
    val visibleCandles = if (startIndex <= endIndex) candles.subList(startIndex, endIndex) else emptyList()

    val minPrice = visibleCandles.minOfOrNull { it.low } ?: 0f
    val maxPrice = visibleCandles.maxOfOrNull { it.high } ?: 1f
    val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01f)

    return ChartRenderData(visibleCandles, startIndex, candleWidth, minPrice, priceRange)
}

private data class ChartRenderData(
    val visibleCandles: List<Candle>,
    val startIndex: Int,
    val candleWidth: Float,
    val minPrice: Float,
    val priceRange: Float
)

private fun screenToChartCoordinates(
    offset: Offset, candles: List<Candle>, chartState: ChartState,
    minPrice: Float, priceRange: Float, candleWidth: Float, startIndex: Int, canvasSize: Offset
): AnchorPoint {
    val candleIndex = (startIndex + ((offset.x - chartState.pan) / candleWidth)).toInt().coerceIn(0, candles.size - 1)
    val timestamp = candles[candleIndex].timestamp
    val price = minPrice + ((canvasSize.y - offset.y) / canvasSize.y) * priceRange
    return AnchorPoint(timestamp, price.toFloat())
}

private fun DrawScope.chartToScreenCoordinates(
    anchor: AnchorPoint, candles: List<Candle>, chartState: ChartState,
    minPrice: Float, priceRange: Float, candleWidth: Float
): Offset? {
    val candleIndex = candles.indexOfFirst { it.timestamp == anchor.timestamp }
    if (candleIndex == -1) return null

    val x = candleIndex * candleWidth + chartState.pan
    val y = size.height * (1 - ((anchor.price - minPrice) / priceRange))
    return Offset(x, y.toFloat())
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawFibonacci(
    tool: DrawingTool.FibonacciRetracement,
    candles: List<Candle>,
    chartState: ChartState,
    minPrice: Float,
    priceRange: Float,
    candleWidth: Float,
    textMeasurer: TextMeasurer
) {
    val levels = listOf(0.0f, 0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1.0f)
    val startScreenPos = chartToScreenCoordinates(tool.start, candles, chartState, minPrice, priceRange, candleWidth)
    val endScreenPos = chartToScreenCoordinates(tool.end, candles, chartState, minPrice, priceRange, candleWidth)

    if(startScreenPos == null || endScreenPos == null) return

    val priceDiff = endScreenPos.y - startScreenPos.y

    levels.forEach { level ->
        val y = startScreenPos.y + priceDiff * level
        drawLine(
            color = Color.Yellow.copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
        drawText(
            textMeasurer,
            text = "%.3f".format(level),
            topLeft = Offset(10f, y - 20f),
            style = TextStyle(color = Color.Yellow, fontSize = 12.sp)
        )
    }
}

private fun DrawScope.drawTrendline(
    tool: DrawingTool.Trendline,
    candles: List<Candle>,
    chartState: ChartState,
    minPrice: Float,
    priceRange: Float,
    candleWidth: Float
) {
    val start = chartToScreenCoordinates(tool.start, candles, chartState, minPrice, priceRange, candleWidth)
    val end = chartToScreenCoordinates(tool.end, candles, chartState, minPrice, priceRange, candleWidth)
    if (start != null && end != null) {
        drawLine(Color.Blue, start, end, strokeWidth = 2f)
    }
}
private fun DrawScope.getChartData(candles: List<Candle>, chartState: ChartState): ChartRenderData {
    val candleWidth = 20f * chartState.zoom
    val startIndex = ((-chartState.pan) / candleWidth).toInt().coerceAtLeast(0)
    val visibleCount = (size.width / candleWidth).toInt() + 2
    val endIndex = (startIndex + visibleCount).coerceAtMost(candles.size)
    val visibleCandles = if (startIndex <= endIndex) candles.subList(startIndex, endIndex) else emptyList()

    val minPrice = visibleCandles.minOfOrNull { it.low } ?: 0f
    val maxPrice = visibleCandles.maxOfOrNull { it.high } ?: 1f
    val priceRange = (maxPrice - minPrice).coerceAtLeast(0.01f)

    return ChartRenderData(visibleCandles, startIndex, candleWidth, minPrice, priceRange)
}