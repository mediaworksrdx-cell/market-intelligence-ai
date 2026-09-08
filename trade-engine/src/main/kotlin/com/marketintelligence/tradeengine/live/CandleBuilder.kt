package com.marketintelligence.tradeengine.live

import com.marketintelligence.tradeengine.models.Candle
import com.zerodhatech.models.Tick
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class CandleBuilder(val symbol: String, val timeframe: String) {

    private val logger = LoggerFactory.getLogger(CandleBuilder::class.java)
    @Volatile
    private var currentCandle: Candle? = null
    private var lastCumulativeVolume: Long = 0L

    private val timeframeMillis = timeframeToMillis(timeframe)

    @Synchronized
    fun addTick(tick: Tick): Candle? {
        val tickTimestamp = tick.tickTimestamp?.time ?: System.currentTimeMillis()
        val currentCumVolume = tick.volumeTradedToday
        val deltaVolume = if (lastCumulativeVolume > 0L && currentCumVolume >= lastCumulativeVolume) {
            (currentCumVolume - lastCumulativeVolume).toDouble()
        } else {
            0.0
        }
        lastCumulativeVolume = currentCumVolume

        val active = currentCandle
        if (active == null) {
            currentCandle = createNewCandle(tick, tickTimestamp, deltaVolume)
            return null
        }

        return if (tickTimestamp >= active.closeTime) {
            val finishedCandle = active.copy(isClosed = true)
            currentCandle = createNewCandle(tick, tickTimestamp, deltaVolume)
            finishedCandle
        } else {
            currentCandle = active.copy(
                high = maxOf(active.high, tick.lastTradedPrice),
                low = minOf(active.low, tick.lastTradedPrice),
                close = tick.lastTradedPrice,
                volume = active.volume + deltaVolume
            )
            null
        }
    }

    fun getCurrentCandle(): Candle? {
        return currentCandle
    }

    private fun createNewCandle(tick: Tick, tickTimestamp: Long, initialVolume: Double): Candle {
        val candleTimestamp = (tickTimestamp / timeframeMillis) * timeframeMillis
        return Candle(
            symbol = symbol,
            timeframe = timeframe,
            openTime = candleTimestamp,
            open = tick.lastTradedPrice,
            high = tick.lastTradedPrice,
            low = tick.lastTradedPrice,
            close = tick.lastTradedPrice,
            volume = initialVolume,
            closeTime = candleTimestamp + timeframeMillis,
            isClosed = false
        )
    }

    private fun timeframeToMillis(timeframe: String): Long {
        return when (timeframe.lowercase()) {
            "minute", "1m" -> TimeUnit.MINUTES.toMillis(1)
            "3minute", "3m" -> TimeUnit.MINUTES.toMillis(3)
            "5minute", "5m" -> TimeUnit.MINUTES.toMillis(5)
            "10minute", "10m" -> TimeUnit.MINUTES.toMillis(10)
            "15minute", "15m" -> TimeUnit.MINUTES.toMillis(15)
            "30minute", "30m" -> TimeUnit.MINUTES.toMillis(30)
            "60minute", "1h" -> TimeUnit.HOURS.toMillis(1)
            "day", "1d" -> TimeUnit.DAYS.toMillis(1)
            else -> TimeUnit.MINUTES.toMillis(1) // Default to 1 minute
        }
    }
}
