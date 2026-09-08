package com.example.redxchartlibrary.charts.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.example.redxchartlibrary.state.ChartViewportState

class CandleLayer : ChartLayer {
    override fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer) {
        with(scope) {
            viewportState.visibleCandles.forEachIndexed { index, candle ->
                val absoluteIndex = viewportState.visibleCandleRange.first + index
                val x = viewportState.toScreenX(absoluteIndex)
                val candleWidth = 20f * viewportState.zoom
                
                val candleColor = if (candle.close >= candle.open) Color(0xFF00E676) else Color(0xFFFF1744)
                
                // Draw Wick
                val highY = size.height * viewportState.toScreenY(candle.high.toFloat())
                val lowY = size.height * viewportState.toScreenY(candle.low.toFloat())
                drawLine(
                    color = candleColor.copy(alpha = 0.6f),
                    start = Offset(x + candleWidth / 2, highY),
                    end = Offset(x + candleWidth / 2, lowY),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw Body
                val openY = size.height * viewportState.toScreenY(candle.open.toFloat())
                val closeY = size.height * viewportState.toScreenY(candle.close.toFloat())
                val top = minOf(openY, closeY)
                val bottom = maxOf(openY, closeY)
                
                drawRect(
                    color = candleColor,
                    topLeft = Offset(x + candleWidth * 0.1f, top),
                    size = Size(candleWidth * 0.8f, (bottom - top).coerceAtLeast(1f))
                )
            }
        }
    }
}
