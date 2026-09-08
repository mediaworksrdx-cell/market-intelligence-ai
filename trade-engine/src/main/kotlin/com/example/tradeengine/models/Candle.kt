package com.example.tradeengine.models

import kotlinx.serialization.Serializable

@Serializable
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
