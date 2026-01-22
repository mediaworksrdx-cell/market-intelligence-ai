package com.example.marketintelligence.domain.model

// This clean business object now lives in the domain layer.
data class Holding(
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double,
    val investedValue: Double,
    val currentValue: Double,
    val totalPnl: Double,
    val todayPnl: Double,
    val market: MarketType
)
