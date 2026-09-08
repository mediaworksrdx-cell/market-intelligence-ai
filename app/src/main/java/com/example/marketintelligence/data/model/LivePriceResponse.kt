package com.example.marketintelligence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LivePriceResponse(
    val instrumentToken: Long,
    val symbol: String,
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long
)
