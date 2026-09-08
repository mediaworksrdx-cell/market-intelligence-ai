package com.example.marketintelligence.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Insert
    suspend fun insert(notification: NotificationEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Insert
    suspend fun insert(transaction: TransactionEntity)
}

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings")
    fun getAllHoldings(): Flow<List<HoldingEntity>>
    
    @Insert
    suspend fun insert(holding: HoldingEntity)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist WHERE type = 'STOCK'")
    fun getStocks(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE type = 'INDEX'")
    fun getIndices(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE type = 'CRYPTO'")
    fun getCryptos(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchlistEntity>)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE type = :type")
    suspend fun getCountByType(type: String): Int
}
