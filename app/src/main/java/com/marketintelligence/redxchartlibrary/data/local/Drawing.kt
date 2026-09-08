package com.marketintelligence.redxchartlibrary.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.SerializersModule

@Entity(tableName = "drawings")
data class Drawing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val timeframe: String,
    val tool: DrawingTool,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

@kotlinx.serialization.Serializable
sealed class DrawingTool {
    @kotlinx.serialization.Serializable
    data class Trendline(val start: AnchorPoint, val end: AnchorPoint) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class HorizontalLine(val anchor: AnchorPoint) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class Ray(val start: AnchorPoint, val end: AnchorPoint) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class FibonacciRetracement(val start: AnchorPoint, val end: AnchorPoint) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class Rectangle(
        val start: AnchorPoint,
        val end: AnchorPoint,
        val isFilled: Boolean = false
    ) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class OrderBlock(
        val start: AnchorPoint,
        val end: AnchorPoint,
        val type: String = "BULLISH"
    ) : DrawingTool()

    @kotlinx.serialization.Serializable
    data class Text(val position: AnchorPoint, val content: String) : DrawingTool()
}

@kotlinx.serialization.Serializable
data class AnchorPoint(
    val timestamp: Long,
    val price: Float
)

class DrawingTypeConverter {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(DrawingTool::class) {
                subclass(DrawingTool.Trendline::class, DrawingTool.Trendline.serializer())
                subclass(DrawingTool.HorizontalLine::class, DrawingTool.HorizontalLine.serializer())
                subclass(DrawingTool.Ray::class, DrawingTool.Ray.serializer())
                subclass(DrawingTool.FibonacciRetracement::class, DrawingTool.FibonacciRetracement.serializer())
                subclass(DrawingTool.Rectangle::class, DrawingTool.Rectangle.serializer())
                subclass(DrawingTool.OrderBlock::class, DrawingTool.OrderBlock.serializer())
                subclass(DrawingTool.Text::class, DrawingTool.Text.serializer())
            }
        }
    }

    @TypeConverter
    fun fromDrawingTool(tool: DrawingTool): String {
        return json.encodeToString(tool)
    }

    @TypeConverter
    fun toDrawingTool(jsonString: String): DrawingTool {
        return json.decodeFromString(jsonString)
    }
}
