package com.example.redxaiscanner.domain.model

import java.math.BigDecimal

data class Candle(
    val timestamp: Long,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal
)