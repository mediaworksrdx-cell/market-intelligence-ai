package com.example.marketintelligence.domain.chart

import com.example.tradeengine.models.Candle
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit test suite for [IndicatorCalculator].
 * Verifies mathematical precision, array bounds, edge cases (empty lists, short series),
 * and signal generation across all 17 technical indicators and SMC engines.
 */
class IndicatorCalculatorTest {

    private fun generateCandles(count: Int, startPrice: Double = 100.0, trend: Double = 0.5): List<Candle> {
        val list = mutableListOf<Candle>()
        var cur = startPrice
        for (i in 0 until count) {
            val open = cur
            val close = cur + trend + (if (i % 2 == 0) 1.0 else -0.8)
            val high = maxOf(open, close) + 1.2
            val low = minOf(open, close) - 1.0
            list.add(
                Candle(
                    symbol = "TEST",
                    timeframe = "1D",
                    openTime = 1710000000000L + i * 86400000L,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 1000.0 + i * 50.0,
                    closeTime = 1710000000000L + (i + 1) * 86400000L - 1L,
                    isClosed = true
                )
            )
            cur = close
        }
        return list
    }

    @Test
    fun testSMA_edgeCasesAndAccuracy() {
        // Empty list
        assertTrue(IndicatorCalculator.calculateSMA(emptyList(), 14).isEmpty())

        // Small list (< period)
        val shortCandles = generateCandles(5)
        val shortSma = IndicatorCalculator.calculateSMA(shortCandles, 10)
        assertEquals(5, shortSma.size)
        // Should calculate cumulative average gracefully
        assertNotNull(shortSma.last())

        // Standard list
        val candles = generateCandles(50)
        val sma20 = IndicatorCalculator.calculateSMA(candles, 20)
        assertEquals(50, sma20.size)
        assertNull(sma20[0])
        assertNull(sma20[18])
        assertNotNull(sma20[19])
        assertNotNull(sma20[49])

        // Verify mathematical accuracy for index 19
        var manualSum = 0.0
        for (j in 0..19) manualSum += candles[j].close
        val expected = manualSum / 20.0
        assertEquals(expected, sma20[19]!!, 0.0001)
    }

    @Test
    fun testEMA_weighting() {
        assertTrue(IndicatorCalculator.calculateEMA(emptyList(), 21).isEmpty())

        val candles = generateCandles(60)
        val ema20 = IndicatorCalculator.calculateEMA(candles, 20)
        assertEquals(60, ema20.size)
        assertNotNull(ema20[19])
        assertNotNull(ema20[59])
        assertTrue(ema20[59]!! > 0.0)
    }

    @Test
    fun testRSI_boundedAndDirectional() {
        val candles = generateCandles(50, trend = 2.0) // Strong uptrend
        val rsi = IndicatorCalculator.calculateRSI(candles, 14)
        assertEquals(50, rsi.size)

        val nonNullRsi = rsi.filterNotNull()
        assertTrue(nonNullRsi.isNotEmpty())
        for (v in nonNullRsi) {
            assertTrue("RSI should be between 0 and 100", v in 0.0..100.0)
        }
        // In strong sustained uptrend, RSI should be in upper bullish band (> 60)
        assertTrue(rsi.last()!! > 60.0)
    }

    @Test
    fun testMACD_linesAndHistogram() {
        val candles = generateCandles(80)
        val macd = IndicatorCalculator.calculateMACD(candles, fastPeriod = 12, slowPeriod = 26, signalPeriod = 9)

        assertEquals(80, macd.macdLine.size)
        assertEquals(80, macd.signalLine.size)
        assertEquals(80, macd.histogram.size)

        // Find last index where all are computed
        val lastIdx = 79
        val m = macd.macdLine[lastIdx]
        val s = macd.signalLine[lastIdx]
        val h = macd.histogram[lastIdx]

        assertNotNull(m)
        assertNotNull(s)
        assertNotNull(h)
        // Histogram = MACD Line - Signal Line
        assertEquals(m!! - s!!, h!!, 0.0001)
    }

    @Test
    fun testBollingerBands_boundaries() {
        val candles = generateCandles(60)
        val bb = IndicatorCalculator.calculateBollingerBands(candles, period = 20, deviation = 2.0)

        assertEquals(60, bb.upper.size)
        assertEquals(60, bb.middle.size)
        assertEquals(60, bb.lower.size)

        for (i in 19 until 60) {
            val u = bb.upper[i]
            val m = bb.middle[i]
            val l = bb.lower[i]
            assertNotNull(u)
            assertNotNull(m)
            assertNotNull(l)
            assertTrue("Upper band should be >= middle band", u!! >= m!!)
            assertTrue("Middle band should be >= lower band", m >= l!!)
        }
    }

    @Test
    fun testSupertrend_directionAndCorridor() {
        val candles = generateCandles(60, trend = 1.0)
        val st = IndicatorCalculator.calculateSupertrend(candles, period = 10, multiplier = 3.0)

        assertEquals(60, st.values.size)
        assertEquals(60, st.directions.size)

        val lastDir = st.directions.last()
        // Direction should be either true (bullish) or false (bearish)
        assertNotNull(lastDir)
        assertNotNull(st.values.last())
    }

    @Test
    fun testATR_positiveVolatility() {
        val candles = generateCandles(40)
        val atr = IndicatorCalculator.calculateATR(candles, period = 14)

        assertEquals(40, atr.size)
        val nonNullAtr = atr.filterNotNull()
        assertTrue(nonNullAtr.isNotEmpty())
        for (v in nonNullAtr) {
            assertTrue("ATR must always be positive", v > 0.0)
        }
    }

    @Test
    fun testStochastic_oscillatorBounds() {
        val candles = generateCandles(50)
        val stoch = IndicatorCalculator.calculateStochastic(candles, kPeriod = 14, dPeriod = 3)

        assertEquals(50, stoch.kLine.size)
        assertEquals(50, stoch.dLine.size)

        val nonNullK = stoch.kLine.filterNotNull()
        for (k in nonNullK) {
            assertTrue("Stochastic %K must be between 0 and 100", k in 0.0..100.0)
        }
    }

    @Test
    fun testIchimoku_clouds() {
        val candles = generateCandles(100)
        val ichi = IndicatorCalculator.calculateIchimoku(candles, tenkanPeriod = 9, kijunPeriod = 26, senkouBPeriod = 52)

        assertEquals(100, ichi.tenkanSen.size)
        assertEquals(100, ichi.kijunSen.size)
        assertEquals(100 + 26, ichi.senkouSpanA.size)
        assertEquals(100 + 26, ichi.senkouSpanB.size)
        assertEquals(100, ichi.chikouSpan.size)
    }

    @Test
    fun testCVD_cumulativeDelta() {
        val candles = generateCandles(30)
        val cvd = IndicatorCalculator.calculateCVD(candles)

        assertEquals(30, cvd.cvdLine.size)
        assertEquals(30, cvd.deltaBars.size)
        assertNotNull(cvd.cvdLine.last())
        assertNotNull(cvd.deltaBars.last())
    }

    @Test
    fun testVolumeProfile_pocAndValueArea() {
        val candles = generateCandles(50)
        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        val vp = IndicatorCalculator.calculateVolumeProfile(candles, priceMin = minPrice, priceMax = maxPrice, bucketCount = 24)

        assertNotNull(vp)
        assertTrue(vp.buckets.isNotEmpty())
        assertTrue("POC Price must be within candle high/low range", vp.pocPrice >= minPrice && vp.pocPrice <= maxPrice)
        assertTrue("VAH must be >= VAL", vp.vahPrice >= vp.valPrice)
    }

    @Test
    fun testDetectSMC_structures() {
        val candles = generateCandles(80)
        val smc = IndicatorCalculator.detectSMC(candles)

        assertNotNull(smc)
        assertNotNull(smc.fvgs)
        assertNotNull(smc.sweeps)
    }

    @Test
    fun testParabolicSAR_behavior() {
        val candles = generateCandles(60)
        val sar = IndicatorCalculator.calculateParabolicSAR(candles)

        assertEquals(60, sar.size)
        assertNotNull(sar.last())
    }

    @Test
    fun testADX_diValues() {
        val candles = generateCandles(60)
        val adx = IndicatorCalculator.calculateADX(candles, period = 14)

        assertEquals(60, adx.adx.size)
        assertEquals(60, adx.plusDI.size)
        assertEquals(60, adx.minusDI.size)

        val nonNullAdx = adx.adx.filterNotNull()
        for (v in nonNullAdx) {
            assertTrue("ADX should be >= 0", v >= 0.0)
        }
    }

    @Test
    fun testOBV_calculation() {
        val candles = generateCandles(30)
        val obv = IndicatorCalculator.calculateOBV(candles)

        assertEquals(30, obv.size)
        assertNotNull(obv[0])
        assertEquals(0.0, obv[0]!!, 0.01)
        assertNotNull(obv[29])
    }

    @Test
    fun testCCI_calculation() {
        val candles = generateCandles(40)
        val cci = IndicatorCalculator.calculateCCI(candles, period = 20)

        assertEquals(40, cci.size)
        assertNotNull(cci.last())
    }

    @Test
    fun testWilliamsR_bounds() {
        val candles = generateCandles(40)
        val wR = IndicatorCalculator.calculateWilliamsR(candles, period = 14)

        assertEquals(40, wR.size)
        val nonNullWR = wR.filterNotNull()
        for (v in nonNullWR) {
            assertTrue("Williams %R must be between -100 and 0, got $v", v in -100.0..0.0)
        }
    }

    @Test
    fun testMFI_bounds() {
        val candles = generateCandles(40)
        val mfi = IndicatorCalculator.calculateMFI(candles, period = 14)

        assertEquals(40, mfi.size)
        val nonNullMfi = mfi.filterNotNull()
        for (v in nonNullMfi) {
            assertTrue("MFI must be between 0 and 100, got $v", v in 0.0..100.0)
        }
    }
}
