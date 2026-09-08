package com.marketintelligence.redxchartlibrary.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.marketintelligence.redxchartlibrary.charts.renderer.*
import com.marketintelligence.redxchartlibrary.data.local.Drawing
import com.marketintelligence.redxchartlibrary.model.Candle
import com.marketintelligence.redxchartlibrary.state.ChartState

@OptIn(ExperimentalTextApi::class)
@Composable
fun TradingChart(
    modifier: Modifier = Modifier,
    candles: List<Candle>,
    chartState: ChartState,
    symbol: String,
    timeframe: String,
    onSaveDrawing: (Drawing) -> Unit
) {
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(candles) {
        chartState.recalculateIndicators(candles)
    }

    val overlayIndicators = chartState.indicators.value.filter { !it.requiresSeparatePane }
    val paneIndicators = chartState.indicators.value.filter { it.requiresSeparatePane }

    Column(modifier = modifier.fillMaxSize()) {
        ChartPane(
            modifier = Modifier.weight(1f),
            candles = candles,
            layers = listOf(
                GridLayer(),
                CandleLayer(),
                IndicatorLayer(chartState, overlayIndicators),
                DrawingLayer(chartState),
                CrosshairLayer(crosshairPosition)
            ),
            onCrosshairMove = { crosshairPosition = it },
            onDraw = { event, anchor ->
                when (event) {
                    DrawEvent.START -> chartState.startDrawing(anchor, symbol, timeframe)
                    DrawEvent.DRAG -> chartState.updateDrawing(anchor)
                    DrawEvent.END -> chartState.finalizeDrawing()?.let { onSaveDrawing(it) }
                }
            }
        )

        paneIndicators.forEach { indicator ->
            chartState.indicatorOutputs[indicator]?.let {
                val textMeasurer = rememberTextMeasurer()
                IndicatorPane(
                    modifier = Modifier.height(100.dp),
                    indicator = indicator,
                    indicatorOutput = it,
                    viewportState = com.marketintelligence.redxchartlibrary.state.rememberChartViewportState(candles, canvasSize = androidx.compose.ui.geometry.Size(0f, 100.dp.value)),
                    textMeasurer = textMeasurer
                )
            }
        }
    }
}
