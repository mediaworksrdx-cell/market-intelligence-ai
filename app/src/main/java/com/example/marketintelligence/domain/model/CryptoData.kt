package com.example.marketintelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CryptoData(
    val id: String,
    val symbol: String,
    val name: String,
    val price: Double = 0.0,
    val changePercent: Double = 0.0,
    val marketCap: Long = 0L
)
