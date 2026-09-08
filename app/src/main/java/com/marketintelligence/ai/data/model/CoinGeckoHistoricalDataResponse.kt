package com.marketintelligence.ai.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class CoinGeckoHistoricalDataResponse(
    val prices: List<List<Double>>
)
