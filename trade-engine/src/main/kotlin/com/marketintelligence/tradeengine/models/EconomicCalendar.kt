package com.marketintelligence.tradeengine.models

import kotlinx.serialization.Serializable

@Serializable
data class EconomicEvent(
    val id: String,
    val timestamp: Long,
    val timeStr: String,
    val dateStr: String,
    val country: String,    // INDIA, USA, EUROPE, UAE, GLOBAL
    val countryCode: String,
    val flag: String,
    val event: String,
    val period: String? = null,
    val impact: String,     // HIGH, MEDIUM, LOW
    val actual: String? = null,
    val forecast: String? = null,
    val previous: String? = null,
    val unit: String? = null,
    val isBeat: Boolean? = null,
    val description: String? = null
)
