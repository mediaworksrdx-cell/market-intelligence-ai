package com.example.marketintelligence.data.engine

import androidx.compose.runtime.Composable
import com.example.tradeengine.models.Candle

interface ChartEngine {
    @Composable
    fun Render(symbol: String, timeframe: String, candles: List<Candle>)
}
