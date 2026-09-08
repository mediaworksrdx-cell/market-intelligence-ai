package com.example.marketintelligence.data.mappers

import com.example.redxchartlibrary.model.Candle as ChartCandle
import com.example.tradeengine.models.Candle as EngineCandle

fun EngineCandle.toChartCandle(): ChartCandle {
    return ChartCandle(
        symbol = symbol,
        timeframe = timeframe,
        openTime = openTime,
        closeTime = closeTime,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
    )
}
