package com.example.marketintelligence.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.redxchartlibrary.data.local.Drawing
import com.example.redxchartlibrary.data.local.DrawingDao
import com.example.redxchartlibrary.data.local.DrawingTypeConverter

@Database(entities = [CandleEntity::class, WatchlistEntity::class, InstrumentEntity::class, Drawing::class, DrawingEntity::class], version = 2, exportSchema = false)
@TypeConverters(DrawingTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun candleDao(): CandleDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun drawingDao(): DrawingDao
    abstract fun chartDrawingDao(): com.example.marketintelligence.data.source.local.DrawingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "market_intelligence_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
