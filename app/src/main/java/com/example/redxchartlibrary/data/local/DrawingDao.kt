package com.example.redxchartlibrary.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawings")
    fun getAllDrawings(): Flow<List<Drawing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: Drawing)

    @Delete
    suspend fun deleteDrawing(drawing: Drawing)
}
