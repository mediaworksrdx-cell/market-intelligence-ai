package com.marketintelligence.tradeengine.api

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
data class SubscribeRequest(
    val instrumentToken: Long,
    val symbol: String
)

@Serializable
data class MacroData(val symbol: String, val value: String)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class ApiSearchResult(val symbol: String, val name: String, val type: String)
