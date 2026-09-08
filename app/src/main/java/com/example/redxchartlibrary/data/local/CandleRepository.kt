package com.example.redxchartlibrary.data.local

import com.example.redxchartlibrary.model.Candle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandleRepository @Inject constructor(private val candleDao: CandleDao) {

    fun getCandles(symbol: String, timeframe: String): Flow<List<Candle>> {
        return candleDao.getCandles(symbol, timeframe)
    }

    suspend fun saveCandles(candles: List<Candle>) {
        candleDao.insertCandles(candles)
    }

    suspend fun clearAllCandles(symbol: String, timeframe: String) {
        candleDao.clearCandles(symbol, timeframe)
    }
}
