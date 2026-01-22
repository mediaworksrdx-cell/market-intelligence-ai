package com.example.redxaiscanner.data.datasource.local

import androidx.room.Entity
import java.math.BigDecimal

@Entity(tableName = "candles", primaryKeys = ["symbol", "timeframe", "timestamp"])
data class CandleEntity(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)