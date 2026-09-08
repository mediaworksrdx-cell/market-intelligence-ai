package com.marketintelligence.ai.ui.analysis

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.BuildConfig
import com.marketintelligence.ai.domain.engine.ChartEngine
import com.marketintelligence.ai.domain.engine.EngineRouter
import com.marketintelligence.ai.domain.engine.MarketSessionEngine
import com.marketintelligence.ai.domain.engine.MarketSessionInfo
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.usecase.GetHistoricalCandlesUseCase
import com.marketintelligence.ai.domain.usecase.ListenForLiveTicksUseCase
import com.marketintelligence.ai.ui.composable.ChartIndicatorConfig
import com.marketintelligence.ai.ui.composable.ChartStyle
import com.marketintelligence.tradeengine.models.Candle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AnalysisUiState(
    val symbol: String = "",
    val selectedTimeframe: String = "1m",
    val candles: List<Candle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val chartStyle: ChartStyle = ChartStyle.CANDLESTICK,
    val indicatorConfig: ChartIndicatorConfig = ChartIndicatorConfig(),
    val sessionInfo: MarketSessionInfo? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistoricalCandlesUseCase: GetHistoricalCandlesUseCase,
    private val listenForLiveTicksUseCase: ListenForLiveTicksUseCase,
    private val sessionEngine: MarketSessionEngine,
    val engineRouter: EngineRouter
) : ViewModel() {

    private val symbol: String = savedStateHandle["symbol"] ?: "BTCUSD"
    private val type: String = savedStateHandle["type"] ?: "CRYPTO"

    private val marketType: MarketType = when {
        symbol.endsWith(".NS") || symbol.endsWith(".BO") || symbol.contains("NIFTY") -> MarketType.IN
        symbol.contains("DFM") || symbol.contains("ADX") -> MarketType.UAE
        else -> MarketType.US
    }

    private val _uiState = MutableStateFlow(
        AnalysisUiState(
            symbol = symbol,
            sessionInfo = sessionEngine.getSessionInfo(marketType)
        )
    )
    val uiState = _uiState.asStateFlow()

    val activeChartEngine: StateFlow<ChartEngine?> = engineRouter.activeChartEngine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        fetchHistoricalData()
        listenForLivePrices()
    }

    fun onChartStyleSelected(style: ChartStyle) {
        _uiState.update { it.copy(chartStyle = style) }
    }

    fun toggleSMC() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showSMC = !it.indicatorConfig.showSMC)) }
    }

    fun toggleEMA() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showEMA = !it.indicatorConfig.showEMA)) }
    }

    fun toggleBollinger() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showBollinger = !it.indicatorConfig.showBollinger)) }
    }

    fun toggleVWAP() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showVWAP = !it.indicatorConfig.showVWAP)) }
    }

    fun toggleVolume() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showVolume = !it.indicatorConfig.showVolume)) }
    }

    fun toggleRSI() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showRSI = !it.indicatorConfig.showRSI)) }
    }

    fun toggleMACD() {
        _uiState.update { it.copy(indicatorConfig = it.indicatorConfig.copy(showMACD = !it.indicatorConfig.showMACD)) }
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe, candles = emptyList()) }
        fetchHistoricalData()
    }

    private fun fetchHistoricalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch the last 24 hours of data
                val to = System.currentTimeMillis()
                val from = to - TimeUnit.HOURS.toMillis(24)

                val historicalCandles = getHistoricalCandlesUseCase.execute(
                    symbol = _uiState.value.symbol,
                    timeframe = _uiState.value.selectedTimeframe,
                    from = from,
                    to = to,
                    type = type
                )
                _uiState.update { it.copy(candles = historicalCandles, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun listenForLivePrices() {
        val endpoint = BuildConfig.WS_BASE_URL.ifBlank { BuildConfig.BASE_URL }
        listenForLiveTicksUseCase.execute(endpoint)
            .conflate()
            .catch { e ->
                Log.e("AnalysisViewModel", "Error in live tick stream: ${e.message}", e)
                _uiState.update { it.copy(error = e.message ?: "Live tick stream error") }
            }
            .filter { tick ->
                val currentSym = _uiState.value.symbol
                tick.symbol.equals(currentSym, ignoreCase = true) ||
                tick.symbol.equals(currentSym.removeSuffix(".NS"), ignoreCase = true) ||
                "${tick.symbol}.NS".equals(currentSym, ignoreCase = true)
            }
            .onEach { tick ->
                // The CurrentCandleManager will process the tick and update the database.
                // We can then listen to database changes to update the UI.
                // For now, we will just update the last candle for instant UI feedback.
                listenForLiveTicksUseCase.processTick(tick)
                _uiState.update { currentState ->
                    val updatedCandles = currentState.candles.toMutableList()
                    if (updatedCandles.isNotEmpty()) {
                        val lastCandle = updatedCandles.last()
                        if (tick.timestamp >= lastCandle.closeTime) {
                            // A new candle should be formed, which will be handled by the CurrentCandleManager.
                            // We can refetch from the DB or wait for a DB update notification.
                            // For simplicity, we just add a new candle here for now.
                            val newCandle = Candle(
                                symbol = tick.symbol,
                                timeframe = currentState.selectedTimeframe,
                                openTime = lastCandle.closeTime,
                                open = tick.price,
                                high = tick.price,
                                low = tick.price,
                                close = tick.price,
                                volume = tick.volume,
                                closeTime = lastCandle.closeTime + (lastCandle.closeTime - lastCandle.openTime)
                            )
                            updatedCandles.add(newCandle)
                        } else {
                            val updatedLastCandle = lastCandle.copy(
                                high = maxOf(lastCandle.high, tick.price),
                                low = minOf(lastCandle.low, tick.price),
                                close = tick.price,
                                volume = lastCandle.volume + tick.volume
                            )
                            updatedCandles[updatedCandles.lastIndex] = updatedLastCandle
                        }
                        currentState.copy(candles = updatedCandles)
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
