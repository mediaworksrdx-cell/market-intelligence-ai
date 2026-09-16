package com.example.marketintelligence.data.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.marketintelligence.domain.chart.*
import com.example.marketintelligence.ui.chart.AdvancedCandleStickChart
import com.example.marketintelligence.ui.chart.ChartState
import com.example.marketintelligence.domain.engine.ChartEngine
import com.example.tradeengine.models.Candle
import javax.inject.Inject

class ProprietaryChartEngine @Inject constructor() : ChartEngine {
    override val engineName: String = "Proprietary Engine"

    @Composable
    override fun Render(symbol: String, timeframe: String, candles: List<Candle>) {
        // Use the advanced chart with default state (indicators/drawings managed by AnalysisScreen)
        val chartState = remember(candles) {
            ChartState(candles = candles)
        }

        AdvancedCandleStickChart(
            chartState = chartState,
            modifier = Modifier.fillMaxSize(),
            timeframe = timeframe
        )
    }
}
