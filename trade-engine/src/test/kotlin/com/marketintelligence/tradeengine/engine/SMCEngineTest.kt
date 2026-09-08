package com.marketintelligence.tradeengine.engine

import com.marketintelligence.tradeengine.models.Candle
import org.junit.Assert.*
import org.junit.Test

class SMCEngineTest {

    @Test
    fun testSMCEngineDoesNotThrowArithmeticExceptionOnRepeatingDecimals() {
        val engine = SMCEngine()
        val now = System.currentTimeMillis()
        val step = 60_000L

        val candles = (0 until 30).map { i ->
            val basePrice = 100.0 + (i % 5) * 1.333333333333333
            Candle(
                symbol = "NIFTY",
                timeframe = "1m",
                openTime = now + i * step,
                open = basePrice,
                high = basePrice + 2.11111111111111,
                low = basePrice - 1.77777777777777,
                close = basePrice + 0.55555555555555,
                volume = 1500.0 + i * 33.333,
                closeTime = now + (i + 1) * step,
                isClosed = true
            )
        }

        val result = engine.analyze(candles)
        assertNotNull(result)
        assertNotNull(result.bias)
        assertNotNull(result.explanation)
    }

    @Test
    fun testSMCEngineWithInsufficientCandlesReturnsRanging() {
        val engine = SMCEngine()
        val candles = listOf(
            Candle("BTC", "1m", 1000L, 100.0, 105.0, 95.0, 102.0, 50.0, 2000L, true)
        )
        val result = engine.analyze(candles)
        assertEquals(MarketBias.RANGING, result.bias)
        assertTrue(result.swingPoints.isEmpty())
    }
}
