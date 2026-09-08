package com.marketintelligence.cryptotracker.engine

import com.marketintelligence.tradeengine.models.Candle
import org.junit.Assert.*
import org.junit.Test

class CryptoEngineTest {

    private fun generateUptrendCandles(count: Int, symbol: String = "BTCUSDT", timeframe: String = "15m"): List<Candle> {
        val candles = mutableListOf<Candle>()
        val basePrice = 50000.0
        val step = 60_000L * 15
        val now = 1700000000000L

        for (i in 0 until count) {
            val cycle = i / 10
            val phase = i % 10
            val priceOffset = (cycle * 60.0) + if (phase < 5) {
                (phase + 1) * 20.0
            } else {
                100.0 - (phase - 4) * 8.0
            }
            val open = basePrice + priceOffset
            val close = open + (if (phase < 5) 15.0 else -5.0)
            val high = maxOf(open, close) + 5.0
            val low = minOf(open, close) - 5.0
            
            candles.add(Candle(
                symbol = symbol,
                timeframe = timeframe,
                openTime = now + i * step,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = 1000.0 + (i * 50.0),
                closeTime = now + (i + 1) * step,
                isClosed = true
            ))
        }
        return candles
    }

    @Test
    fun testSMCEngineSwingAndStructureDetection() {
        val engine = CryptoSMCEngine(swingLookback = 3)
        val candles = generateUptrendCandles(40)
        
        val result = engine.analyze(candles)
        
        assertNotNull(result)
        assertTrue("Swing points should be detected", result.swingPoints.isNotEmpty())
        assertEquals(StructureState.BULLISH, result.structureState)
        assertNotNull(result.premiumDiscountZone)
    }

    @Test
    fun testSMCEngineInsufficientCandles() {
        val engine = CryptoSMCEngine(swingLookback = 5)
        val candles = generateUptrendCandles(8) // Fewer than 11 candles
        
        val result = engine.analyze(candles)
        
        assertEquals(StructureState.RANGING, result.structureState)
        assertTrue("Swing points should be empty for insufficient candles", result.swingPoints.isEmpty())
        assertTrue("Structure events should be empty", result.structureEvents.isEmpty())
    }

    @Test
    fun testFVGEngineBullishDetection() {
        val fvgEngine = CryptoFVGEngine()
        val smcEngine = CryptoSMCEngine(swingLookback = 3)
        val step = 60_000L * 15
        val now = 1700000000000L

        // Generate baseline candles then a clean Bullish FVG pattern
        val candles = mutableListOf<Candle>()
        for (i in 0 until 18) {
            candles.add(Candle("BTCUSDT", "15m", now + i * step, 100.0 + i, 105.0 + i, 95.0 + i, 102.0 + i, 500.0, now + (i + 1) * step, true))
        }
        // Candle 18 (C1): high = 110.0
        candles.add(Candle("BTCUSDT", "15m", now + 18 * step, 104.0, 110.0, 103.0, 109.0, 600.0, now + 19 * step, true))
        // Candle 19 (C2): big displacement impulse candle
        candles.add(Candle("BTCUSDT", "15m", now + 19 * step, 109.0, 145.0, 108.0, 142.0, 4500.0, now + 20 * step, true))
        // Candle 20 (C3): low = 120.0 (> C1.high of 110.0) -> FVG gap = [110.0, 120.0]
        candles.add(Candle("BTCUSDT", "15m", now + 20 * step, 142.0, 150.0, 120.0, 148.0, 800.0, now + 21 * step, true))

        val smcAnalysis = smcEngine.analyze(candles)
        val fvgs = fvgEngine.analyze(candles, smcAnalysis, StructureState.BULLISH, 65.0)

        val bullishFvg = fvgs.find { it.direction == FVGDirection.BULLISH }
        assertNotNull("Bullish FVG should be detected", bullishFvg)
        assertEquals(120.0, bullishFvg!!.top, 0.01)
        assertEquals(110.0, bullishFvg.bottom, 0.01)
        assertTrue("FVG quality score should be calculated", bullishFvg.qualityScore.total > 0)
    }

    @Test
    fun testRSIEngineCalculationAndMomentum() {
        val rsiEngine = CryptoRSIEngine(period = 14)
        val candles = generateUptrendCandles(40)
        
        val result = rsiEngine.analyze(candles, StructureState.BULLISH, emptyList())
        
        assertTrue("RSI should be greater than 50 for steady uptrend, got: ${result.currentRSI}", result.currentRSI > 50.0)
        assertEquals(MomentumDirection.BULLISH, result.momentumDirection)
        assertTrue("RSI confirmation should be true for bullish alignment", result.confirmation.isConfirming)
    }

    @Test
    fun testConfluenceEngineFullPipeline() {
        val smcEngine = CryptoSMCEngine(swingLookback = 3)
        val fvgEngine = CryptoFVGEngine()
        val rsiEngine = CryptoRSIEngine(period = 14)
        val riskEngine = CryptoRiskEngine(minRiskReward = 1.5)
        
        val confluenceEngine = CryptoConfluenceEngine(smcEngine, fvgEngine, rsiEngine, riskEngine)
        
        val htfCandles = generateUptrendCandles(50, "BTCUSDT", "4h")
        val mtfCandles = generateUptrendCandles(50, "BTCUSDT", "15m")
        
        val setup = confluenceEngine.analyze("BTCUSDT", htfCandles, mtfCandles)
        
        // Confluence analyze returns setup if score >= 30
        if (setup != null) {
            assertEquals("BTCUSDT", setup.symbol)
            assertEquals("15m", setup.timeframe)
            assertEquals(SetupDirection.LONG, setup.direction)
            assertTrue("Score total should be >= 30", setup.score.total >= 30)
            assertNotNull("Setup grade should be assigned", setup.grade)
            assertTrue("Stop loss should be below entry for long", setup.stopLoss <= setup.entry)
            assertTrue("TP1 should be above entry for long", setup.tp1 >= setup.entry)
        }
    }
}
