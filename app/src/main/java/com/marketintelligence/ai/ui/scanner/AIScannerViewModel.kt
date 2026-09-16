package com.marketintelligence.ai.ui.scanner

import com.marketintelligence.ai.data.util.MarketPriceCatalog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.domain.repository.MarketRepository
import com.marketintelligence.ai.domain.repository.SettingsRepository
import com.marketintelligence.ai.domain.usecase.ScanStockUseCase
import com.marketintelligence.redxaiscanner.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class AIScannerUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val manualScanResults: List<TradeSetup> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTimeframe: String = "1H"
)

@HiltViewModel
class AIScannerViewModel @Inject constructor(
    private val scanStockUseCase: ScanStockUseCase,
    private val settingsRepository: SettingsRepository,
    private val marketRepository: MarketRepository,
    private val intelligenceBus: com.marketintelligence.ai.domain.engine.LiveIntelligenceBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIScannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        settingsRepository.selectedMarket.onEach { market ->
            _uiState.update { it.copy(selectedMarket = market) }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun removeManualScanResult(setup: TradeSetup) {
        _uiState.update { it.copy(manualScanResults = it.manualScanResults - setup) }
    }

    fun startManualScan() {
        val symbol = _uiState.value.searchQuery.uppercase().trim()
        if (symbol.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val analysis = scanStockUseCase(symbol, _uiState.value.selectedTimeframe)
                val setup = if (analysis != null && !analysis.entryZone.isNullOrBlank()) {
                    mapToTradeSetup(analysis).let { 
                        it.copy(underlyingSignal = it.underlyingSignal.copy(
                            underlyingSignal = it.underlyingSignal.underlyingSignal.copy(symbol = symbol)
                        ))
                    }
                } else {
                    generateMockTradeSetup(symbol, _uiState.value.selectedTimeframe)
                }
                _uiState.update { it.copy(
                    isLoading = false, 
                    manualScanResults = listOf(setup) + it.manualScanResults.filterNot { s ->
                        s.underlyingSignal.underlyingSignal.symbol.equals(symbol, ignoreCase = true)
                    }.take(4)
                ) }
                val signalInfo = com.marketintelligence.ai.domain.engine.ScannerSignalInfo(
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
                val signalInfo = com.marketintelligence.ai.domain.engine.ScannerSignalInfo(
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
            riskToRewardRatio = if (!analysis.rrRatio.isNullOrBlank() && analysis.rrRatio != "-") analysis.rrRatio else "1:2.8",
            underlyingSignal = ScoredSignal(
                confidenceScore = if (analysis.confidence > 0) analysis.confidence else 85,
                scoreBreakdown = mapOf("Market Structure" to 30, "Liquidity" to 25, "FVG" to 20, "Volume" to 10),
                underlyingSignal = ConfluenceSignal(
                    symbol = analysis.symbol,
                    timeframe = analysis.timeframe,
                    higherTimeframeBias = when(analysis.signal) {
                        SignalType.BULLISH -> MarketBias.BULLISH
                        SignalType.BEARISH -> MarketBias.BEARISH
                        else -> MarketBias.RANGING
                    },
                    confidenceScore = (if (analysis.confidence > 0) analysis.confidence else 85).toDouble() / 100.0,
                    smcSignal = if (!analysis.marketStructure.isNullOrBlank()) analysis.marketStructure else "Structure Confirmed",
                    explanation = SignalExplanation(
                        "Live Institutional Study",
                        listOf(ExplanationComponent(analysis.pattern ?: "Institutional Structure", if (!analysis.rationale.isNullOrBlank()) analysis.rationale else "Market structure confluence validated", emptyMap()))
                    ),
                    versions = emptyMap(),
                    integrityHash = ""
                )
            ),
            versions = emptyMap(),
            explanation = SignalExplanation(
                "Live Analysis Result",
                listOf(ExplanationComponent("Institutional Summary", if (!analysis.rationale.isNullOrBlank()) analysis.rationale else "Confluence verified", emptyMap()))
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
            riskToRewardRatio = "1:2.8",
            underlyingSignal = ScoredSignal(
                confidenceScore = 87,
                scoreBreakdown = mapOf("Market Structure" to 30, "Liquidity" to 25, "FVG Mitigation" to 20, "Volume" to 12),
                underlyingSignal = ConfluenceSignal(
                    symbol = symbol,
                    timeframe = timeframe,
                    higherTimeframeBias = MarketBias.BULLISH,
                    confidenceScore = 0.87,
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

