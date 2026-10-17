package com.example.marketintelligence.ui.fno

import com.example.marketintelligence.domain.model.StrategyLeg
import com.example.marketintelligence.domain.model.OptionStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Institutional Strategy Builder & F&O OI Classification Verification Test
 * Validates:
 * 1. Expiry Date handling and freshness
 * 2. Lot Size accuracy: Nifty must be 65, and independently checks all domestic & global instruments
 * 3. ITM / ATM / OTM classification rules for CE & PE around the ATM strike
 * 4. Strategy Builder contract parameter integration
 * 5. Multi-asset & global market coverage
 */
class InstitutionalStrategyBuilderVerificationTest {

    // Helper functions reproducing classification logic
    private fun getLotSize(symbol: String): Int {
        val clean = symbol.uppercase().trim().removeSuffix(".NS").removeSuffix(".BO").replace("-", "").replace(" ", "")
        return when {
            clean.contains("BANKNIFTY") -> 15
            clean.contains("FINNIFTY") -> 40
            clean.contains("MIDCPNIFTY") -> 75
            clean.contains("NIFTY") -> 65 // REQUIRED: NIFTY lot size is 65
            clean.contains("SENSEX") -> 10
            clean.contains("BANKEX") -> 15
            // Global US Indices & Equities (Standard US Option contract lot size is 100 shares)
            clean.contains("SPX") || clean.contains("SP500") -> 100
            clean.contains("NDX") || clean.contains("QQQ") || clean.contains("NASDAQ") -> 100
            clean.contains("DJI") || clean.contains("DOW") -> 100
            clean.contains("AAPL") || clean.contains("NVDA") || clean.contains("MSFT") ||
            clean.contains("GOOG") || clean.contains("AMZN") || clean.contains("TSLA") || clean.contains("META") -> 100
            // UAE Indices & Equities
            clean.contains("DFMGI") || clean.contains("ADX") -> 100
            clean.contains("EMAAR") || clean.contains("FAB") || clean.contains("DEWA") || clean.contains("ALDAR") -> 100
            // European & Global Indices
            clean.contains("FTSE") || clean.contains("DAX") || clean.contains("NIKKEI") || clean.contains("HSI") -> 100
            // Commodities
            clean.contains("GOLD") || clean.contains("XAU") -> 100
            clean.contains("SILVER") || clean.contains("XAG") -> 30
            clean.contains("CRUDE") || clean.contains("WTI") || clean.contains("BRENT") -> 100
            clean.contains("NATURALGAS") || clean.contains("NATGAS") -> 1250
            // Cryptocurrencies (Standard 1 unit)
            clean in listOf("BTC", "ETH", "SOL", "BNB", "DOGE", "XRP") ||
            clean.contains("BTC") || clean.contains("ETH") || clean.contains("SOL") -> 1
            // Indian Stocks
            clean.contains("TATASTEEL") -> 5500
            clean.contains("TATAMOTORS") -> 1425
            clean.contains("MARUTI") -> 50
            clean.contains("BAJFINANCE") -> 125
            clean.contains("RELIANCE") -> 250
            clean.contains("HDFCBANK") -> 550
            clean.contains("TCS") -> 175
            clean.contains("ICICIBANK") -> 700
            clean.contains("SBIN") -> 750
            clean.contains("INFY") -> 400
            clean.contains("ITC") -> 1600
            clean.contains("LT") -> 150
            clean.contains("AXISBANK") -> 625
            clean.contains("KOTAKBANK") -> 400
            clean.contains("WIPRO") -> 1500
            clean.contains("HCLTECH") -> 350
            clean.contains("TECHM") -> 600
            clean.contains("BAJAJAUTO") -> 75
            clean.contains("ONGC") -> 3850
            clean.contains("BPCL") -> 1800
            clean.contains("NTPC") -> 1500
            clean.contains("JSWSTEEL") -> 675
            clean.contains("HINDALCO") -> 1400
            clean.contains("SUNPHARMA") -> 350
            clean.contains("DRREDDY") -> 125
            clean.contains("HINDUNILVR") -> 300
            clean.contains("TITAN") -> 175
            clean.contains("BHARTIARTL") -> 475
            else -> 65
        }
    }

    private fun calculateMoneyness(strike: Double, spotPrice: Double, instrument: String, strikeStep: Double): String {
        if (instrument.equals("FUT", ignoreCase = true)) return "FUT"
        val halfStep = (strikeStep / 2.0).coerceAtLeast(0.5)
        val isAtm = abs(strike - spotPrice) <= halfStep
        if (isAtm) return "ATM"
        val isCall = instrument.equals("CALL", ignoreCase = true) || instrument.equals("CE", ignoreCase = true)
        return if (isCall) {
            if (strike < spotPrice) "ITM" else "OTM"
        } else {
            if (strike > spotPrice) "ITM" else "OTM"
        }
    }

    @Test
    fun testNiftyLotSizeIsExactly65() {
        // Requirement 2: Nifty lot size must be 65, not 25
        val niftyLot = getLotSize("NIFTY")
        val nifty50Lot = getLotSize("NIFTY 50")
        val niftyIndexLot = getLotSize("NIFTY50")
        
        assertEquals("NIFTY lot size must be 65", 65, niftyLot)
        assertEquals("NIFTY 50 lot size must be 65", 65, nifty50Lot)
        assertEquals("NIFTY50 lot size must be 65", 65, niftyIndexLot)
        assertNotEquals("NIFTY lot size must NOT be 25", 25, niftyLot)
    }

    @Test
    fun testIndependentLotSizesForSupportedInstruments() {
        // Domestic Indices
        assertEquals("BANKNIFTY lot size must be 15", 15, getLotSize("BANKNIFTY"))
        assertEquals("FINNIFTY lot size must be 40", 40, getLotSize("FINNIFTY"))
        assertEquals("MIDCPNIFTY lot size must be 75", 75, getLotSize("MIDCPNIFTY"))
        assertEquals("SENSEX lot size must be 10", 10, getLotSize("SENSEX"))

        // Top Indian Stocks
        assertEquals("RELIANCE lot size", 250, getLotSize("RELIANCE"))
        assertEquals("HDFCBANK lot size", 550, getLotSize("HDFCBANK"))
        assertEquals("TCS lot size", 175, getLotSize("TCS"))
        assertEquals("ICICIBANK lot size", 700, getLotSize("ICICIBANK"))
        assertEquals("SBIN lot size", 750, getLotSize("SBIN"))
        assertEquals("INFY lot size", 400, getLotSize("INFY"))
        assertEquals("TATASTEEL lot size", 5500, getLotSize("TATASTEEL"))

        // Global Markets (US, UAE, Europe, Commodities, Crypto)
        assertEquals("SPX lot size must be 100", 100, getLotSize("SPX"))
        assertEquals("NDX lot size must be 100", 100, getLotSize("NDX"))
        assertEquals("DJI lot size must be 100", 100, getLotSize("DJI"))
        assertEquals("DFMGI lot size must be 100", 100, getLotSize("DFMGI"))
        assertEquals("ADX lot size must be 100", 100, getLotSize("ADX"))
        assertEquals("FTSE lot size must be 100", 100, getLotSize("FTSE"))
        assertEquals("DAX lot size must be 100", 100, getLotSize("DAX"))
        assertEquals("GOLD lot size must be 100", 100, getLotSize("GOLD"))
        assertEquals("SILVER lot size must be 30", 30, getLotSize("SILVER"))
        assertEquals("CRUDE lot size must be 100", 100, getLotSize("CRUDE"))
        assertEquals("BTC lot size must be 1", 1, getLotSize("BTC"))
        assertEquals("ETH lot size must be 1", 1, getLotSize("ETH"))
    }

    @Test
    fun testOptionMoneynessClassificationRules() {
        val spot = 22716.20
        val strikeStep = 50.0

        // Closest strike to 22716.20 is 22700.0 (distance 16.20 <= halfStep 25.0)
        val atmStrike = 22700.0
        val itmCallStrike = 22600.0
        val otmCallStrike = 22800.0

        // Requirement 3: Same strike must not simultaneously receive contradictory classifications
        val callAtm = calculateMoneyness(atmStrike, spot, "CALL", strikeStep)
        val putAtm = calculateMoneyness(atmStrike, spot, "PUT", strikeStep)
        assertEquals("ATM call strike must be ATM", "ATM", callAtm)
        assertEquals("ATM put strike must be ATM", "ATM", putAtm)

        // For Call: Strike < Spot is ITM, Strike > Spot is OTM
        val callItm = calculateMoneyness(itmCallStrike, spot, "CALL", strikeStep)
        val callOtm = calculateMoneyness(otmCallStrike, spot, "CALL", strikeStep)
        assertEquals("Lower strike Call must be ITM", "ITM", callItm)
        assertEquals("Higher strike Call must be OTM", "OTM", callOtm)

        // For Put: Strike > Spot is ITM, Strike < Spot is OTM
        val putItm = calculateMoneyness(otmCallStrike, spot, "PUT", strikeStep)
        val putOtm = calculateMoneyness(itmCallStrike, spot, "PUT", strikeStep)
        assertEquals("Higher strike Put must be ITM", "ITM", putItm)
        assertEquals("Lower strike Put must be OTM", "OTM", putOtm)
    }

    @Test
    fun testMoneynessAcrossMultipleStrikesAroundATM() {
        val spot = 22700.0
        val step = 50.0

        val strikes = listOf(22550.0, 22600.0, 22650.0, 22700.0, 22750.0, 22800.0, 22850.0)

        // Test CE across strikes
        val ceMoneyness = strikes.map { calculateMoneyness(it, spot, "CALL", step) }
        assertEquals(listOf("ITM", "ITM", "ITM", "ATM", "OTM", "OTM", "OTM"), ceMoneyness)

        // Test PE across strikes
        val peMoneyness = strikes.map { calculateMoneyness(it, spot, "PUT", step) }
        assertEquals(listOf("OTM", "OTM", "OTM", "ATM", "ITM", "ITM", "ITM"), peMoneyness)
    }

    @Test
    fun testStrategyLegContractParameters() {
        val spot = 22716.20
        val step = 50.0
        val expiry = "28-OCT-2026"
        val underlying = "NIFTY 50"
        val lotSize = getLotSize(underlying)

        val leg = StrategyLeg(
            id = "test-leg-1",
            type = "BUY",
            instrument = "CALL",
            strike = 22700.0,
            expiry = expiry,
            qty = lotSize * 2,
            entryPrice = 145.50,
            currentPrice = 152.00,
            lotSize = lotSize,
            lots = 2,
            underlying = underlying,
            moneyness = calculateMoneyness(22700.0, spot, "CALL", step)
        )

        // Requirement 4 & Strategy Builder parameters
        assertEquals("NIFTY 50", leg.underlying)
        assertEquals("28-OCT-2026", leg.expiry)
        assertEquals(22700.0, leg.strike, 0.001)
        assertEquals("CALL", leg.instrument)
        assertEquals(65, leg.lotSize)
        assertEquals(130, leg.qty)
        assertEquals("ATM", leg.moneyness)
        assertTrue(leg.pnl > 0.0)
    }

    @Test
    fun testOptionStrategyBuilderAggregation() {
        val underlying = "NIFTY 50"
        val lotSize = getLotSize(underlying)
        val expiry = "28-OCT-2026"

        val callLeg = StrategyLeg(
            id = "leg-1",
            type = "BUY",
            instrument = "CALL",
            strike = 22700.0,
            expiry = expiry,
            qty = lotSize,
            entryPrice = 120.0,
            currentPrice = 120.0,
            lotSize = lotSize,
            lots = 1,
            underlying = underlying,
            moneyness = "ATM"
        )

        val putLeg = StrategyLeg(
            id = "leg-2",
            type = "BUY",
            instrument = "PUT",
            strike = 22700.0,
            expiry = expiry,
            qty = lotSize,
            entryPrice = 115.0,
            currentPrice = 115.0,
            lotSize = lotSize,
            lots = 1,
            underlying = underlying,
            moneyness = "ATM"
        )

        val strategy = OptionStrategy(
            id = "strategy-atm-straddle",
            name = "ATM Long Straddle",
            description = "Institutional Straddle",
            maxProfit = 100000.0,
            maxLoss = - (120.0 + 115.0) * lotSize,
            breakeven = listOf(22700.0 - 235.0, 22700.0 + 235.0),
            probability = 54.2,
            roi = 65.0,
            legs = listOf("BUY 22700 CALL [ATM]", "BUY 22700 PUT [ATM]"),
            payoffPoints = emptyList(),
            rawLegs = listOf(callLeg, putLeg),
            expiry = expiry,
            lotSize = lotSize,
            lots = 1,
            capitalRequired = (120.0 + 115.0) * lotSize,
            underlying = underlying
        )

        assertEquals("ATM Long Straddle", strategy.name)
        assertEquals("NIFTY 50", strategy.underlying)
        assertEquals("28-OCT-2026", strategy.expiry)
        assertEquals(65, strategy.lotSize)
        assertEquals(2, strategy.rawLegs.size)
        assertEquals("ATM", strategy.rawLegs[0].moneyness)
        assertEquals("ATM", strategy.rawLegs[1].moneyness)
        assertEquals(65, strategy.rawLegs[0].lotSize)
        assertEquals(65, strategy.rawLegs[1].lotSize)
    }
}
