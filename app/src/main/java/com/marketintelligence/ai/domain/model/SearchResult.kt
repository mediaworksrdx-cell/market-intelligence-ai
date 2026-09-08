package com.marketintelligence.ai.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val symbol: String,
    val name: String,
    val type: String // e.g., "Stock" or "Crypto"
)
