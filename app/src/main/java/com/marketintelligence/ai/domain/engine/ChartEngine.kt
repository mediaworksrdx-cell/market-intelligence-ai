package com.marketintelligence.ai.domain.engine

import androidx.compose.runtime.Composable
import com.marketintelligence.tradeengine.models.Candle

interface ChartEngine : Engine {
    @Composable
    fun Render(symbol: String, timeframe: String, candles: List<Candle>)
}
