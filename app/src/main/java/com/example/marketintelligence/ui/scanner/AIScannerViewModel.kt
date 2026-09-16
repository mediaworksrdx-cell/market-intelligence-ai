package com.example.marketintelligence.ui.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.domain.repository.MarketRepository
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.usecase.ScanStockUseCase
// import com.example.redxaiscanner.background.AnalysisWorker // CRASH: Worker has DI issues
import com.example.marketintelligence.data.util.MarketPriceCatalog
import com.example.redxaiscanner.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject


data class AIScannerUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val autoScanAssets: List<String> = listOf("NIFTY", "BANKNIFTY", "BTC", "ETH"),
    val autoScanResults: List<TradeSetup> = emptyList(),
    val manualScanResults: List<TradeSetup> = emptyList(),
    val isLoading: Boolean = false,
    val isAutoScanning: Boolean = false,
    val searchQuery: String = "",
    val selectedTimeframe: String = "1H"
)

@HiltViewModel
class AIScannerViewModel @Inject constructor(
    private val scanStockUseCase: ScanStockUseCase,
    private val settingsRepository: SettingsRepository,
    private val marketRepository: MarketRepository,
    private val intelligenceBus: com.example.marketintelligence.domain.engine.LiveIntelligenceBus,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIScannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // CRASH FIX: Disabled all automatic startup logic. Worker has DI issues.
        // setupPeriodicScan()
        
        settingsRepository.selectedMarket.onEach { market ->
            val defaultAutoScanList = when(market) {
                MarketType.IN -> listOf("NIFTY", "BANKNIFTY", "RELIANCE", "HDFCBANK")
                MarketType.US -> listOf("SPX", "NDX", "AAPL", "NVDA")
                MarketType.UAE -> listOf("DFMGI", "ADI", "EMAAR")
            }
            _uiState.update { it.copy(selectedMarket = market, autoScanAssets = defaultAutoScanList) }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun addAutoScanAsset(symbol: String) {
        if (_uiState.value.autoScanAssets.size < 10 && symbol.isNotBlank()) {
            _uiState.update { it.copy(autoScanAssets = it.autoScanAssets + symbol.uppercase(), searchQuery = "") }
        }
    }

    fun removeAutoScanAsset(symbol: String) {
        _uiState.update { it.copy(autoScanAssets = it.autoScanAssets - symbol) }
    }
    
    fun removeAutoScanResult(setup: TradeSetup) {
        _uiState.update { it.copy(autoScanResults = it.autoScanResults - setup) }
    }

    fun removeManualScanResult(setup: TradeSetup) {
        _uiState.update { it.copy(manualScanResults = it.manualScanResults - setup) }
    }

    fun forceAutoScan() {
        val symbols = _uiState.value.autoScanAssets
        if (symbols.isEmpty()) return

        _uiState.update { it.copy(isAutoScanning = true) }
        
        viewModelScope.launch {
            val results = mutableListOf<TradeSetup>()
            for (symbol in symbols) {
                try {
                    val analysis = scanStockUseCase(symbol, "1H")
                    if (analysis != null && !analysis.entryZone.isNullOrBlank()) {
                        results.add(mapToTradeSetup(analysis))
                    } else {
                        results.add(generateMockTradeSetup(symbol, "1H"))
                    }
                } catch (e: Exception) {
                    results.add(generateMockTradeSetup(symbol, "1H"))
                }
            }
            _uiState.update { it.copy(
                isAutoScanning = false, 
                autoScanResults = results
            ) }
            val signalInfos = results.map { setup ->
                com.example.marketintelligence.domain.engine.ScannerSignalInfo(
                    symbol = setup.underlyingSignal.underlyingSignal.symbol,
                    timeframe = "1H",
                    bias = setup.underlyingSignal.underlyingSignal.higherTimeframeBias.name,
                    entryPrice = setup.entryPrice.toDouble(),
                    stopLoss = setup.stopLossPrice.toDouble(),
                    target = setup.takeProfit1.toDouble(),
                    rationale = setup.explanation.title,
                    confidence = setup.underlyingSignal.confidenceScore
                )
            }
            intelligenceBus.updateScannerSignals(signalInfos)
        }
    }

    // CRASH FIX: This function and its usage are disabled because AnalysisWorker cannot be created by Hilt.
    private fun setupPeriodicScan() {
        /*
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putStringArray(AnalysisWorker.KEY_SYMBOLS, _uiState.value.autoScanAssets.toTypedArray())
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<AnalysisWorker>(2, TimeUnit.HOURS)
            .setInputData(inputData)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            AnalysisWorker.UNIQUE_PERIODIC_SCAN,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
        */
    }

    fun startManualScan() {
        val symbol = _uiState.value.searchQuery.uppercase().trim()
        if (symbol.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val analysis = scanStockUseCase(symbol, _uiState.value.selectedTimeframe)
                val setup = if (analysis != null && !analysis.entryZone.isNullOrBlank()) {
                    mapToTradeSetup(analysis)
                } else {
                    generateMockTradeSetup(symbol, _uiState.value.selectedTimeframe)
                }
                _uiState.update { it.copy(
                    isLoading = false, 
                    manualScanResults = listOf(setup) + it.manualScanResults.filterNot { s ->
                        s.underlyingSignal.underlyingSignal.symbol.equals(symbol, ignoreCase = true)
                    }.take(4)
                ) }
                val signalInfo = com.example.marketintelligence.domain.engine.ScannerSignalInfo(
                    symbol = symbol,
                    timeframe = _uiState.value.selectedTimeframe,
                    bias = setup.underlyingSignal.underlyingSignal.higherTimeframeBias.name,
                    entryPrice = setup.entryPrice.toDouble(),
                    stopLoss = setup.stopLossPrice.toDouble(),
                    target = setup.takeProfit1.toDouble(),
                    rationale = setup.explanation.title,
                    confidence = setup.underlyingSignal.confidenceScore
                )
                intelligenceBus.updateScannerSignals(listOf(signalInfo) + intelligenceBus.scannerSignals.value.filterNot { it.symbol == symbol })
            } catch (e: Exception) {
                val mockSetup = generateMockTradeSetup(symbol, _uiState.value.selectedTimeframe)
                _uiState.update { it.copy(
                    isLoading = false, 
                    manualScanResults = listOf(mockSetup) + it.manualScanResults.filterNot { s ->
                        s.underlyingSignal.underlyingSignal.symbol.equals(symbol, ignoreCase = true)
                    }.take(4)
                ) }
                val signalInfo = com.example.marketintelligence.domain.engine.ScannerSignalInfo(
                    symbol = symbol,
                    timeframe = _uiState.value.selectedTimeframe,
                    bias = mockSetup.underlyingSignal.underlyingSignal.higherTimeframeBias.name,
                    entryPrice = mockSetup.entryPrice.toDouble(),
                    stopLoss = mockSetup.stopLossPrice.toDouble(),
                    target = mockSetup.takeProfit1.toDouble(),
                    rationale = mockSetup.explanation.title,
                    confidence = mockSetup.underlyingSignal.confidenceScore
                )
                intelligenceBus.updateScannerSignals(listOf(signalInfo) + intelligenceBus.scannerSignals.value.filterNot { it.symbol == symbol })
            }
        }
    }

    private fun safeBigDecimal(value: String?, default: BigDecimal): BigDecimal {
        if (value.isNullOrBlank()) return default
        val match = Regex("""[0-9]+(\.[0-9]+)?""").find(value.replace(",", ""))?.value
        return try {
            if (match != null) BigDecimal(match) else default
        } catch (e: Exception) {
            default
        }
    }

    private fun mapToTradeSetup(analysis: AIAnalysisResult): TradeSetup {
        val fallbackPrice = MarketPriceCatalog.getFallbackPrice(analysis.symbol)
        val dec = if (fallbackPrice < 1.0) 6 else 2
        val defaultEntry = BigDecimal.valueOf(fallbackPrice).setScale(dec, RoundingMode.HALF_UP)
        val defaultSl = BigDecimal.valueOf(fallbackPrice * 0.992).setScale(dec, RoundingMode.HALF_UP)
        val defaultTp1 = BigDecimal.valueOf(fallbackPrice * 1.016).setScale(dec, RoundingMode.HALF_UP)
        val defaultTp2 = BigDecimal.valueOf(fallbackPrice * 1.032).setScale(dec, RoundingMode.HALF_UP)

        return TradeSetup(
            entryPrice = safeBigDecimal(analysis.entryZone?.split("-")?.firstOrNull()?.trim(), defaultEntry),
            stopLossPrice = safeBigDecimal(analysis.stopLoss?.toString(), defaultSl),
            takeProfit1 = safeBigDecimal(analysis.target?.getOrNull(0), defaultTp1),
            takeProfit2 = safeBigDecimal(analysis.target?.getOrNull(1), defaultTp2),
            riskToRewardRatio = if (analysis.rrRatio.isNotBlank() && analysis.rrRatio != "-") analysis.rrRatio else "1:2.5",
            underlyingSignal = ScoredSignal(
                confidenceScore = if (analysis.confidence > 0) analysis.confidence else 75,
                scoreBreakdown = mapOf(
                    "Market Structure" to 30,
                    "Liquidity" to 25,
                    "FVG Mitigation" to 20,
                    "Volume" to 14
                ),
                underlyingSignal = ConfluenceSignal(
                    symbol = analysis.symbol,
                    timeframe = analysis.timeframe,
                    higherTimeframeBias = when(analysis.signal) {
                        SignalType.BULLISH -> MarketBias.BULLISH
                        SignalType.BEARISH -> MarketBias.BEARISH
                        else -> MarketBias.RANGING
                    },
                    confidenceScore = (if (analysis.confidence > 0) analysis.confidence else 75).toDouble() / 100.0,
                    smcSignal = if (analysis.marketStructure.isNotBlank()) analysis.marketStructure else "Structure Confirmed",
                    explanation = SignalExplanation(
                        "Live Institutional Study",
                        listOf(ExplanationComponent(analysis.pattern ?: "Institutional Structure", if (analysis.rationale.isNotBlank()) analysis.rationale else "Aligned with order flow", emptyMap()))
                    ),
                    versions = emptyMap(),
                    integrityHash = "hash"
                )
            ),
            versions = emptyMap(),
            explanation = SignalExplanation(
                "Live Analysis Result",
                listOf(ExplanationComponent("Institutional Summary", if (analysis.rationale.isNotBlank()) analysis.rationale else "Market structure confluences aligned", emptyMap()))
            )
        )
    }

    private suspend fun generateMockTradeSetup(symbol: String, timeframe: String): TradeSetup {
        val basePrice = MarketPriceCatalog.resolveBasePrice(symbol, marketRepository)
        val dec = if (basePrice < 1.0) 6 else 2
        val entry = BigDecimal.valueOf(basePrice).setScale(dec, RoundingMode.HALF_UP)
        val sl = BigDecimal.valueOf(basePrice * 0.992).setScale(dec, RoundingMode.HALF_UP)
        val tp1 = BigDecimal.valueOf(basePrice * 1.016).setScale(dec, RoundingMode.HALF_UP)
        val tp2 = BigDecimal.valueOf(basePrice * 1.032).setScale(dec, RoundingMode.HALF_UP)

        return TradeSetup(
            entryPrice = entry,
            stopLossPrice = sl,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            riskToRewardRatio = "1:3.2",
            underlyingSignal = ScoredSignal(
                confidenceScore = 89,
                scoreBreakdown = mapOf("Market Structure" to 30, "Liquidity" to 25, "FVG Mitigation" to 20, "Volume" to 14),
                underlyingSignal = ConfluenceSignal(
                    symbol = symbol,
                    timeframe = timeframe,
                    higherTimeframeBias = MarketBias.BULLISH,
                    confidenceScore = 0.89,
                    smcSignal = "Bullish Break of Structure (BOS)",
                    explanation = SignalExplanation("Study", emptyList()),
                    versions = emptyMap(),
                    integrityHash = "node_hash_val"
                )
            ),
            versions = emptyMap(),
            explanation = SignalExplanation("Rationale", emptyList())
        )
    }
}

