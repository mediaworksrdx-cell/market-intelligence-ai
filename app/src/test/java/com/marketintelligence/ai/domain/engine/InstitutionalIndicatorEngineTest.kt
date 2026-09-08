package com.marketintelligence.ai.domain.engine

import com.marketintelligence.tradeengine.models.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstitutionalIndicatorEngineTest {

    private val engine = InstitutionalIndicatorEngine()

    private fun generateCandleSeries(prices: List<Double>, volume: Double = 1000.0): List<Candle> {
        val baseTime = 1700000000000L
        return prices.mapIndexed { idx, p ->
            Candle(
                symbol = "TEST",
                timeframe = "1m",
                openTime = baseTime + (idx * 60000L),
                open = p,
                high = p + 2.0,
                low = p - 2.0,
                close = p,
                volume = volume,
                closeTime = baseTime + ((idx + 1) * 60000L)
            )
        }
    }

    @Test
    fun testEMAComputation() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0)
        val candles = generateCandleSeries(prices)

        val emaMap = engine.computeEMA(candles, period = 5)
        val lastTimestamp = candles.last().openTime
        val ema = emaMap[lastTimestamp]

        assertNotNull(ema)
        assertTrue(ema!! > 15.0 && ema < 20.0)
    }

    @Test
    fun testBollingerBandsComputation() {
        val prices = (1..30).map { 100.0 + (it % 5) }
        val candles = generateCandleSeries(prices)

        val bbMap = engine.computeBollingerBands(candles, period = 20, numStdDev = 2.0)
        val lastTimestamp = candles.last().openTime
        val bb = bbMap[lastTimestamp]

        assertNotNull(bb)
        assertTrue(bb!!.upperBand > bb.middleSma)
        assertTrue(bb.lowerBand < bb.middleSma)
    }

    @Test
    fun testWildersRSIComputation() {
        val bullPrices = (1..25).map { 100.0 + (it * 2.0) }
        val bullCandles = generateCandleSeries(bullPrices)
        val bullRsi = engine.computeWildersRSI(bullCandles, period = 14)
        val lastBullRsi = bullRsi[bullCandles.last().openTime]

        assertNotNull(lastBullRsi)
        assertTrue(lastBullRsi!! > 75.0)

        val bearPrices = (1..25).map { 200.0 - (it * 2.0) }
        val bearCandles = generateCandleSeries(bearPrices)
        val bearRsi = engine.computeWildersRSI(bearCandles, period = 14)
        val lastBearRsi = bearRsi[bearCandles.last().openTime]

        assertNotNull(lastBearRsi)
        assertTrue(lastBearRsi!! < 25.0)
    }

    @Test
    fun testMACDComputation() {
        val prices = (1..50).map { 100.0 + (it * 1.5) }
        val candles = generateCandleSeries(prices)

        val macdMap = engine.computeMACD(candles, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9)
        val lastMacd = macdMap[candles.last().openTime]

        assertNotNull(lastMacd)
        assertTrue(lastMacd!!.macdLine > 0.0)
    }

    @Test
    fun testVWAPComputation() {
        val candles = listOf(
            Candle(symbol = "TEST", timeframe = "1m", openTime = 1000L, open = 100.0, high = 105.0, low = 95.0, close = 100.0, volume = 1000.0, closeTime = 2000L),
            Candle(symbol = "TEST", timeframe = "1m", openTime = 2000L, open = 110.0, high = 115.0, low = 105.0, close = 110.0, volume = 2000.0, closeTime = 3000L)
        )
        val vwapMap = engine.computeVWAP(candles)

        val expectedVwap = 320000.0 / 3000.0
        val actualVwap = vwapMap[2000L]

        assertNotNull(actualVwap)
        assertEquals(expectedVwap, actualVwap!!, 0.001)
    }

    @Test
    fun testHeikinAshiConversion() {
        val candles = listOf(
            Candle(symbol = "TEST", timeframe = "1m", openTime = 1000L, open = 100.0, high = 110.0, low = 90.0, close = 105.0, volume = 500.0, closeTime = 2000L),
            Candle(symbol = "TEST", timeframe = "1m", openTime = 2000L, open = 105.0, high = 115.0, low = 100.0, close = 112.0, volume = 600.0, closeTime = 3000L)
        )
        val haList = engine.computeHeikinAshi(candles)

        assertEquals(2, haList.size)
        assertEquals(108.0, haList[1].close, 0.001)
    }
}
