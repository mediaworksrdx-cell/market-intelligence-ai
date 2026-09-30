package com.example.marketintelligence.data.source.remote

import javax.inject.Inject

class TwelveDataApiService @Inject constructor() {
    suspend fun getQuote(symbol: String): QuoteDto {
        return QuoteDto(symbol, 0.0, 0.0, 0.0, 0.0, "0")
    }
}

data class QuoteDto(
    val symbol: String,
    val price: Double,
    val open: Double,
    val change: Double,
    val percentChange: Double,
    val volume: String
)
