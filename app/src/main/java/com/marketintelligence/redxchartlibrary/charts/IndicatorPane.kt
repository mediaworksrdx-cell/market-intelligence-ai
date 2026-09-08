package com.marketintelligence.redxchartlibrary.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import com.marketintelligence.redxchartlibrary.data.indicators.Indicator
import com.marketintelligence.redxchartlibrary.data.indicators.IndicatorOutput
import com.marketintelligence.redxchartlibrary.state.ChartViewportState
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalTextApi::class)
@Composable
fun IndicatorPane(
    modifier: Modifier = Modifier,
    indicator: Indicator,
    indicatorOutput: IndicatorOutput,
    viewportState: ChartViewportState,
    textMeasurer: TextMeasurer
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        when (indicator) {
            is Indicator.RSI -> drawRsi(this, viewportState, indicatorOutput, textMeasurer)
            is Indicator.MACD -> drawMacd(this, viewportState, indicatorOutput, textMeasurer)
            is Indicator.Volume -> drawVolume(this, viewportState, indicatorOutput, textMeasurer)
            is Indicator.BollingerBands -> {}
            is Indicator.EMA -> {}
            is Indicator.SMA -> {}
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun drawRsi(
    scope: DrawScope,
    viewportState: ChartViewportState,
    output: IndicatorOutput,
    textMeasurer: TextMeasurer
) {
    with(scope) {
        // Define RSI pane bounds and scaling
        val minRsi = 0f
        val maxRsi = 100f

        fun rsiToY(value: Float): Float {
            return size.height - ((value - minRsi) / (maxRsi - minRsi)) * size.height
        }

        // Draw RSI line
        val path = Path()
        var firstPoint = true
        viewportState.visibleCandles.forEach { candle ->
            output.data[candle.openTime]?.firstOrNull()?.let { rsiValue ->
                val x = viewportState.toScreenX(viewportState.candles.indexOf(candle))
                val y = rsiToY(rsiValue)

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
        drawPath(path, Color.Yellow, style = Stroke(width = 1.5f))
    }
}

@OptIn(ExperimentalTextApi::class)
private fun drawMacd(
    scope: DrawScope,
    viewportState: ChartViewportState,
    output: IndicatorOutput,
    textMeasurer: TextMeasurer
) {
    with(scope) {
        // Find min/max for Y-axis scaling
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE
        viewportState.visibleCandles.forEach { candle ->
            output.data[candle.openTime]?.forEach { value ->
                value?.let {
                    minVal = min(minVal, it)
                    maxVal = max(maxVal, it)
                }
            }
        }

        val range = (maxVal - minVal).coerceAtLeast(0.1f)

        fun macdToY(value: Float): Float {
            return size.height - ((value - minVal) / range) * size.height
        }

        val macdPath = Path()
        val signalPath = Path()
        var firstMacd = true
        var firstSignal = true

        viewportState.visibleCandles.forEach { candle ->
            output.data[candle.openTime]?.let { values ->
                val macdValue = values.getOrNull(0)
                val signalValue = values.getOrNull(1)
                val histogramValue = values.getOrNull(2)

                val index = viewportState.candles.indexOf(candle)
                val x = viewportState.toScreenX(index)
                val candleWidth = 20f * viewportState.zoom

                if (x >= 0 && x <= size.width) {
                    // Draw MACD line
                    macdValue?.let {
                        val y = macdToY(it)
                        if (firstMacd) {
                            macdPath.moveTo(x, y)
                            firstMacd = false
                        } else {
                            macdPath.lineTo(x, y)
                        }
                    }

                    // Draw Signal line
                    signalValue?.let {
                        val y = macdToY(it)
                        if (firstSignal) {
                            signalPath.moveTo(x, y)
                            firstSignal = false
                        } else {
                            signalPath.lineTo(x, y)
                        }
                    }

                    // Draw Histogram
                    histogramValue?.let {
                        val y = macdToY(it)
                        val zeroY = macdToY(0f)
                        val color = if (it >= 0) Color.Green else Color.Red
                        drawRect(color, topLeft = Offset(x, if(it >= 0) y else zeroY), size = Size(candleWidth * 0.8f, (if(it >= 0) zeroY - y else y - zeroY).coerceAtLeast(1f)))
                    }
                }
            }
        }

        drawPath(macdPath, Color.Blue, style = Stroke(width = 1.5f))
        drawPath(signalPath, Color.Red, style = Stroke(width = 1.5f))
    }
}

@OptIn(ExperimentalTextApi::class)
private fun drawVolume(
    scope: DrawScope,
    viewportState: ChartViewportState,
    output: IndicatorOutput,
    textMeasurer: TextMeasurer
) {
    with(scope) {
        val maxVolume = viewportState.visibleCandles.maxOfOrNull { it.volume }?.toFloat() ?: 1f

        fun volumeToY(value: Float): Float {
            return size.height - (value / maxVolume) * size.height
        }

        val maPath = Path()
        var firstMaPoint = true

        viewportState.visibleCandles.forEach { candle ->
            val index = viewportState.candles.indexOf(candle)
            val x = viewportState.toScreenX(index)
            val candleWidth = 20f * viewportState.zoom

            // Draw Volume Bar
            val volumeY = volumeToY(candle.volume.toFloat())
            val barColor = if (candle.close >= candle.open) Color.Green else Color.Red
            drawRect(
                color = barColor.copy(alpha = 0.5f),
                topLeft = Offset(x, volumeY),
                size = Size(candleWidth * 0.8f, size.height - volumeY)
            )

            // Draw Volume MA
            output.data[candle.openTime]?.getOrNull(1)?.let { maValue ->
                val maY = volumeToY(maValue)
                if (firstMaPoint) {
                    maPath.moveTo(x, maY)
                    firstMaPoint = false
                } else {
                    maPath.lineTo(x, maY)
                }
            }
        }

        drawPath(maPath, Color.Yellow, style = Stroke(width = 1.5f))
    }
}
