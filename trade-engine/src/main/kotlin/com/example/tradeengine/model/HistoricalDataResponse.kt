package com.example.tradeengine.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoricalDataResponse(
    val prices: List<List<String>>
)
