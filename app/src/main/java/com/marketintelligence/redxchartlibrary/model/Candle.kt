package com.marketintelligence.redxchartlibrary.model

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "timeframe", "openTime"],
    indices = [Index(value = ["symbol", "timeframe"])]
)
data class Candle(
    val symbol: String,
    val timeframe: String,
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val isClosed: Boolean = false
)
