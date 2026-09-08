package com.example.tradeengine.util

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
    return TimeFrame.values().firstOrNull { it.value == this } 
        ?: throw IllegalArgumentException("Unsupported timeframe: $this")
}
