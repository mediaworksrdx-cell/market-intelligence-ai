package com.example.marketintelligence.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.marketintelligence.data.model.ExecutedTradeEntity

@Database(
    entities = [
        NotificationEntity::class, 
        TransactionEntity::class, 
        HoldingEntity::class,
        ExecutedTradeEntity::class 
    ], 
    version = 5, 
    exportSchema = false
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun transactionDao(): TransactionDao
    abstract fun holdingDao(): HoldingDao
    abstract fun executedTradeDao(): ExecutedTradeDao 
}
