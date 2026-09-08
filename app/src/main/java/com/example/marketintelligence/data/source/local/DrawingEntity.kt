package com.example.marketintelligence.data.source.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room entity for persisting chart drawings per symbol.
 */
@Entity(
    tableName = "chart_drawings",
    indices = [Index(value = ["symbol"])]
)
data class DrawingEntity(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val toolType: String,       // DrawingToolType.name
    val pointsJson: String,     // JSON array of {timestamp, price} objects
    val color: Long,
    val lineWidth: Float,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * DAO for CRUD operations on chart drawings.
 */
@Dao
interface DrawingDao {

    @Query("SELECT * FROM chart_drawings WHERE symbol = :symbol ORDER BY createdAt ASC")
    fun getDrawingsForSymbol(symbol: String): Flow<List<DrawingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: DrawingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawings(drawings: List<DrawingEntity>)

    @Delete
    suspend fun deleteDrawing(drawing: DrawingEntity)

    @Query("DELETE FROM chart_drawings WHERE symbol = :symbol")
    suspend fun deleteAllForSymbol(symbol: String)

    @Query("DELETE FROM chart_drawings WHERE id = :id")
    suspend fun deleteById(id: String)
}
