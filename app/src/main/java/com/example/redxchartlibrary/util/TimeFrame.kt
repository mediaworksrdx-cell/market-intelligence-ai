package com.example.redxchartlibrary.util

import java.util.concurrent.TimeUnit

sealed class TimeFrame(val identifier: String, val duration: Long) {
    object M1 : TimeFrame("1m", TimeUnit.MINUTES.toMillis(1))
    object M5 : TimeFrame("5m", TimeUnit.MINUTES.toMillis(5))
    object M15 : TimeFrame("15m", TimeUnit.MINUTES.toMillis(15))
    object M30 : TimeFrame("30m", TimeUnit.MINUTES.toMillis(30))
    object H1 : TimeFrame("1H", TimeUnit.HOURS.toMillis(1))
    object H4 : TimeFrame("4H", TimeUnit.HOURS.toMillis(4))
    object D1 : TimeFrame("1D", TimeUnit.DAYS.toMillis(1))
    object W1 : TimeFrame("1W", TimeUnit.DAYS.toMillis(7))

    companion object {
        fun fromIdentifier(identifier: String): TimeFrame {
            return when (identifier) {
                "1m" -> M1
                "5m" -> M5
                "15m" -> M15
                "30m" -> M30
                "1H" -> H1
                "4H" -> H4
                "1D" -> D1
                "1W" -> W1
                else -> throw IllegalArgumentException("Invalid timeframe identifier: $identifier")
            }
        }
    }
}
