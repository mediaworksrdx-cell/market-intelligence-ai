package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.model.StrategyLeg
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OptionsStrategyEngineTest {

    private lateinit var quantEngine: Black76QuantEngine
    private lateinit var strategyEngine: OptionsStrategyEngine

    @Before
    fun setUp() {
        quantEngine = Black76QuantEngine()
        strategyEngine = OptionsStrategyEngine(quantEngine)
    }

    @Test
    fun testBullCallSpread_DefinedProfitAndLoss() {
        val spot = 22500.0
        val lotSize = 50
        val legs = listOf(
            StrategyLeg(
                id = "leg1",
                type = "BUY",
                instrument = "CALL",
                strike = 22500.0,
                expiry = "2026-10-29",
                qty = lotSize,
                entryPrice = 200.0,
                currentPrice = 200.0,
                lotSize = lotSize,
                lots = 1
            ),
            StrategyLeg(
                id = "leg2",
                type = "SELL",
                instrument = "CALL",
                strike = 22700.0,
                expiry = "2026-10-29",
                qty = lotSize,
                entryPrice = 110.0,
                currentPrice = 110.0,
                lotSize = lotSize,
                lots = 1
            )
        )

        val result = strategyEngine.analyzeStrategy(
            legs = legs,
            spotPrice = spot,
            impliedVolatility = 14.0,
            daysToExpiry = 7.0,
            lotSize = lotSize
        )

        // Net debit = (200 - 110) * 50 = 90 * 50 = 4500
        assertEquals(-4500.0, result.netCreditDebit, 1.0)
        assertFalse("Bull call spread should not have unlimited profit", result.isMaxProfitUnlimited)
        assertFalse("Bull call spread should not have unlimited loss", result.isMaxLossUnlimited)

        // Max profit = (Strike difference - Net Debit) * Qty = (200 - 90) * 50 = 110 * 50 = 5500
        assertEquals(5500.0, result.maxProfit, 100.0)
        // Max loss = -4500
        assertEquals(-4500.0, result.maxLoss, 100.0)

        // Breakeven = Lower strike + net debit = 22500 + 90 = 22590
        assertEquals(1, result.breakevens.size)
        assertEquals(22590.0, result.breakevens[0], 10.0)

        // POP should be between 30% and 70%
        assertTrue("POP should be reasonable", result.probabilityOfProfit in 30.0..70.0)

        // Delta should be positive for Bull Call Spread
        assertTrue("Portfolio delta should be bullish", result.totalGreeks.delta > 0.0)
    }

    @Test
    fun testLongStraddle_UnlimitedProfitAndDefinedLoss() {
        val spot = 22500.0
        val lotSize = 50
        val legs = listOf(
            StrategyLeg(
                id = "leg1",
                type = "BUY",
                instrument = "CALL",
                strike = 22500.0,
                expiry = "2026-10-29",
                qty = lotSize,
                entryPrice = 180.0,
                currentPrice = 180.0,
                lotSize = lotSize
            ),
            StrategyLeg(
                id = "leg2",
                type = "BUY",
                instrument = "PUT",
                strike = 22500.0,
                expiry = "2026-10-29",
                qty = lotSize,
                entryPrice = 170.0,
                currentPrice = 170.0,
                lotSize = lotSize
            )
        )

        val result = strategyEngine.analyzeStrategy(
            legs = legs,
            spotPrice = spot,
            impliedVolatility = 15.0,
            daysToExpiry = 7.0,
            lotSize = lotSize
        )

        assertTrue("Long straddle must have unlimited profit", result.isMaxProfitUnlimited)
        assertFalse("Long straddle max loss is bounded", result.isMaxLossUnlimited)
        assertEquals("Unlimited", result.riskRewardRatio)

        // Net debit = (180 + 170) * 50 = 350 * 50 = 17500
        assertEquals(-17500.0, result.netCreditDebit, 1.0)
        assertEquals(-17500.0, result.maxLoss, 100.0)

        // Two breakevens: 22500 - 350 = 22150 and 22500 + 350 = 22850
        assertEquals(2, result.breakevens.size)
        assertEquals(22150.0, result.breakevens[0], 15.0)
        assertEquals(22850.0, result.breakevens[1], 15.0)
    }

    @Test
    fun testNakedShortCall_UnlimitedLoss() {
        val spot = 22500.0
        val lotSize = 50
        val legs = listOf(
            StrategyLeg(
                id = "leg1",
                type = "SELL",
                instrument = "CALL",
                strike = 22600.0,
                expiry = "2026-10-29",
                qty = lotSize,
                entryPrice = 120.0,
                currentPrice = 120.0,
                lotSize = lotSize
            )
        )

        val result = strategyEngine.analyzeStrategy(
            legs = legs,
            spotPrice = spot,
            impliedVolatility = 14.0,
            daysToExpiry = 5.0,
            lotSize = lotSize
        )

        assertFalse("Short call does not have unlimited profit", result.isMaxProfitUnlimited)
        assertTrue("Naked short call must have unlimited loss", result.isMaxLossUnlimited)
        assertEquals("Unlimited Loss", result.riskRewardRatio)
        assertEquals(6000.0, result.maxProfit, 1.0) // 120 * 50
        assertTrue("Estimated margin for short call should be substantial", result.estimatedMargin > 50000.0)
    }

    @Test
    fun testIronCondor_FourLegsTwoBreakevens() {
        val spot = 22500.0
        val lotSize = 50
        val legs = listOf(
            // Bull Put Spread
            StrategyLeg("l1", "BUY", "PUT", 22200.0, "2026-10-29", lotSize, 40.0, 40.0, lotSize),
            StrategyLeg("l2", "SELL", "PUT", 22300.0, "2026-10-29", lotSize, 70.0, 70.0, lotSize),
            // Bear Call Spread
            StrategyLeg("l3", "SELL", "CALL", 22700.0, "2026-10-29", lotSize, 80.0, 80.0, lotSize),
            StrategyLeg("l4", "BUY", "CALL", 22800.0, "2026-10-29", lotSize, 45.0, 45.0, lotSize)
        )

        val result = strategyEngine.analyzeStrategy(
            legs = legs,
            spotPrice = spot,
            impliedVolatility = 13.0,
            daysToExpiry = 6.0,
            lotSize = lotSize
        )

        assertFalse(result.isMaxProfitUnlimited)
        assertFalse(result.isMaxLossUnlimited)

        // Net credit = (70 - 40) + (80 - 45) = 30 + 35 = 65 pts * 50 = 3250
        assertEquals(3250.0, result.netCreditDebit, 1.0)
        assertEquals(3250.0, result.maxProfit, 100.0)
        // Max loss = (Wing width 100 - Net credit 65) * 50 = 35 * 50 = -1750
        assertEquals(-1750.0, result.maxLoss, 100.0)

        // Should have 2 breakevens
        assertEquals(2, result.breakevens.size)
        // High POP for credit Iron Condor
        assertTrue("Iron condor POP should be > 50%", result.probabilityOfProfit > 50.0)
        // Scenario matrix should have 11 rows
        assertEquals(11, result.scenarioMatrix.size)
    }

    @Test
    fun testFuturesAnalysis() {
        val spot = 22500.0
        val futPrice = 22580.0
        val legs = listOf(
            StrategyLeg("l1", "BUY", "FUT", 22500.0, "2026-10-29", 50, 22500.0, 22580.0, 50)
        )

        val result = strategyEngine.analyzeStrategy(
            legs = legs,
            spotPrice = spot,
            impliedVolatility = 14.0,
            daysToExpiry = 10.0,
            lotSize = 50,
            futuresPrice = futPrice
        )

        val fut = result.futuresAnalysis
        assertNotNull(fut)
        assertEquals(80.0, fut!!.basis, 0.1)
        assertTrue(fut.annualizedCostOfCarry > 0.0)
        assertTrue(fut.fairValue > spot)
    }
}
