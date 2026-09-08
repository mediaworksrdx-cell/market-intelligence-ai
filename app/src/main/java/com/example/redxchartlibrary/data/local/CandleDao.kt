package com.example.redxchartlibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.redxchartlibrary.model.Candle
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<Candle>)

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeframe = :timeframe ORDER BY openTime ASC")
    fun getCandles(symbol: String, timeframe: String): Flow<List<Candle>>
    
    @Query("DELETE FROM candles WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun clearCandles(symbol: String, timeframe: String)
}
