package com.marketintelligence.redxchartlibrary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.marketintelligence.redxchartlibrary.model.Candle

@Database(entities = [Drawing::class, Candle::class], version = 2, exportSchema = false)
@TypeConverters(DrawingTypeConverter::class)
abstract class ChartDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao
    abstract fun candleDao(): CandleDao

    companion object {
        @Volatile
        private var INSTANCE: ChartDatabase? = null

        fun getDatabase(context: Context): ChartDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChartDatabase::class.java,
                    "chart_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
