package com.example.marketintelligence.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SubscribeRequest(
    val symbol: String,
    val instrumentToken: Long
)
