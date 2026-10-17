package com.example.marketintelligence.ui.chart

import com.example.marketintelligence.domain.chart.IndicatorCalculator
import com.example.marketintelligence.domain.chart.IndicatorConfig
import com.example.marketintelligence.domain.chart.IndicatorType
import com.example.tradeengine.models.Candle
import org.junit.Assert.*
import org.junit.Test

class IndicatorCalculatorAndSettingsTest {

    private fun generateTestCandles(count: Int, startPrice: Double = 100.0): List<Candle> {
        val list = mutableListOf<Candle>()
        var cur = startPrice
        for (i in 0 until count) {
            val open = cur
            val close = cur + (if (i % 2 == 0) 1.5 else -1.2)
            val high = maxOf(open, close) + 0.5
            val low = minOf(open, close) - 0.5
            list.add(
                Candle(
                    symbol = "TEST",
                    timeframe = "1D",
                    openTime = 1000L * i,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 1000.0 + i * 10,
                    closeTime = 1000L * i + 999L,
                    isClosed = true
                )
            )
            cur = close
        }
        return list
    }

    @Test
    fun testSMAUpTo200DaysMovingAverage() {
        val candles = generateTestCandles(300)
        
        // Test 20, 50, 100, 200 SMA
        val sma20 = IndicatorCalculator.calculateSMA(candles, 20)
        val sma50 = IndicatorCalculator.calculateSMA(candles, 50)
        val sma100 = IndicatorCalculator.calculateSMA(candles, 100)
        val sma200 = IndicatorCalculator.calculateSMA(candles, 200)

        assertEquals(300, sma20.size)
        assertEquals(300, sma50.size)
        assertEquals(300, sma100.size)
        assertEquals(300, sma200.size)

        // For 200 SMA on 300 candles, last 101 candles should have non-null values
        assertNotNull(sma200[199])
        assertNotNull(sma200[299])
        assertTrue(sma200[299]!! > 0)
    }

    @Test
    fun testEMAUpTo200DaysMovingAverage() {
        val candles = generateTestCandles(300)
        
        val ema20 = IndicatorCalculator.calculateEMA(candles, 20)
        val ema50 = IndicatorCalculator.calculateEMA(candles, 50)
        val ema100 = IndicatorCalculator.calculateEMA(candles, 100)
        val ema200 = IndicatorCalculator.calculateEMA(candles, 200)

        assertEquals(300, ema20.size)
        assertEquals(300, ema50.size)
        assertEquals(300, ema100.size)
        assertEquals(300, ema200.size)

        assertNotNull(ema200[199])
        assertNotNull(ema200[299])
        assertTrue(ema200[299]!! > 0)
    }

    @Test
    fun testBollingerBandsLargePeriod() {
        val candles = generateTestCandles(300)
        val bb = IndicatorCalculator.calculateBollingerBands(candles, period = 200, deviation = 2.0)

        assertEquals(300, bb.upper.size)
        assertEquals(300, bb.middle.size)
        assertEquals(300, bb.lower.size)

        val lastUpper = bb.upper[299]
        val lastMiddle = bb.middle[299]
        val lastLower = bb.lower[299]

        assertNotNull(lastUpper)
        assertNotNull(lastMiddle)
        assertNotNull(lastLower)
        assertTrue(lastUpper!! >= lastMiddle!!)
        assertTrue(lastMiddle >= lastLower!!)
    }

    @Test
    fun testRSIUpTo200Period() {
        val candles = generateTestCandles(300)
        val rsi14 = IndicatorCalculator.calculateRSI(candles, 14)
        val rsi200 = IndicatorCalculator.calculateRSI(candles, 200)

        assertEquals(300, rsi14.size)
        assertEquals(300, rsi200.size)
        assertNotNull(rsi14[299])
        assertNotNull(rsi200[299])
        assertTrue(rsi200[299]!! in 0.0..100.0)
    }

    @Test
    fun testIndicatorConfigFactoryDefaults() {
        assertEquals(20, IndicatorConfig.defaultPeriod(IndicatorType.SMA))
        assertEquals(21, IndicatorConfig.defaultPeriod(IndicatorType.EMA))
        assertEquals(20, IndicatorConfig.defaultPeriod(IndicatorType.BOLLINGER_BANDS))
        assertEquals(14, IndicatorConfig.defaultPeriod(IndicatorType.RSI))
        assertEquals(12, IndicatorConfig.defaultPeriod(IndicatorType.MACD))
        assertEquals(10, IndicatorConfig.defaultPeriod(IndicatorType.SUPERTREND))
        assertEquals(14, IndicatorConfig.defaultPeriod(IndicatorType.ATR))

        // Multipliers
        assertEquals(2.0, IndicatorConfig.defaultMultiplier(IndicatorType.BOLLINGER_BANDS), 0.001)
        assertEquals(3.0, IndicatorConfig.defaultMultiplier(IndicatorType.SUPERTREND), 0.001)
    }

    @Test
    fun testDefaultResetIntegrity() {
        val modifiedSma = IndicatorConfig(
            type = IndicatorType.SMA,
            period = 21, // User changed 20 to 21
            enabled = true
        )
        assertEquals(21, modifiedSma.period)

        // Reset to default
        val resetSma = modifiedSma.copy(
            period = IndicatorConfig.defaultPeriod(modifiedSma.type),
            multiplier = IndicatorConfig.defaultMultiplier(modifiedSma.type),
            secondaryPeriod = IndicatorConfig.defaultSecondaryPeriod(modifiedSma.type),
            tertiaryPeriod = IndicatorConfig.defaultTertiaryPeriod(modifiedSma.type),
            color = IndicatorConfig.defaultColor(modifiedSma.type)
        )

        assertEquals(20, resetSma.period)
        assertEquals(0xFFFF9800L, resetSma.color)
    }

    @Test
    fun testVWAPSessionResetOnNewDay() {
        val candles = mutableListOf<Candle>()
        val day1StartMs = 1712000000000L // e.g. Day 1
        val day2StartMs = day1StartMs + 86400000L // Day 2 (next day)

        // Day 1 candles around 20,000
        for (i in 0 until 5) {
            candles.add(
                Candle(
                    symbol = "NIFTY",
                    timeframe = "5m",
                    openTime = day1StartMs + i * 300000L,
                    open = 20000.0,
                    high = 20020.0,
                    low = 19980.0,
                    close = 20010.0,
                    volume = 1000.0,
                    closeTime = day1StartMs + (i + 1) * 300000L,
                    isClosed = true
                )
            )
        }

        // Day 2 candles around 22,000
        for (i in 0 until 5) {
            candles.add(
                Candle(
                    symbol = "NIFTY",
                    timeframe = "5m",
                    openTime = day2StartMs + i * 300000L,
                    open = 22000.0,
                    high = 22020.0,
                    low = 21980.0,
                    close = 22010.0,
                    volume = 1000.0,
                    closeTime = day2StartMs + (i + 1) * 300000L,
                    isClosed = true
                )
            )
        }

        val vwap = IndicatorCalculator.calculateVWAP(candles)
        assertEquals(10, vwap.size)

        // Day 1 last candle VWAP should be ~20003.33
        val day1VWAP = vwap[4]
        assertNotNull(day1VWAP)
        assertTrue("Day 1 VWAP should be around 20,000", day1VWAP!! in 19980.0..20020.0)

        // Day 2 first candle VWAP must reset to Day 2 price (~22003.33) and NOT be weighted down by Day 1's 20,000!
        val day2FirstVWAP = vwap[5]
        assertNotNull(day2FirstVWAP)
        assertTrue("Day 2 VWAP should reset to ~22,000, actual: $day2FirstVWAP", day2FirstVWAP!! in 21980.0..22020.0)
    }

    @Test
    fun testVWAPStaysCloseToCandles() {
        val candles = generateTestCandles(100, startPrice = 22600.0)
        val vwap = IndicatorCalculator.calculateVWAP(candles)

        assertEquals(100, vwap.size)
        val lastVWAP = vwap[99]
        assertNotNull(lastVWAP)
        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        assertTrue("VWAP must remain within realistic price corridor", lastVWAP!! in (minPrice - 50.0)..(maxPrice + 50.0))
    }
}
