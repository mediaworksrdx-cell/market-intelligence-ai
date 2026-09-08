package com.marketintelligence.ai.domain.engine

import com.marketintelligence.tradeengine.models.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardenedSMCEngineTest {

    private val engine = HardenedSMCEngine()

    @Test
    fun testBullishFVGWithATRFilter() {
        val candles = mutableListOf<Candle>()
        val base = 100.0

        for (i in 0..20) {
            candles.add(
                Candle(
                    symbol = "TEST",
                    timeframe = "1m",
                    openTime = 1000L + (i * 60000L),
                    open = base + (i * 0.1),
                    high = base + (i * 0.1) + 1.0,
                    low = base + (i * 0.1) - 1.0,
                    close = base + (i * 0.1),
                    volume = 1000.0,
                    closeTime = 1000L + ((i + 1) * 60000L)
                )
            )
        }

        val t = candles.last().closeTime
        val c1 = Candle(symbol = "TEST", timeframe = "1m", openTime = t + 60000L, open = 102.0, high = 105.0, low = 101.0, close = 104.0, volume = 1000.0, closeTime = t + 120000L)
        val c2 = Candle(symbol = "TEST", timeframe = "1m", openTime = t + 120000L, open = 104.0, high = 115.0, low = 103.5, close = 114.0, volume = 5000.0, closeTime = t + 180000L)
        val c3 = Candle(symbol = "TEST", timeframe = "1m", openTime = t + 180000L, open = 114.0, high = 118.0, low = 110.0, close = 116.0, volume = 2000.0, closeTime = t + 240000L)

        candles.addAll(listOf(c1, c2, c3))

        val analysis = engine.analyze(candles)

        assertNotNull(analysis)
        assertTrue(analysis.fairValueGaps.isNotEmpty())
        val fvg = analysis.fairValueGaps.firstOrNull { it.direction == SMCDirection.BULLISH }
        assertNotNull(fvg)
        assertEquals(110.0, fvg!!.top, 0.01)
        assertEquals(105.0, fvg.bottom, 0.01)
    }

    @Test
    fun testOrderBlockVolumeConfirmation() {
        val candles = mutableListOf<Candle>()
        val base = 200.0

        for (i in 0..20) {
            candles.add(
                Candle(
                    symbol = "TEST",
                    timeframe = "1m",
                    openTime = 1000L + (i * 60000L),
                    open = base + (i * 0.1),
                    high = base + (i * 0.1) + 1.0,
                    low = base + (i * 0.1) - 1.0,
                    close = base + (i * 0.1),
                    volume = 1000.0,
                    closeTime = 1000L + ((i + 1) * 60000L)
                )
            )
        }

        val t = candles.last().closeTime
        val bearCandle = Candle(symbol = "TEST", timeframe = "1m", openTime = t + 60000L, open = 205.0, high = 206.0, low = 200.0, close = 201.0, volume = 1000.0, closeTime = t + 120000L)
        val breakoutCandle = Candle(symbol = "TEST", timeframe = "1m", openTime = t + 120000L, open = 201.0, high = 215.0, low = 201.0, close = 214.0, volume = 4000.0, closeTime = t + 180000L)

        candles.addAll(listOf(bearCandle, breakoutCandle))

        val analysis = engine.analyze(candles)

        assertNotNull(analysis)
        val ob = analysis.orderBlocks.firstOrNull { it.direction == SMCDirection.BULLISH }
        assertNotNull(ob)
        assertEquals(206.0, ob!!.top, 0.01)
        assertEquals(200.0, ob.bottom, 0.01)
    }
}
