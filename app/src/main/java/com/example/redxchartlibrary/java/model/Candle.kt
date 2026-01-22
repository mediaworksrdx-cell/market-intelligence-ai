package com.example.redxchartlibrary.model

import kotlinx.serialization.Serializable

@Serializable
data class Candle(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long
)