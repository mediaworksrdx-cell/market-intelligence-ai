package com.example.redxchartlibrary.data.local

import kotlinx.coroutines.flow.Flow

class DrawingRepository(private val drawingDao: DrawingDao) {
    fun getAllDrawings(): Flow<List<Drawing>> = drawingDao.getAllDrawings()
    suspend fun insertOrUpdate(drawing: Drawing) = drawingDao.insertDrawing(drawing)
}
