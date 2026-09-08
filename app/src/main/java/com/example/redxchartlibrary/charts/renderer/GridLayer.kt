package com.example.redxchartlibrary.charts.renderer

import androidx.compose.ui.geometry.Offset
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
class GridLayer : ChartLayer {
    override fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer) {
        with(scope) {
            val gridCount = 5
            val priceStep = (viewportState.priceRange.endInclusive - viewportState.priceRange.start) / gridCount

            for (i in 0..gridCount) {
                val price = viewportState.priceRange.start + (i * priceStep)
                val y = size.height * viewportState.toScreenY(price)
                
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                
                drawText(
                    textMeasurer,
                    text = "%.2f".format(price),
                    topLeft = Offset(size.width - 60.dp.toPx(), y - 10.dp.toPx()),
                    style = TextStyle(color = Color.Gray, fontSize = 9.sp)
                )
            }
        }
    }
}
