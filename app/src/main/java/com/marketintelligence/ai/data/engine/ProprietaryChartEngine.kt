package com.marketintelligence.ai.data.engine

import androidx.compose.runtime.Composable
import com.marketintelligence.ai.domain.engine.ChartEngine
import com.marketintelligence.ai.ui.composable.InstitutionalChartEngine
import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject

class ProprietaryChartEngine @Inject constructor() : ChartEngine {
    override val engineName: String = "Proprietary Engine"

    @Composable
    override fun Render(symbol: String, timeframe: String, candles: List<Candle>) {
        InstitutionalChartEngine(
            symbol = symbol,
            timeframe = timeframe,
            candles = candles
        )
    }
}
