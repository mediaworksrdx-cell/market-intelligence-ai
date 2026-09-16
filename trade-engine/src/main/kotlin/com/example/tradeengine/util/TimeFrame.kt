package com.example.tradeengine.util

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
    return TimeFrame.values().firstOrNull { it.value.equals(trimmed, ignoreCase = true) } 
        ?: when {
            trimmed.endsWith("m", ignoreCase = true) -> {
                when (trimmed.dropLast(1).toIntOrNull()) {
                    1 -> TimeFrame.ONE_MINUTE
                    5 -> TimeFrame.FIVE_MINUTES
                    15 -> TimeFrame.FIFTEEN_MINUTES
                    30 -> TimeFrame.THIRTY_MINUTES
                    else -> TimeFrame.ONE_MINUTE
                }
            }
            trimmed.endsWith("h", ignoreCase = true) -> {
                if ((trimmed.dropLast(1).toIntOrNull() ?: 1) >= 4) TimeFrame.FOUR_HOURS else TimeFrame.ONE_HOUR
            }
            trimmed.endsWith("w", ignoreCase = true) -> TimeFrame.ONE_WEEK
            trimmed.endsWith("d", ignoreCase = true) -> TimeFrame.ONE_DAY
            else -> TimeFrame.ONE_DAY
        }
}
