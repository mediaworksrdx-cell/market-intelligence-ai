package com.marketintelligence.redxchartlibrary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drawings")
data class DrawingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symbol: String,
    val drawingData: String // Store drawing data as JSON or other serialized format
)
