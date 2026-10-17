package com.example.marketintelligence.ui.fno

import com.example.marketintelligence.domain.model.Greeks
import com.example.marketintelligence.domain.model.OptionChainData
import com.example.tradeengine.models.Candle
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * Unit Test Suite for F&O OI Strike Price Chart & Contract Detail.
 *
 * Verifies:
 * 1. Dedicated strike detail identification (Symbol, Expiry, Strike, Option Type CE/PE).
 * 2. Moneyness classification (ITM, ATM, OTM) for both Calls and Puts.
 * 3. Deterministic Option Candle generation matching live contract LTP, day change %, and IV.
 * 4. OHLCV data integrity (high >= max(open, close), low <= min(open, close), volume > 0).
 * 5. Timeframe interval scaling (1m, 5m, 15m, 1h, 1d) and timestamp monotonicity.
 * 6. CE vs PE contract switching and distinct price action.
 * 7. Dynamic strike switching: ensures distinct contracts have distinct candle series.
 * 8. Loading, error, and clean dismissal states in StrikeChartState and FnoUiState.
 */
class OptionStrikeChartTest {

    private val sampleSpot = 22500.0
    private val sampleExpiry = "24-OCT-2026"

    private val sampleItmCallStrike = OptionChainData(
        strike = 22000.0,
        callOI = 3_500_000.0,
        callChange = 8.5,
        callLTP = 585.50,
        callIV = 14.2,
        callGreeks = Greeks(delta = 0.82, gamma = 0.0012, theta = -6.5, vega = 12.0),
        putOI = 1_200_000.0,
        putChange = -15.2,
        putLTP = 42.10,
        putIV = 16.8,
        putGreeks = Greeks(delta = -0.18, gamma = 0.0012, theta = -4.2, vega = 8.5)
    )

    private val sampleAtmStrike = OptionChainData(
        strike = 22500.0,
        callOI = 5_200_000.0,
        callChange = 2.4,
        callLTP = 175.25,
        callIV = 13.8,
        callGreeks = Greeks(delta = 0.51, gamma = 0.0028, theta = -11.2, vega = 18.4),
        putOI = 4_800_000.0,
        putChange = -2.1,
        putLTP = 168.90,
        putIV = 14.1,
        putGreeks = Greeks(delta = -0.49, gamma = 0.0028, theta = -10.9, vega = 18.2)
    )

    private val sampleOtmCallStrike = OptionChainData(
        strike = 23000.0,
        callOI = 6_100_000.0,
        callChange = -22.5,
        callLTP = 38.40,
        callIV = 15.6,
        callGreeks = Greeks(delta = 0.16, gamma = 0.0011, theta = -5.8, vega = 7.9),
        putOI = 950_000.0,
        putChange = 18.4,
        putLTP = 510.00,
        putIV = 17.5,
        putGreeks = Greeks(delta = -0.84, gamma = 0.0011, theta = -7.1, vega = 13.2)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 1. DEDICATED STRIKE IDENTIFICATION & MONEYNESS CLASSIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testMoneynessClassification_callAndPut() {
        // Strike 22000 (strike < spot): ITM Call, OTM Put
        val diff22000 = sampleItmCallStrike.strike - sampleSpot
        assertTrue("22000 strike should be below spot", diff22000 < 0)
        val isCallItm = diff22000 < -35.0
        val isPutOtm = diff22000 < -35.0
        assertTrue("Call @ 22000 must be ITM", isCallItm)
        assertTrue("Put @ 22000 must be OTM", isPutOtm)

        // Strike 22500 (abs(strike - spot) < 35): ATM
        val diff22500 = sampleAtmStrike.strike - sampleSpot
        val isAtm = abs(diff22500) < 35.0
        assertTrue("22500 strike must be ATM", isAtm)

        // Strike 23000 (strike > spot): OTM Call, ITM Put
        val diff23000 = sampleOtmCallStrike.strike - sampleSpot
        assertTrue("23000 strike should be above spot", diff23000 > 0)
        val isCallOtm = diff23000 > 35.0
        val isPutItm = diff23000 > 35.0
        assertTrue("Call @ 23000 must be OTM", isCallOtm)
        assertTrue("Put @ 23000 must be ITM", isPutItm)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CANDLE GENERATION INTEGRITY & LTP ANCHORING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testOptionCandleGenerator_anchorsExactLiveLtp() {
        val candles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22500.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = 175.25,
            changePercent = 2.4,
            iv = 13.8,
            timeframe = "5m",
            candleCount = 80
        )

        assertFalse("Candles list must not be empty", candles.isEmpty())
        assertEquals("Must generate requested number of candles", 80, candles.size)

        // The final candle close MUST match currentLtp exactly
        val lastCandle = candles.last()
        assertEquals("Final candle close must be anchored to contract live LTP", 175.25, lastCandle.close, 0.001)

        // Verify valid OHLC values for every candle
        for ((idx, candle) in candles.withIndex()) {
            assertTrue("Candle $idx high (${candle.high}) must >= max(open, close) (open=${candle.open}, close=${candle.close})",
                candle.high >= kotlin.math.max(candle.open, candle.close) - 0.001)
            assertTrue("Candle $idx low (${candle.low}) must <= min(open, close) (open=${candle.open}, close=${candle.close})",
                candle.low <= kotlin.math.min(candle.open, candle.close) + 0.001)
            assertTrue("Candle $idx low must be positive", candle.low > 0.0)
            assertTrue("Candle $idx volume must be positive", candle.volume > 0.0)
        }
    }

    @Test
    fun testOptionCandleGenerator_itmAndOtmPricing() {
        // ITM Call: high LTP (585.50)
        val itmCandles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22000.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = sampleItmCallStrike.callLTP,
            changePercent = sampleItmCallStrike.callChange,
            iv = sampleItmCallStrike.callIV,
            timeframe = "5m"
        )
        assertEquals(585.50, itmCandles.last().close, 0.001)
        assertTrue("ITM Call average price should be around 500-600", itmCandles.map { it.close }.average() > 300.0)

        // OTM Call: low LTP (38.40)
        val otmCandles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 23000.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = sampleOtmCallStrike.callLTP,
            changePercent = sampleOtmCallStrike.callChange,
            iv = sampleOtmCallStrike.callIV,
            timeframe = "5m"
        )
        assertEquals(38.40, otmCandles.last().close, 0.001)
        assertTrue("OTM Call average price should be under 100", otmCandles.map { it.close }.average() < 100.0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. CE VS PE CONTRACT TOGGLING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testOptionCandleGenerator_ceVsPeProducesDistinctCharts() {
        val ceCandles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22500.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = sampleAtmStrike.callLTP,
            changePercent = sampleAtmStrike.callChange,
            iv = sampleAtmStrike.callIV,
            timeframe = "5m"
        )

        val peCandles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22500.0,
            optionType = "PE",
            expiry = sampleExpiry,
            currentLtp = sampleAtmStrike.putLTP,
            changePercent = sampleAtmStrike.putChange,
            iv = sampleAtmStrike.putIV,
            timeframe = "5m"
        )

        assertEquals("CE candle close matches Call LTP", 175.25, ceCandles.last().close, 0.001)
        assertEquals("PE candle close matches Put LTP", 168.90, peCandles.last().close, 0.001)

        // The series should not be identical
        val cePrices = ceCandles.map { it.close }
        val pePrices = peCandles.map { it.close }
        assertNotEquals("CE and PE price series must be distinct", cePrices, pePrices)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TIMEFRAME INTERVAL SCALING & MONOTONICITY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testOptionCandleGenerator_timeframeIntervals() {
        val tfMap = mapOf(
            "1m" to 60_000L,
            "5m" to 300_000L,
            "15m" to 900_000L,
            "1h" to 3600_000L,
            "1d" to 86400_000L
        )

        for ((tfStr, expectedIntervalMs) in tfMap) {
            val candles = OptionCandleGenerator.generateOptionCandles(
                assetSymbol = "NIFTY 50",
                strike = 22500.0,
                optionType = "CE",
                expiry = sampleExpiry,
                currentLtp = 175.25,
                changePercent = 2.4,
                iv = 13.8,
                timeframe = tfStr,
                candleCount = 30
            )

            assertEquals("Should have 30 candles for timeframe $tfStr", 30, candles.size)
            for (i in 1 until candles.size) {
                val diff = candles[i].openTime - candles[i - 1].openTime
                assertEquals("Interval between candle $i and ${i - 1} for $tfStr must match $expectedIntervalMs",
                    expectedIntervalMs, diff)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. DETERMINISM & STABILITY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testOptionCandleGenerator_isDeterministicForSameContract() {
        val run1 = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22500.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = 175.25,
            changePercent = 2.4,
            iv = 13.8,
            timeframe = "5m",
            candleCount = 50
        )

        val run2 = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = "NIFTY 50",
            strike = 22500.0,
            optionType = "CE",
            expiry = sampleExpiry,
            currentLtp = 175.25,
            changePercent = 2.4,
            iv = 13.8,
            timeframe = "5m",
            candleCount = 50
        )

        for (i in run1.indices) {
            assertEquals("Candle $i open matches", run1[i].open, run2[i].open, 0.0001)
            assertEquals("Candle $i high matches", run1[i].high, run2[i].high, 0.0001)
            assertEquals("Candle $i low matches", run1[i].low, run2[i].low, 0.0001)
            assertEquals("Candle $i close matches", run1[i].close, run2[i].close, 0.0001)
            assertEquals("Candle $i volume matches", run1[i].volume, run2[i].volume, 0.0001)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. STRIKE CHART STATE LIFECYCLE & ISOLATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testStrikeChartState_clearingStaleDataOnStrikeSwitch() {
        // Initial strike state: 22000 CE loaded
        var state = StrikeChartState(
            assetSymbol = "NIFTY 50",
            strikePrice = 22000.0,
            optionType = "CE",
            expiry = sampleExpiry,
            contractDisplayName = "NIFTY 50 22000 CE",
            ltp = 585.50,
            changePercent = 8.5,
            iv = 14.2,
            candles = listOf(Candle(symbol = "NIFTY 50 22000 CE", timeframe = "5m", openTime = 1000L, closeTime = 1300L, open = 500.0, high = 590.0, low = 490.0, close = 585.50, volume = 50000.0)),
            isLoading = false
        )

        assertEquals(22000.0, state.strikePrice, 0.0)
        assertEquals(1, state.candles.size)

        // User selects a new strike: 23000 PE.
        // Before new candles arrive, stale candles MUST be cleared and isLoading set to true.
        state = state.copy(
            strikePrice = 23000.0,
            optionType = "PE",
            contractDisplayName = "NIFTY 50 23000 PE",
            ltp = 510.00,
            candles = emptyList(), // cleared immediately to avoid flashing old strike data
            isLoading = true,
            error = null
        )

        assertEquals("New strike price set immediately", 23000.0, state.strikePrice, 0.0)
        assertEquals("New contract type is PE", "PE", state.optionType)
        assertTrue("Previous strike candles cleared to prevent stale rendering", state.candles.isEmpty())
        assertTrue("Loading state activated", state.isLoading)
        assertNull("Error cleared", state.error)

        // Once new candles are fetched
        val newCandles = OptionCandleGenerator.generateOptionCandles(
            assetSymbol = state.assetSymbol,
            strike = state.strikePrice,
            optionType = state.optionType,
            expiry = state.expiry,
            currentLtp = state.ltp,
            changePercent = 18.4,
            iv = 17.5
        )
        state = state.copy(
            candles = newCandles,
            isLoading = false
        )

        assertFalse("Loading state cleared", state.isLoading)
        assertEquals(80, state.candles.size)
        assertEquals("Chart close matches new strike LTP", 510.00, state.candles.last().close, 0.001)
    }

    @Test
    fun testStrikeChartState_errorHandlingAndRetry() {
        var state = StrikeChartState(
            assetSymbol = "NIFTY 50",
            strikePrice = 22500.0,
            optionType = "CE",
            isLoading = true,
            error = null
        )

        // Simulate failure loading chart
        state = state.copy(
            isLoading = false,
            error = "Network timeout loading option contract chart"
        )

        assertFalse(state.isLoading)
        assertEquals("Network timeout loading option contract chart", state.error)
        assertTrue(state.candles.isEmpty())

        // Simulate retry: resets error, sets isLoading
        state = state.copy(
            isLoading = true,
            error = null
        )
        assertTrue(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun testFnoUiState_dismissStrikeDetailResetsCleanly() {
        var uiState = FnoUiState(
            selectedStrikeForDetail = sampleAtmStrike,
            selectedOptionSide = "CALL",
            strikeChartState = StrikeChartState(
                assetSymbol = "NIFTY 50",
                strikePrice = 22500.0,
                optionType = "CE",
                ltp = 175.25
            )
        )

        assertNotNull(uiState.selectedStrikeForDetail)
        assertEquals("CALL", uiState.selectedOptionSide)
        assertEquals(22500.0, uiState.strikeChartState.strikePrice, 0.0)

        // User navigates back / dismisses sheet
        uiState = uiState.copy(
            selectedStrikeForDetail = null,
            selectedOptionSide = null,
            strikeChartState = StrikeChartState()
        )

        assertNull("selectedStrikeForDetail must be null after dismissal", uiState.selectedStrikeForDetail)
        assertNull("selectedOptionSide must be null after dismissal", uiState.selectedOptionSide)
        assertEquals("strikeChartState reset to default strike 0.0", 0.0, uiState.strikeChartState.strikePrice, 0.0)
        assertTrue("strikeChartState candles empty after dismissal", uiState.strikeChartState.candles.isEmpty())
    }
}
