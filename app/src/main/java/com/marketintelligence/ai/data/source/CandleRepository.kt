package com.marketintelligence.ai.data.source

import com.marketintelligence.ai.data.source.local.CandleDao
import com.marketintelligence.ai.data.source.local.toEntityList
import com.marketintelligence.ai.data.source.local.toModel
import com.marketintelligence.ai.data.source.local.toModelList
import com.marketintelligence.tradeengine.models.Candle

class CandleRepository(private val candleDao: CandleDao) {

    suspend fun getLatestCandle(symbol: String, timeframe: String): Candle? {
        return candleDao.getLatestCandle(symbol, timeframe)?.toModel()
    }

    suspend fun getCandles(symbol: String, timeframe: String, from: Long, to: Long): List<Candle> {
        return candleDao.getCandles(symbol, timeframe, from, to).toModelList()
    }

    suspend fun insertCandles(candles: List<Candle>) {
        candleDao.insertCandles(candles.toEntityList())
    }

    suspend fun updateCandle(openTime: Long, symbol: String, timeframe: String, high: Double, low: Double, close: Double, volume: Double) {
        candleDao.updateCandle(openTime, symbol, timeframe, high, low, close, volume)
    }

    suspend fun closeCandle(openTime: Long, symbol: String, timeframe: String) {
        candleDao.closeCandle(openTime, symbol, timeframe)
    }
}
