package com.marketintelligence.redxchartlibrary.charts.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.marketintelligence.redxchartlibrary.data.local.AnchorPoint
import com.marketintelligence.redxchartlibrary.data.local.DrawingTool
import com.marketintelligence.redxchartlibrary.state.ChartState
import com.marketintelligence.redxchartlibrary.state.ChartViewportState

class DrawingLayer(private val chartState: ChartState) : ChartLayer {
    override fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer) {
        chartState.drawings.value.forEach { drawing ->
            if (drawing.isVisible) {
                when (val tool = drawing.tool) {
                    is DrawingTool.Trendline -> scope.drawTrendline(tool, viewportState)
                    is DrawingTool.HorizontalLine -> scope.drawHorizontalLine(tool, viewportState)
                    is DrawingTool.FibonacciRetracement -> {}
                    is DrawingTool.OrderBlock -> {}
                    is DrawingTool.Ray -> {}
                    is DrawingTool.Rectangle -> {}
                    is DrawingTool.Text -> {}
                }
            }
        }
    }

    private fun DrawScope.toScreenCoordinates(anchor: AnchorPoint, viewportState: ChartViewportState): Offset {
        val x = viewportState.toScreenX(viewportState.candles.indexOfFirst { it.openTime == anchor.timestamp })
        val y = size.height * viewportState.toScreenY(anchor.price)
        return Offset(x, y)
    }

    private fun DrawScope.drawTrendline(tool: DrawingTool.Trendline, viewportState: ChartViewportState) {
        val start = toScreenCoordinates(tool.start, viewportState)
        val end = toScreenCoordinates(tool.end, viewportState)
        drawLine(Color.Blue, start, end, strokeWidth = 2.dp.toPx())
    }

    private fun DrawScope.drawHorizontalLine(tool: DrawingTool.HorizontalLine, viewportState: ChartViewportState) {
        val y = size.height * viewportState.toScreenY(tool.anchor.price)
        drawLine(
            color = Color.Blue,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx()
        )
    }
}
