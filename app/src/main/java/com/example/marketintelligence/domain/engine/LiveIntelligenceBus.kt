package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.model.AIAnalysisResult
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
    val bias: String, // BULLISH / BEARISH / NEUTRAL
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
        "NIFTY 50" to LivePriceInfo("NIFTY 50", 22716.20, -64.05, -0.28),
        "NIFTY" to LivePriceInfo("NIFTY", 22716.20, -64.05, -0.28),
        "SENSEX" to LivePriceInfo("SENSEX", 72529.07, -242.65, -0.33),
        "BANKNIFTY" to LivePriceInfo("BANKNIFTY", 54259.95, -211.70, -0.39),
        "FINNIFTY" to LivePriceInfo("FINNIFTY", 24648.50, -23.15, -0.09),
        "RELIANCE" to LivePriceInfo("RELIANCE", 1182.00, -15.60, -1.30),
        "RELIANCE.NS" to LivePriceInfo("RELIANCE.NS", 1182.00, -15.60, -1.30),
        "HDFCBANK" to LivePriceInfo("HDFCBANK", 722.70, 3.65, 0.51),
        "HDFCBANK.NS" to LivePriceInfo("HDFCBANK.NS", 722.70, 3.65, 0.51),
        "TCS" to LivePriceInfo("TCS", 2032.40, -38.30, -1.85),
        "TCS.NS" to LivePriceInfo("TCS.NS", 2032.40, -38.30, -1.85),
        "INFY" to LivePriceInfo("INFY", 1015.40, 12.20, 1.22),
        "INFY.NS" to LivePriceInfo("INFY.NS", 1015.40, 12.20, 1.22),
        "ICICIBANK" to LivePriceInfo("ICICIBANK", 1292.20, -9.80, -0.75),
        "ICICIBANK.NS" to LivePriceInfo("ICICIBANK.NS", 1292.20, -9.80, -0.75),
        "SBIN" to LivePriceInfo("SBIN", 964.70, 2.70, 0.28),
        "SBIN.NS" to LivePriceInfo("SBIN.NS", 964.70, 2.70, 0.28),
        "TATAMOTORS" to LivePriceInfo("TATAMOTORS", 280.95, -0.80, -0.28),
        "TATAMOTORS.NS" to LivePriceInfo("TATAMOTORS.NS", 280.95, -0.80, -0.28),
        "BHARTIARTL" to LivePriceInfo("BHARTIARTL", 1771.20, -0.20, -0.01),
        "BHARTIARTL.NS" to LivePriceInfo("BHARTIARTL.NS", 1771.20, -0.20, -0.01),
        "ITC" to LivePriceInfo("ITC", 265.10, -0.10, -0.04),
        "ITC.NS" to LivePriceInfo("ITC.NS", 265.10, -0.10, -0.04),
        "LT" to LivePriceInfo("LT", 3749.10, -17.30, -0.46),
        "LT.NS" to LivePriceInfo("LT.NS", 3749.10, -17.30, -0.46),
        "BTC" to LivePriceInfo("BTC", 83923.0, 1140.0, 1.45),
        "ETH" to LivePriceInfo("ETH", 2495.0, 18.5, 0.75),
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
            entryPrice = 1182.0,
            stopLoss = 1165.0,
            target = 1220.0,
            rationale = "Bullish SMC Demand Zone mitigation with institutional volume absorption.",
            confidence = 88
        ),
        ScannerSignalInfo(
            symbol = "NIFTY",
            timeframe = "15m",
            bias = "BULLISH",
            entryPrice = 22716.20,
            stopLoss = 22650.0,
            target = 22850.0,
            rationale = "Liquidity sweep below previous day low followed by rapid change of character (CHoCH).",
            confidence = 84
        ),
        ScannerSignalInfo(
            symbol = "BTC",
            timeframe = "4H",
            bias = "BULLISH",
            entryPrice = 83923.0,
            stopLoss = 82500.0,
            target = 86500.0,
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
        spotPrice = 22716.20,
        changePercent = -0.28,
        regime = "TRENDING BULLISH ACCUMULATION",
        pcr = 1.05,
        maxPain = 22700.0,
        callWall = 22800.0,
        putWall = 22600.0,
        institutionalBias = "FII Long Gamma Pinning with Call Writing at 22,800 resistance."
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
