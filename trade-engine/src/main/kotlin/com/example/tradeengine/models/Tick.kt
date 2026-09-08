package com.example.tradeengine.models

import kotlinx.serialization.Serializable

@Serializable
data class Tick(
    val symbol: String,
    val price: Double,
    val volume: Double,
    val timestamp: Long
)
