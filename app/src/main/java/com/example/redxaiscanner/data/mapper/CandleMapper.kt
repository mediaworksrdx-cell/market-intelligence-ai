package com.example.redxaiscanner.data.mapper

import com.example.redxaiscanner.data.datasource.local.CandleEntity
import com.example.redxaiscanner.data.datasource.remote.MarketDataDto
import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

fun MarketDataDto.toCandleEntities(timeframe: String): List<CandleEntity> {
    return candles.map { candleData ->
        CandleEntity(
            symbol = this.symbol,
            timeframe = timeframe,
            timestamp = candleData[0].toLong(),
            open = BigDecimal(candleData[1]),
            high = BigDecimal(candleData[2]),
            low = BigDecimal(candleData[3]),
            close = BigDecimal(candleData[4]),
            volume = BigDecimal(candleData[5])
        )
    }
}

fun CandleEntity.toCandle(): Candle {
    return Candle(
        timestamp = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume
    )
}