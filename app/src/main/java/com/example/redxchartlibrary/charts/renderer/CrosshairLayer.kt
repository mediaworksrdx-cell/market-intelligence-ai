package com.example.redxchartlibrary.charts.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.redxchartlibrary.state.ChartViewportState

@OptIn(ExperimentalTextApi::class)
class CrosshairLayer(private val crosshairPosition: Offset?) : ChartLayer {
    override fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer) {
        crosshairPosition?.let { position ->
            with(scope) {
                // Draw crosshair lines
                drawLine(Color.Gray, start = Offset(0f, position.y), end = Offset(size.width, position.y), strokeWidth = 1f)
                drawLine(Color.Gray, start = Offset(position.x, 0f), end = Offset(position.x, size.height), strokeWidth = 1f)

                // Find the candle under the crosshair
                val candleIndex = ((position.x - viewportState.panX) / (20f * viewportState.zoom)).toInt()
                val candle = viewportState.candles.getOrNull(candleIndex)

                candle?.let { 
                    val text = "O: ${it.open} H: ${it.high} L: ${it.low} C: ${it.close}"
                    val textLayoutResult = textMeasurer.measure(text, style = TextStyle(fontSize = 10.sp, color = Color.White))
                    
                    // Draw tooltip background
                    drawRect(
                        color = Color.Black.copy(alpha = 0.7f),
                        topLeft = Offset(position.x + 8.dp.toPx(), position.y - textLayoutResult.size.height - 8.dp.toPx()),
                        size = Size(width = textLayoutResult.size.width + 16.dp.toPx(), height = textLayoutResult.size.height + 8.dp.toPx())
                    )

                    // Draw tooltip text
                    drawText(
                        textLayoutResult,
                        topLeft = Offset(position.x + 12.dp.toPx(), position.y - textLayoutResult.size.height - 4.dp.toPx())
                    )
                }
            }
        }
    }
}
