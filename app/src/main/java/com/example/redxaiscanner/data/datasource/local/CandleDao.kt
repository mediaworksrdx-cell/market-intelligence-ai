package com.example.redxaiscanner.data.datasource.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {

    @Upsert
    suspend fun upsertAll(candles: List<CandleEntity>)

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp DESC")
    fun getCandles(symbol: String, timeframe: String): Flow<List<CandleEntity>>

    @Query("DELETE FROM candles WHERE symbol = :symbol AND timeframe = :timeframe AND timestamp NOT IN (SELECT timestamp FROM candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun trimCache(symbol: String, timeframe: String, limit: Int)
}