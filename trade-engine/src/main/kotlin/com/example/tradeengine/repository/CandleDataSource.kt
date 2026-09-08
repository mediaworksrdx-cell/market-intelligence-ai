package com.example.tradeengine.repository

import com.example.tradeengine.models.Candle

interface CandleDataSource {
    suspend fun getLatestCandle(symbol: String, timeframe: String): Candle?
    suspend fun insertCandles(candles: List<Candle>)
    suspend fun updateCandle(openTime: Long, symbol: String, timeframe: String, high: Double, low: Double, close: Double, volume: Double)
    suspend fun closeCandle(openTime: Long, symbol: String, timeframe: String)
    suspend fun getCandles(symbol: String, timeframe: String, from: Long, to: Long): List<Candle>
}
