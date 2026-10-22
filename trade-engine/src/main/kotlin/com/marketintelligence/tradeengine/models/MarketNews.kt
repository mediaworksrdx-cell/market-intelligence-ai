package com.marketintelligence.tradeengine.models

import kotlinx.serialization.Serializable

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val publishedAt: String,
    val timeAgo: String,
    val category: String, // EQUITIES, COMMODITIES, FOREX, CRYPTO, MACRO
    val market: String,   // INDIA, USA, UAE, GLOBAL
    val sentiment: String,// BULLISH, BEARISH, NEUTRAL
    val tickers: List<String> = emptyList(),
    val isBreaking: Boolean = false
)
