package com.example.redxchartlibrary.charts.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.example.redxchartlibrary.data.indicators.Indicator
import com.example.redxchartlibrary.data.indicators.IndicatorOutput
import com.example.redxchartlibrary.state.ChartState
import com.example.redxchartlibrary.state.ChartViewportState

class IndicatorLayer(
    private val chartState: ChartState,
    private val indicators: List<Indicator>
) : ChartLayer {
    override fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer) {
        indicators.forEach { indicator ->
            chartState.indicatorOutputs[indicator]?.let { output ->
                when (indicator) {
                    is Indicator.SMA, is Indicator.EMA -> drawLineIndicator(scope, viewportState, output, Color.Cyan)
                    is Indicator.BollingerBands -> drawBandsIndicator(scope, viewportState, output)
                    is Indicator.MACD -> {}
                    is Indicator.RSI -> {}
                    is Indicator.Volume -> {}
                }
            }
        }
    }

    private fun drawLineIndicator(scope: DrawScope, viewportState: ChartViewportState, output: IndicatorOutput, color: Color) {
        with(scope) {
            val path = Path()
            var firstPoint = true

            viewportState.visibleCandles.forEach { candle ->
                output.data[candle.openTime]?.firstOrNull()?.let { value ->
                    val x = viewportState.toScreenX(viewportState.candles.indexOf(candle))
                    val y = size.height * viewportState.toScreenY(value)

                    if (x >= 0 && x <= size.width) {
                        if (firstPoint) {
                            path.moveTo(x, y)
                            firstPoint = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
            }

            drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
        }
    }

    private fun drawBandsIndicator(scope: DrawScope, viewportState: ChartViewportState, output: IndicatorOutput) {
        with(scope) {
            val upperBandPath = Path()
            val middleBandPath = Path()
            val lowerBandPath = Path()

            val upperBandPoints = mutableListOf<Pair<Float, Float>>()
            val lowerBandPoints = mutableListOf<Pair<Float, Float>>()

            var firstPoint = true

            viewportState.visibleCandles.forEach { candle ->
                output.data[candle.openTime]?.let { values ->
                    val sma = values.getOrNull(0)
                    val upper = values.getOrNull(1)
                    val lower = values.getOrNull(2)

                    val x = viewportState.toScreenX(viewportState.candles.indexOf(candle))

                    if (x >= 0 && x <= size.width) {
                        val smaY = sma?.let { size.height * viewportState.toScreenY(it) }
                        val upperY = upper?.let { size.height * viewportState.toScreenY(it) }
                        val lowerY = lower?.let { size.height * viewportState.toScreenY(it) }

                        if (firstPoint) {
                            smaY?.let { middleBandPath.moveTo(x, it) }
                            upperY?.let { upperBandPath.moveTo(x, it); upperBandPoints.add(x to it) }
                            lowerY?.let { lowerBandPath.moveTo(x, it); lowerBandPoints.add(x to it) }
                            firstPoint = false
                        } else {
                            smaY?.let { middleBandPath.lineTo(x, it) }
                            upperY?.let { upperBandPath.lineTo(x, it); upperBandPoints.add(x to it) }
                            lowerY?.let { lowerBandPath.lineTo(x, it); lowerBandPoints.add(x to it) }
                        }
                    }
                }
            }

            // Draw the bands
            drawPath(middleBandPath, Color.Yellow, style = Stroke(width = 1.dp.toPx()))
            drawPath(upperBandPath, Color.Yellow, style = Stroke(width = 1.dp.toPx()))
            drawPath(lowerBandPath, Color.Yellow, style = Stroke(width = 1.dp.toPx()))

            // Fill the area between the bands
            if (upperBandPoints.isNotEmpty() && lowerBandPoints.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(upperBandPoints.first().first, upperBandPoints.first().second)
                    upperBandPoints.forEach { (x, y) ->
                        lineTo(x, y)
                    }
                    lineTo(lowerBandPoints.last().first, lowerBandPoints.last().second)
                    for (i in lowerBandPoints.lastIndex - 1 downTo 0) {
                        lineTo(lowerBandPoints[i].first, lowerBandPoints[i].second)
                    }
                    close()
                }
                drawPath(fillPath, Color.Yellow.copy(alpha = 0.1f))
            }
        }
    }
}
