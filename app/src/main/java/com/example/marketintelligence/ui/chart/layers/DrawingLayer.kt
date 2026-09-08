package com.example.marketintelligence.ui.chart.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.marketintelligence.domain.chart.DrawingData
import com.example.marketintelligence.domain.chart.DrawingToolType

/**
 * Renders user-drawn annotations (trendlines, horizontal lines, Fibonacci, rectangles, channels)
 * on the price chart canvas.
 */
fun DrawScope.drawDrawingLayer(
    drawings: List<DrawingData>,
    currentDrawing: DrawingData?,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceMax: Double,
    chartAreaHeight: Float,
    rightMargin: Float,
    candleStartTime: Long,
    candleIntervalMs: Long
) {
    val chartWidth = size.width - rightMargin
    val priceRange = priceMax - priceMin
    if (priceRange <= 0 || candleIntervalMs <= 0) return

    val allDrawings = if (currentDrawing != null) drawings + currentDrawing else drawings

    for (drawing in allDrawings) {
        if (drawing.points.isEmpty()) continue
        val color = Color(drawing.color)
        val strokeWidth = drawing.lineWidth.dp.toPx()

        when (drawing.toolType) {
            DrawingToolType.TRENDLINE -> drawTrendline(drawing, visibleStartIndex, visibleEndIndex, priceMin, priceRange, chartAreaHeight, chartWidth, candleStartTime, candleIntervalMs, color, strokeWidth)
            DrawingToolType.HORIZONTAL_LINE -> drawHorizontalLine(drawing, priceMin, priceRange, chartAreaHeight, chartWidth, color, strokeWidth)
            DrawingToolType.FIBONACCI -> drawFibonacci(drawing, visibleStartIndex, visibleEndIndex, priceMin, priceRange, chartAreaHeight, chartWidth, candleStartTime, candleIntervalMs, color, strokeWidth)
            DrawingToolType.RECTANGLE -> drawRectangleDrawing(drawing, visibleStartIndex, visibleEndIndex, priceMin, priceRange, chartAreaHeight, chartWidth, candleStartTime, candleIntervalMs, color, strokeWidth)
            DrawingToolType.CHANNEL -> drawChannel(drawing, visibleStartIndex, visibleEndIndex, priceMin, priceRange, chartAreaHeight, chartWidth, candleStartTime, candleIntervalMs, color, strokeWidth)
            DrawingToolType.NONE -> {}
        }
    }
}

private fun DrawScope.timestampToX(
    timestamp: Long,
    visibleStartIndex: Int,
    candleStartTime: Long,
    candleIntervalMs: Long,
    chartWidth: Float,
    visibleCount: Int
): Float {
    val candleIndex = ((timestamp - candleStartTime) / candleIntervalMs).toFloat()
    val localIndex = candleIndex - visibleStartIndex
    val candleWidth = chartWidth / visibleCount
    return localIndex * candleWidth + candleWidth / 2f
}

private fun DrawScope.priceToY(price: Double, priceMin: Double, priceRange: Double, chartHeight: Float): Float {
    return chartHeight - ((price - priceMin) / priceRange * chartHeight).toFloat()
}

private fun DrawScope.drawTrendline(
    drawing: DrawingData,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    chartWidth: Float,
    candleStartTime: Long,
    candleIntervalMs: Long,
    color: Color,
    strokeWidth: Float
) {
    if (drawing.points.size < 2) return
    val visibleCount = visibleEndIndex - visibleStartIndex
    val p1 = drawing.points[0]
    val p2 = drawing.points[1]
    val x1 = timestampToX(p1.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y1 = priceToY(p1.price, priceMin, priceRange, chartHeight)
    val x2 = timestampToX(p2.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y2 = priceToY(p2.price, priceMin, priceRange, chartHeight)

    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = strokeWidth)

    // Draw anchor dots
    drawCircle(color, radius = 4.dp.toPx(), center = Offset(x1, y1))
    drawCircle(color, radius = 4.dp.toPx(), center = Offset(x2, y2))
}

private fun DrawScope.drawHorizontalLine(
    drawing: DrawingData,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    chartWidth: Float,
    color: Color,
    strokeWidth: Float
) {
    if (drawing.points.isEmpty()) return
    val price = drawing.points[0].price
    val y = priceToY(price, priceMin, priceRange, chartHeight)

    drawLine(
        color,
        Offset(0f, y),
        Offset(chartWidth, y),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))
    )
}

private fun DrawScope.drawFibonacci(
    drawing: DrawingData,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    chartWidth: Float,
    candleStartTime: Long,
    candleIntervalMs: Long,
    color: Color,
    strokeWidth: Float
) {
    if (drawing.points.size < 2) return
    val highPrice = maxOf(drawing.points[0].price, drawing.points[1].price)
    val lowPrice = minOf(drawing.points[0].price, drawing.points[1].price)
    val range = highPrice - lowPrice
    if (range <= 0) return

    val fibLevels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0)
    val fibColors = listOf(
        Color(0xFFFF1744), // 0%
        Color(0xFFFF9800), // 23.6%
        Color(0xFFFFEB3B), // 38.2%
        Color(0xFF00E676), // 50%
        Color(0xFF2196F3), // 61.8%
        Color(0xFF9C27B0), // 78.6%
        Color(0xFFFF1744)  // 100%
    )

    for ((index, level) in fibLevels.withIndex()) {
        val price = highPrice - range * level
        val y = priceToY(price, priceMin, priceRange, chartHeight)
        val lineColor = fibColors.getOrElse(index) { color }

        drawLine(
            lineColor.copy(alpha = 0.7f),
            Offset(0f, y),
            Offset(chartWidth, y),
            strokeWidth = 0.8.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 3.dp.toPx()))
        )
    }

    // Fill between 0.382 and 0.618
    val y382 = priceToY(highPrice - range * 0.382, priceMin, priceRange, chartHeight)
    val y618 = priceToY(highPrice - range * 0.618, priceMin, priceRange, chartHeight)
    drawRect(
        Color(0xFF00E676).copy(alpha = 0.05f),
        topLeft = Offset(0f, minOf(y382, y618)),
        size = Size(chartWidth, kotlin.math.abs(y618 - y382))
    )
}

private fun DrawScope.drawRectangleDrawing(
    drawing: DrawingData,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    chartWidth: Float,
    candleStartTime: Long,
    candleIntervalMs: Long,
    color: Color,
    strokeWidth: Float
) {
    if (drawing.points.size < 2) return
    val visibleCount = visibleEndIndex - visibleStartIndex
    val p1 = drawing.points[0]
    val p2 = drawing.points[1]

    val x1 = timestampToX(p1.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y1 = priceToY(p1.price, priceMin, priceRange, chartHeight)
    val x2 = timestampToX(p2.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y2 = priceToY(p2.price, priceMin, priceRange, chartHeight)

    val left = minOf(x1, x2)
    val top = minOf(y1, y2)
    val width = kotlin.math.abs(x2 - x1)
    val height = kotlin.math.abs(y2 - y1)

    // Fill
    drawRect(color.copy(alpha = 0.08f), topLeft = Offset(left, top), size = Size(width, height))
    // Border
    drawRect(color.copy(alpha = 0.6f), topLeft = Offset(left, top), size = Size(width, height), style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawChannel(
    drawing: DrawingData,
    visibleStartIndex: Int,
    visibleEndIndex: Int,
    priceMin: Double,
    priceRange: Double,
    chartHeight: Float,
    chartWidth: Float,
    candleStartTime: Long,
    candleIntervalMs: Long,
    color: Color,
    strokeWidth: Float
) {
    // Channel needs 3 points: 2 for the first line, 1 for the parallel offset
    if (drawing.points.size < 3) {
        // Draw as trendline if only 2 points
        if (drawing.points.size == 2) {
            drawTrendline(drawing, visibleStartIndex, visibleEndIndex, priceMin, priceRange, chartHeight, chartWidth, candleStartTime, candleIntervalMs, color, strokeWidth)
        }
        return
    }

    val visibleCount = visibleEndIndex - visibleStartIndex
    val p1 = drawing.points[0]
    val p2 = drawing.points[1]
    val p3 = drawing.points[2]

    // First line
    val x1 = timestampToX(p1.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y1 = priceToY(p1.price, priceMin, priceRange, chartHeight)
    val x2 = timestampToX(p2.timestamp, visibleStartIndex, candleStartTime, candleIntervalMs, chartWidth, visibleCount)
    val y2 = priceToY(p2.price, priceMin, priceRange, chartHeight)
    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = strokeWidth)

    // Parallel line offset by the third point
    val priceOffset = p3.price - p1.price
    val y1p = priceToY(p1.price + priceOffset, priceMin, priceRange, chartHeight)
    val y2p = priceToY(p2.price + priceOffset, priceMin, priceRange, chartHeight)
    drawLine(color, Offset(x1, y1p), Offset(x2, y2p), strokeWidth = strokeWidth)

    // Fill between
    val path = Path()
    path.moveTo(x1, y1)
    path.lineTo(x2, y2)
    path.lineTo(x2, y2p)
    path.lineTo(x1, y1p)
    path.close()
    drawPath(path, color.copy(alpha = 0.06f))
}
