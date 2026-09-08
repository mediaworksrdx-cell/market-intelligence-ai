package com.example.tradeengine.live

import com.example.tradeengine.models.Candle
import com.zerodhatech.models.Tick
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class CandleBuilder(val symbol: String, val timeframe: String) {

    private val logger = LoggerFactory.getLogger(CandleBuilder::class.java)
    private val ticks = ConcurrentLinkedQueue<Tick>()
    private var currentCandle: Candle? = null

    private val timeframeMillis = timeframeToMillis(timeframe)

    fun addTick(tick: Tick): Candle? {
        val tickTimestamp = tick.tickTimestamp.time

        if (currentCandle == null) {
            currentCandle = createNewCandle(tick)
            return null
        }

        val candleTimestamp = currentCandle!!.openTime * 1000

        return if (tickTimestamp >= candleTimestamp + timeframeMillis) {
            val finishedCandle = currentCandle!!.copy(isClosed = true)
            currentCandle = createNewCandle(tick)
            finishedCandle
        } else {
            currentCandle = currentCandle!!.copy(
                high = maxOf(currentCandle!!.high, tick.lastTradedPrice),
                low = minOf(currentCandle!!.low, tick.lastTradedPrice),
                close = tick.lastTradedPrice,
                volume = currentCandle!!.volume + (tick.volumeTradedToday - (ticks.last()?.volumeTradedToday ?: 0L)).toDouble()
            )
            null
        }
    }

    fun getCurrentCandle(): Candle? {
        return currentCandle
    }

    private fun createNewCandle(tick: Tick): Candle {
        val candleTimestamp = (tick.tickTimestamp.time / timeframeMillis) * timeframeMillis
        return Candle(
            symbol = symbol,
            timeframe = timeframe,
            openTime = candleTimestamp / 1000,
            open = tick.lastTradedPrice,
            high = tick.lastTradedPrice,
            low = tick.lastTradedPrice,
            close = tick.lastTradedPrice,
            volume = tick.volumeTradedToday.toDouble(),
            closeTime = (candleTimestamp + timeframeMillis - 1) / 1000,
            isClosed = false
        )
    }

    private fun timeframeToMillis(timeframe: String): Long {
        return when (timeframe.lowercase()) {
            "minute" -> TimeUnit.MINUTES.toMillis(1)
            "3minute" -> TimeUnit.MINUTES.toMillis(3)
            "5minute" -> TimeUnit.MINUTES.toMillis(5)
            "10minute" -> TimeUnit.MINUTES.toMillis(10)
            "15minute" -> TimeUnit.MINUTES.toMillis(15)
            "30minute" -> TimeUnit.MINUTES.toMillis(30)
            "60minute" -> TimeUnit.MINUTES.toMillis(60)
            else -> TimeUnit.MINUTES.toMillis(1) // Default to 1 minute
        }
    }
}
