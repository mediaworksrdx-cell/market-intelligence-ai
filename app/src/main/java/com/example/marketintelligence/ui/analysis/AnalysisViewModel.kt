package com.example.marketintelligence.ui.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.engine.ChartEngine
import com.example.marketintelligence.domain.model.AIAnalysisResult
import com.example.marketintelligence.data.local.MockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AnalysisUiState(
    val symbol: String = "",
    val selectedTimeframe: String = "1H",
    val aiAnalysis: AIAnalysisResult? = null,
    val technicalIndicators: TechnicalIndicators = TechnicalIndicators(),
    val isLoading: Boolean = false
)

data class TechnicalIndicators(
    val rsi: Double = 54.2,
    val ema20: Double = 22450.0,
    val ema50: Double = 22100.0,
    val ema200: Double = 21500.0,
    val macd: String = "Bullish Crossover",
    val adx: Double = 24.5
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val engineRouter: EngineRouter
) : ViewModel() {
    
    private val symbol: String = savedStateHandle["symbol"] ?: "NIFTY"
    
    private val _uiState = MutableStateFlow(AnalysisUiState(symbol = symbol))
    val uiState = _uiState.asStateFlow()

    val activeChartEngine: Flow<ChartEngine> = engineRouter.activeChartEngine

    init {
        loadAnalysisData(symbol, _uiState.value.selectedTimeframe)
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadAnalysisData(symbol, timeframe)
    }

    private fun loadAnalysisData(symbol: String, timeframe: String) {
        _uiState.update { it.copy(
            aiAnalysis = MockData.NOTIFICATIONS_MOCK.firstOrNull()?.aiResult?.copy(symbol = symbol, timeframe = timeframe)
        ) }
    }
}
