package com.marketintelligence.ai.data.source.local

import com.marketintelligence.tradeengine.models.Candle

fun Candle.toEntity(): CandleEntity {
    return CandleEntity(
        symbol = symbol,
        timeframe = timeframe,
        openTime = openTime,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
        closeTime = closeTime,
        isClosed = isClosed
    )
}

fun CandleEntity.toModel(): Candle {
    return Candle(
        symbol = symbol,
        timeframe = timeframe,
        openTime = openTime,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
        closeTime = closeTime,
        isClosed = isClosed
    )
}

fun List<Candle>.toEntityList(): List<CandleEntity> {
    return this.map { it.toEntity() }
}

fun List<CandleEntity>.toModelList(): List<Candle> {
    return this.map { it.toModel() }
}
