package com.example.marketintelligence.data.repository

import com.example.marketintelligence.data.source.local.DrawingDao
import com.example.marketintelligence.data.source.local.DrawingEntity
import com.example.marketintelligence.domain.chart.ChartPoint
import com.example.marketintelligence.domain.chart.DrawingData
import com.example.marketintelligence.domain.chart.DrawingToolType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SerializablePoint(val timestamp: Long, val price: Double)

/**
 * Repository for persisting chart drawings per symbol.
 * Handles conversion between domain DrawingData and Room DrawingEntity.
 */
@Singleton
class DrawingRepository @Inject constructor(
    private val drawingDao: DrawingDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getDrawingsForSymbol(symbol: String): Flow<List<DrawingData>> {
        return drawingDao.getDrawingsForSymbol(symbol).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    val points = json.decodeFromString<List<SerializablePoint>>(entity.pointsJson)
                        .map { ChartPoint(it.timestamp, it.price) }
                    DrawingData(
                        id = entity.id,
                        toolType = DrawingToolType.valueOf(entity.toolType),
                        points = points,
                        color = entity.color,
                        lineWidth = entity.lineWidth,
                        isComplete = true
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun saveDrawing(symbol: String, drawing: DrawingData) {
        val pointsJson = json.encodeToString(
            drawing.points.map { SerializablePoint(it.timestamp, it.price) }
        )
        drawingDao.insertDrawing(
            DrawingEntity(
                id = drawing.id,
                symbol = symbol,
                toolType = drawing.toolType.name,
                pointsJson = pointsJson,
                color = drawing.color,
                lineWidth = drawing.lineWidth
            )
        )
    }

    suspend fun saveDrawings(symbol: String, drawings: List<DrawingData>) {
        val entities = drawings.map { drawing ->
            val pointsJson = json.encodeToString(
                drawing.points.map { SerializablePoint(it.timestamp, it.price) }
            )
            DrawingEntity(
                id = drawing.id,
                symbol = symbol,
                toolType = drawing.toolType.name,
                pointsJson = pointsJson,
                color = drawing.color,
                lineWidth = drawing.lineWidth
            )
        }
        drawingDao.insertDrawings(entities)
    }

    suspend fun deleteDrawing(id: String) {
        drawingDao.deleteById(id)
    }

    suspend fun clearDrawingsForSymbol(symbol: String) {
        drawingDao.deleteAllForSymbol(symbol)
    }
}
