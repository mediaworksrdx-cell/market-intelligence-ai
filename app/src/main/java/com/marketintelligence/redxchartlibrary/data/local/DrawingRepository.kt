package com.marketintelligence.redxchartlibrary.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawingRepository @Inject constructor(private val drawingDao: DrawingDao) {

    fun getDrawingsForChart(symbol: String, timeframe: String): Flow<List<Drawing>> {
        return drawingDao.getDrawingsForChart(symbol, timeframe)
    }

    suspend fun insertDrawing(drawing: Drawing): Long {
        return drawingDao.insertDrawing(drawing)
    }

    suspend fun updateDrawing(drawing: Drawing) {
        drawingDao.updateDrawing(drawing)
    }

    suspend fun deleteDrawing(id: Int) {
        drawingDao.deleteDrawing(id)
    }
}
