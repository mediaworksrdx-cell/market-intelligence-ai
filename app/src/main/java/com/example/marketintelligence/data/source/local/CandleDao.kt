package com.example.marketintelligence.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CandleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY openTime DESC LIMIT 1")
    suspend fun getLatestCandle(symbol: String, timeframe: String): CandleEntity?

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe AND openTime >= :from AND openTime < :to ORDER BY openTime ASC")
    suspend fun getCandles(symbol: String, timeframe: String, from: Long, to: Long): List<CandleEntity>

    @Query("UPDATE candles SET high = :high, low = :low, close = :close, volume = :volume WHERE openTime = :openTime AND symbol = :symbol AND timeframe = :timeframe")
    suspend fun updateCandle(openTime: Long, symbol: String, timeframe: String, high: Double, low: Double, close: Double, volume: Double)

    @Query("UPDATE candles SET isClosed = 1 WHERE openTime = :openTime AND symbol = :symbol AND timeframe = :timeframe")
    suspend fun closeCandle(openTime: Long, symbol: String, timeframe: String)
}
