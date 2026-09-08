package com.marketintelligence.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoinGeckoSearchResponse(
    val coins: List<Coin>
)

@Serializable
data class Coin(
    val id: String,
    val name: String,
    val symbol: String
)
