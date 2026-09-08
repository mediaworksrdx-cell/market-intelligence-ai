package com.example.marketintelligence.domain.engine

import androidx.compose.runtime.Composable
import com.example.tradeengine.models.Candle

interface ChartEngine : Engine {
    @Composable
    fun Render(symbol: String, timeframe: String, candles: List<Candle>)
}
