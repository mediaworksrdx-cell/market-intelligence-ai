package com.example.redxchartlibrary.model

import kotlinx.serialization.Serializable

@Serializable
data class Tick(
    val symbol: String,
    val price: Double = 0.0,
    val volume: Double = 0.0,
    val timestamp: Long
)
