package com.marketintelligence.ai.data.source.local

import androidx.room.*
import com.marketintelligence.ai.data.model.ExecutedTradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutedTradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: ExecutedTradeEntity)

    @Query("SELECT * FROM executed_trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<ExecutedTradeEntity>>

    @Query("SELECT * FROM executed_trades WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getTradesBySymbol(symbol: String): Flow<List<ExecutedTradeEntity>>

    @Query("DELETE FROM executed_trades")
    suspend fun deleteAll()
}
