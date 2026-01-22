package com.example.redxfnoscanner.data

import kotlinx.serialization.Serializable

@Serializable
data class SpotPrice(
    val symbol: String,
    val price: Double,
    val timestamp: Long
)

@Serializable
data class Option(
    val type: String, // "CE" or "PE"
    val strikePrice: Double,
    val lastTradedPrice: Double,
    val priceChange: Double,
    val openInterest: Int,
    val changeInOpenInterest: Int,
    val impliedVolatility: Double
)

@Serializable
data class OptionChain(
    val expiryDate: String,
    val options: List<Option>
)

@Serializable
data class FnoData(
    val spotPrice: SpotPrice,
    val optionChains: List<OptionChain>
)
