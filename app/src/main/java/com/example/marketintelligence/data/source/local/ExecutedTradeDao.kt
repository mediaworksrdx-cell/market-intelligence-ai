package com.example.marketintelligence.data.source.local

import androidx.room.*
import com.example.marketintelligence.data.model.ExecutedTradeEntity

@Dao
interface ExecutedTradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: ExecutedTradeEntity)
}
