package com.marketintelligence.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val symbol: String,
    val name: String,
    val type: String
)
