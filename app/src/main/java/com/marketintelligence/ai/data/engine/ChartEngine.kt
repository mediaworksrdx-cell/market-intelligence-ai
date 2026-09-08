package com.marketintelligence.ai.data.engine

import androidx.compose.runtime.Composable
import com.marketintelligence.tradeengine.models.Candle

interface ChartEngine {
    @Composable
    fun Render(symbol: String, timeframe: String, candles: List<Candle>)
}
