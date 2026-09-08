package com.marketintelligence.tradeengine.util

import java.util.concurrent.TimeUnit

enum class TimeFrame(val value: String, private val millis: Long) {
    ONE_MINUTE("1m", TimeUnit.MINUTES.toMillis(1)),
    FIVE_MINUTES("5m", TimeUnit.MINUTES.toMillis(5)),
    FIFTEEN_MINUTES("15m", TimeUnit.MINUTES.toMillis(15)),
    ONE_HOUR("1h", TimeUnit.HOURS.toMillis(1)),
    FOUR_HOURS("4h", TimeUnit.HOURS.toMillis(4)),
    ONE_DAY("1d", TimeUnit.DAYS.toMillis(1));

    fun toMillis(): Long = millis
}

fun String.toTimeFrame(): TimeFrame {
    val normalized = when (this.lowercase().trim()) {
        "minute", "1m", "1minute" -> "1m"
        "3minute", "3m" -> "3m"
        "5minute", "5m" -> "5m"
        "15minute", "15m" -> "15m"
        "60minute", "1h", "60m" -> "1h"
        "4h", "240m", "4hour" -> "4h"
        "day", "1d", "daily" -> "1d"
        else -> this
    }
    return TimeFrame.values().firstOrNull { it.value.equals(normalized, ignoreCase = true) } 
        ?: TimeFrame.ONE_MINUTE // Fallback safely to 1m instead of throwing runtime crash
}
