package com.example.marketintelligence.data.source.local

import androidx.room.Dao
import androidx.room.Insert
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
