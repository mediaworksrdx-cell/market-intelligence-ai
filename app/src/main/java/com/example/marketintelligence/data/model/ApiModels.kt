package com.example.marketintelligence.data.model

import com.example.marketintelligence.data.model.OptionContract
import kotlinx.serialization.Serializable

@Serializable
data class LivePrice(
    val instrumentToken: Long,
    val symbol: String,
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long
)

@Serializable
data class IndexDataResponse(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
)

@Serializable
data class StockDataResponse(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double
)

@Serializable
data class FnoData(
    val symbol: String,
    val spotPrice: Double,
    val futures: List<FuturesContract>,
    val options: List<OptionContract>
)

@Serializable
data class FuturesContract(
    val expiry: String,
    val ltp: Double,
    val changePercent: Double
)
