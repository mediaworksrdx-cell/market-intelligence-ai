package com.example.redxchartlibrary.model

import kotlinx.serialization.Serializable

@Serializable
data class Tick(
    val timestamp: Long,
    val price: Float
)