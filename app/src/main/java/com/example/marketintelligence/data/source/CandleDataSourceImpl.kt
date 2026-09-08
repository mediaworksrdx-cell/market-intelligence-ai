package com.example.marketintelligence.data.source

import com.example.tradeengine.models.Candle
import com.example.tradeengine.repository.CandleDataSource

class CandleDataSourceImpl(private val candleRepository: CandleRepository) : CandleDataSource {

    override suspend fun getLatestCandle(symbol: String, timeframe: String): Candle? {
        return candleRepository.getLatestCandle(symbol, timeframe)
    }

    override suspend fun insertCandles(candles: List<Candle>) {
        candleRepository.insertCandles(candles)
    }

    override suspend fun updateCandle(
        openTime: Long,
        symbol: String,
        timeframe: String,
        high: Double,
        low: Double,
        close: Double,
        volume: Double
    ) {
        candleRepository.updateCandle(openTime, symbol, timeframe, high, low, close, volume)
    }

    override suspend fun closeCandle(openTime: Long, symbol: String, timeframe: String) {
        candleRepository.closeCandle(openTime, symbol, timeframe)
    }

    override suspend fun getCandles(symbol: String, timeframe: String, from: Long, to: Long): List<Candle> {
        return candleRepository.getCandles(symbol, timeframe, from, to)
    }
}
