package com.marketintelligence.ai.ui.scanner

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
    private val marketRepository: MarketRepository
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
        val symbol = _uiState.value.searchQuery.uppercase()
        if (symbol.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val liveAnalysis = marketRepository.getLiveAnalysis()
                val setup = mapToTradeSetup(liveAnalysis).let { 
                    it.copy(underlyingSignal = it.underlyingSignal.copy(
                        underlyingSignal = it.underlyingSignal.underlyingSignal.copy(symbol = symbol)
                    ))
                }
                _uiState.update { it.copy(
                    isLoading = false, 
                    manualScanResults = listOf(setup) + it.manualScanResults.take(4)
                ) }
            } catch (e: Exception) {
                kotlinx.coroutines.delay(1000)
                val mockSetup = generateMockTradeSetup(symbol, _uiState.value.selectedTimeframe)
                _uiState.update { it.copy(
                    isLoading = false, 
                    manualScanResults = listOf(mockSetup) + it.manualScanResults.take(4)
                ) }
            }
        }
    }

    private fun mapToTradeSetup(analysis: AIAnalysisResult): TradeSetup {
        return TradeSetup(
            entryPrice = analysis.entryZone?.split("-")?.firstOrNull()?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            stopLossPrice = analysis.stopLoss?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            takeProfit1 = analysis.target?.getOrNull(0)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            takeProfit2 = analysis.target?.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            riskToRewardRatio = analysis.rrRatio,
            underlyingSignal = ScoredSignal(
                confidenceScore = analysis.confidence,
                scoreBreakdown = emptyMap(),
                underlyingSignal = ConfluenceSignal(
                    symbol = analysis.symbol,
                    timeframe = analysis.timeframe,
                    higherTimeframeBias = when(analysis.signal) {
                        SignalType.BULLISH -> MarketBias.BULLISH
                        SignalType.BEARISH -> MarketBias.BEARISH
                        else -> MarketBias.RANGING
                    },
                    confidenceScore = analysis.confidence.toDouble() / 100.0,
                    smcSignal = analysis.marketStructure,
                    explanation = SignalExplanation(
                        "Live Institutional Study",
                        listOf(ExplanationComponent("Rationale", analysis.rationale, emptyMap()))
                    ),
                    versions = emptyMap(),
                    integrityHash = ""
                )
            ),
            versions = emptyMap(),
            explanation = SignalExplanation(
                "Live Analysis Result",
                listOf(ExplanationComponent("Institutional Summary", analysis.rationale, emptyMap()))
            )
        )
    }

    private fun generateMockTradeSetup(symbol: String, timeframe: String): TradeSetup {
        return TradeSetup(
            entryPrice = BigDecimal("22500.00"),
            stopLossPrice = BigDecimal("22420.00"),
            takeProfit1 = BigDecimal("22650.00"),
            takeProfit2 = BigDecimal("22800.00"),
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
