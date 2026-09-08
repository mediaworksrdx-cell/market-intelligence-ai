package com.example.redxchartlibrary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings WHERE symbol = :symbol AND timeframe = :timeframe")
    fun getDrawingsForChart(symbol: String, timeframe: String): Flow<List<Drawing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: Drawing): Long

    @Update
    suspend fun updateDrawing(drawing: Drawing)

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun deleteDrawing(id: Int)
}
