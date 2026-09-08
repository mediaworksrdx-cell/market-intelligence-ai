package com.marketintelligence.ai.data.source.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "timeframe", "openTime"],
    indices = [Index(value = ["symbol", "timeframe", "openTime"])]
)
data class CandleEntity(
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
