package com.marketintelligence.tradeengine.util

import java.util.concurrent.TimeUnit

enum class TimeFrame(val value: String, private val millis: Long) {
    ONE_MINUTE("1m", TimeUnit.MINUTES.toMillis(1)),
    FIVE_MINUTES("5m", TimeUnit.MINUTES.toMillis(5)),
    FIFTEEN_MINUTES("15m", TimeUnit.MINUTES.toMillis(15)),
    THIRTY_MINUTES("30m", TimeUnit.MINUTES.toMillis(30)),
    ONE_HOUR("1h", TimeUnit.HOURS.toMillis(1)),
    FOUR_HOURS("4h", TimeUnit.HOURS.toMillis(4)),
    ONE_DAY("1d", TimeUnit.DAYS.toMillis(1)),
    ONE_WEEK("1w", TimeUnit.DAYS.toMillis(7)),
    ONE_MONTH("1M", TimeUnit.DAYS.toMillis(30));

    fun toMillis(): Long = millis
}

fun String.toTimeFrame(): TimeFrame {
    val trimmed = this.trim()
    if (trimmed == "1M" || trimmed.equals("1mo", ignoreCase = true)) return TimeFrame.ONE_MONTH
    if (trimmed == "1m") return TimeFrame.ONE_MINUTE
    val normalized = when (trimmed.lowercase()) {
        "minute", "1m", "1minute" -> "1m"
        "3minute", "3m" -> "1m"
        "5minute", "5m" -> "5m"
        "15minute", "15m" -> "15m"
        "30minute", "30m" -> "30m"
        "60minute", "1h", "60m" -> "1h"
        "4h", "240m", "4hour" -> "4h"
        "day", "1d", "daily" -> "1d"
        "week", "1w", "weekly" -> "1w"
        "month", "1mo", "monthly" -> "1M"
        else -> trimmed
    }
    return TimeFrame.values().firstOrNull { it.value.equals(normalized, ignoreCase = true) } 
        ?: TimeFrame.ONE_DAY
}
