package com.marketintelligence.ai.data.mappers

import com.marketintelligence.redxchartlibrary.model.Candle as ChartCandle
import com.marketintelligence.tradeengine.models.Candle as EngineCandle

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
