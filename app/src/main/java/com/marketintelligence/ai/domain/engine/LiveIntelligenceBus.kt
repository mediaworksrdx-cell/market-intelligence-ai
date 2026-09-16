package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class LivePriceInfo(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class ScannerSignalInfo(
    val symbol: String,
    val timeframe: String,
    val bias: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val target: Double,
    val rationale: String,
    val confidence: Int
)

data class FnoIntelligence(
    val symbol: String,
    val spotPrice: Double,
    val changePercent: Double,
    val regime: String,
    val pcr: Double,
    val maxPain: Double,
    val callWall: Double,
    val putWall: Double,
    val institutionalBias: String
)

data class PortfolioHoldingInfo(
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercent: Double
)

data class PortfolioIntelligence(
    val totalCurrentValue: Double,
    val totalInvestedValue: Double,
    val totalPnl: Double,
    val todayPnl: Double,
    val holdings: List<PortfolioHoldingInfo>
)

/**
 * Central event and state bus unifying Live Prices, AI Scanner Signals,
 * F&O Scanner Intelligence, and Portfolio Context for AI Mentor.
 */
@Singleton
class LiveIntelligenceBus @Inject constructor() {

    // --- 1. Live Prices ---
    private val defaultPrices = mapOf(
        "NIFTY 50" to LivePriceInfo("NIFTY 50", 23600.0, -144.05, -0.61),
        "NIFTY" to LivePriceInfo("NIFTY", 23600.0, -144.05, -0.61),
        "SENSEX" to LivePriceInfo("SENSEX", 77200.0, -555.23, -0.73),
        "BANKNIFTY" to LivePriceInfo("BANKNIFTY", 50400.0, -210.50, -0.42),
        "RELIANCE" to LivePriceInfo("RELIANCE", 1301.0, -14.60, -1.11),
        "RELIANCE.NS" to LivePriceInfo("RELIANCE.NS", 1301.0, -14.60, -1.11),
        "HDFCBANK" to LivePriceInfo("HDFCBANK", 1720.0, 12.40, 0.73),
        "HDFCBANK.NS" to LivePriceInfo("HDFCBANK.NS", 1720.0, 12.40, 0.73),
        "TCS" to LivePriceInfo("TCS", 2199.5, -56.0, -2.48),
        "TCS.NS" to LivePriceInfo("TCS.NS", 2199.5, -56.0, -2.48),
        "INFY" to LivePriceInfo("INFY", 1047.5, -34.5, -3.19),
        "INFY.NS" to LivePriceInfo("INFY.NS", 1047.5, -34.5, -3.19),
        "ICICIBANK" to LivePriceInfo("ICICIBANK", 1389.8, -9.6, -0.69),
        "ICICIBANK.NS" to LivePriceInfo("ICICIBANK.NS", 1389.8, -9.6, -0.69),
        "SBIN" to LivePriceInfo("SBIN", 1002.0, -6.0, -0.60),
        "SBIN.NS" to LivePriceInfo("SBIN.NS", 1002.0, -6.0, -0.60),
        "BTC" to LivePriceInfo("BTC", 79227.0, 1140.0, 1.45),
        "ETH" to LivePriceInfo("ETH", 2480.0, 18.5, 0.75),
        "SPX" to LivePriceInfo("SPX", 5485.88, -14.12, -0.26),
        "DFMGI" to LivePriceInfo("DFMGI", 4849.37, 5.20, 0.11)
    )

    private val _livePrices = MutableStateFlow<Map<String, LivePriceInfo>>(defaultPrices)
    val livePrices = _livePrices.asStateFlow()

    fun updateLivePrice(symbol: String, price: Double, change: Double, changePercent: Double) {
        val upper = symbol.uppercase().trim()
        val clean = upper.removeSuffix(".NS").removeSuffix(".BO")
        val info = LivePriceInfo(symbol = upper, price = price, change = change, changePercent = changePercent)
        val current = _livePrices.value.toMutableMap()
        current[upper] = info
        current[clean] = info
        _livePrices.value = current
    }

    fun getLivePrice(symbol: String): LivePriceInfo? {
        val upper = symbol.uppercase().trim()
        val clean = upper.removeSuffix(".NS").removeSuffix(".BO")
        return _livePrices.value[upper] ?: _livePrices.value[clean]
    }

    // --- 2. AI Scanner Signals ---
    private val defaultScannerSignals = listOf(
        ScannerSignalInfo(
            symbol = "RELIANCE",
            timeframe = "1H",
            bias = "BULLISH",
            entryPrice = 1301.0,
            stopLoss = 1285.0,
            target = 1340.0,
            rationale = "Bullish SMC Demand Zone mitigation with institutional volume absorption.",
            confidence = 88
        ),
        ScannerSignalInfo(
            symbol = "NIFTY",
            timeframe = "15m",
            bias = "BULLISH",
            entryPrice = 23600.0,
            stopLoss = 23520.0,
            target = 23780.0,
            rationale = "Liquidity sweep below previous day low followed by rapid change of character (CHoCH).",
            confidence = 84
        ),
        ScannerSignalInfo(
            symbol = "BTC",
            timeframe = "4H",
            bias = "BULLISH",
            entryPrice = 79227.0,
            stopLoss = 77500.0,
            target = 83500.0,
            rationale = "Higher timeframe liquidity run confirmed with positive delta volume.",
            confidence = 91
        )
    )

    private val _scannerSignals = MutableStateFlow<List<ScannerSignalInfo>>(defaultScannerSignals)
    val scannerSignals = _scannerSignals.asStateFlow()

    fun updateScannerSignals(signals: List<ScannerSignalInfo>) {
        if (signals.isNotEmpty()) {
            _scannerSignals.value = signals
        }
    }

    // --- 3. F&O Scanner Intelligence ---
    private val defaultFno = FnoIntelligence(
        symbol = "NIFTY 50",
        spotPrice = 23600.0,
        changePercent = -0.61,
        regime = "TRENDING BULLISH ACCUMULATION",
        pcr = 1.05,
        maxPain = 23500.0,
        callWall = 23800.0,
        putWall = 23400.0,
        institutionalBias = "FII Long Gamma Pinning with Call Writing at 23,800 resistance."
    )

    private val _fnoIntelligence = MutableStateFlow<FnoIntelligence>(defaultFno)
    val fnoIntelligence = _fnoIntelligence.asStateFlow()

    fun updateFnoIntelligence(info: FnoIntelligence) {
        _fnoIntelligence.value = info
    }

    // --- 4. Portfolio Context ---
    private val defaultPortfolio = PortfolioIntelligence(
        totalCurrentValue = 485200.0,
        totalInvestedValue = 452000.0,
        totalPnl = 33200.0,
        todayPnl = 3450.0,
        holdings = listOf(
            PortfolioHoldingInfo("RELIANCE.NS", 15.0, 1285.0, 1301.0, 240.0, 1.24),
            PortfolioHoldingInfo("HDFCBANK.NS", 30.0, 1690.0, 1720.0, 900.0, 1.78),
            PortfolioHoldingInfo("INFY.NS", 20.0, 1815.0, 1850.0, 700.0, 1.93),
            PortfolioHoldingInfo("BTC", 1.0, 76500.0, 79227.0, 2727.0, 3.56)
        )
    )

    private val _portfolioIntelligence = MutableStateFlow<PortfolioIntelligence>(defaultPortfolio)
    val portfolioIntelligence = _portfolioIntelligence.asStateFlow()

    fun updatePortfolioIntelligence(info: PortfolioIntelligence) {
        _portfolioIntelligence.value = info
    }

    // --- Backward Compatible Signal Streams ---
    private val _fnoSignalFlow = MutableSharedFlow<AiTradeSignal>(replay = 1)
    val fnoSignalFlow = _fnoSignalFlow.asSharedFlow()

    suspend fun postFnoSignal(signal: AiTradeSignal) {
        _fnoSignalFlow.emit(signal)
    }

    private val _scannerSignalFlow = MutableSharedFlow<AIAnalysisResult>(replay = 1)
    val scannerSignalFlow = _scannerSignalFlow.asSharedFlow()

    suspend fun postScannerSignal(signal: AIAnalysisResult) {
        _scannerSignalFlow.emit(signal)
    }
}
