package com.marketintelligence.ai.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marketintelligence.ai.data.model.ExecutedTradeEntity
import com.marketintelligence.redxchartlibrary.data.local.Drawing
import com.marketintelligence.redxchartlibrary.data.local.DrawingDao
import com.marketintelligence.redxchartlibrary.data.local.DrawingTypeConverter

@Database(
    entities = [
        CandleEntity::class,
        WatchlistEntity::class,
        InstrumentEntity::class,
        Drawing::class,
        HoldingEntity::class,
        TransactionEntity::class,
        NotificationEntity::class,
        ExecutedTradeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DrawingTypeConverter::class, DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun candleDao(): CandleDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun drawingDao(): DrawingDao
    abstract fun holdingDao(): HoldingDao
    abstract fun transactionDao(): TransactionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun executedTradeDao(): ExecutedTradeDao

    companion object {
        const val DATABASE_NAME = "market-intelligence-db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
