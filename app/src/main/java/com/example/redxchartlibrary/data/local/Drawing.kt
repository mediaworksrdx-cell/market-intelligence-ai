package com.example.redxchartlibrary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "drawings")
data class Drawing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tool: DrawingTool
)

// We need to make these serializable for Kotlinx Serialization
@kotlinx.serialization.Serializable
sealed class DrawingTool {
    @kotlinx.serialization.Serializable
    data class Trendline(val start: AnchorPoint, val end: AnchorPoint) : DrawingTool()
    @kotlinx.serialization.Serializable
    data class FibonacciRetracement(val start: AnchorPoint, val end: AnchorPoint) : DrawingTool()
}

@kotlinx.serialization.Serializable
data class AnchorPoint(
    val timestamp: Long,
    val price: Float
)

class DrawingTypeConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDrawingTool(tool: DrawingTool): String {
        return json.encodeToString(tool)
    }

    @TypeConverter
    fun toDrawingTool(jsonString: String): DrawingTool {
        return json.decodeFromString(jsonString)
    }
}
