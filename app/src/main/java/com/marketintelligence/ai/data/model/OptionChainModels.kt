
package com.marketintelligence.ai.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OptionContract(
    @SerialName("strike")
    val strike: Double,
    @SerialName("last_price")
    val lastPrice: Double,
    @SerialName("open_interest")
    val openInterest: Double,
    @SerialName("implied_volatility")
    val impliedVolatility: Double,
    @SerialName("volume")
    val volume: Int
)

@Serializable
data class OptionChainData(
    @SerialName("underlying_price")
    val underlyingPrice: Double,
    @SerialName("calls")
    val calls: List<OptionContract>,
    @SerialName("puts")
    val puts: List<OptionContract>
)
