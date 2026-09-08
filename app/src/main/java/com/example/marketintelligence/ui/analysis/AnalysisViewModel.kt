package com.example.marketintelligence.ui.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.domain.chart.*
import com.example.marketintelligence.domain.usecase.GetHistoricalCandlesUseCase
import com.example.marketintelligence.domain.usecase.ListenForLiveTicksUseCase
import com.example.marketintelligence.ui.chart.ChartState
import com.example.tradeengine.models.Candle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AnalysisUiState(
    val symbol: String = "",
    val selectedTimeframe: String = "1m",
    val candles: List<Candle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val chartState: ChartState = ChartState(),
    val showIndicatorSheet: Boolean = false,
    val showDrawingPalette: Boolean = false,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistoricalCandlesUseCase: GetHistoricalCandlesUseCase,
    private val listenForLiveTicksUseCase: ListenForLiveTicksUseCase
) : ViewModel() {

    private val symbol: String = savedStateHandle["symbol"] ?: "BTCUSD"
    private val type: String = savedStateHandle["type"] ?: "CRYPTO"

    private val _uiState = MutableStateFlow(AnalysisUiState(symbol = symbol))
    val uiState = _uiState.asStateFlow()

    init {
        fetchHistoricalData()
        listenForLivePrices()
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe, candles = emptyList()) }
        fetchHistoricalData()
    }

    // ── Chart Type ──
    fun onChartTypeSelected(chartType: ChartType) {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(chartType = chartType))
        }
    }

    // ── Indicator Management ──
    fun toggleIndicatorSheet() {
        _uiState.update { it.copy(showIndicatorSheet = !it.showIndicatorSheet, showDrawingPalette = false) }
    }

    fun toggleIndicator(type: IndicatorType) {
        _uiState.update { state ->
            val current = state.chartState.activeIndicators.toMutableList()
            val existing = current.find { it.type == type }
            if (existing != null) {
                current.remove(existing)
                if (existing.enabled) {
                    current.add(existing.copy(enabled = false))
                } else {
                    current.add(existing.copy(enabled = true))
                }
            } else {
                current.add(IndicatorConfig(type = type, enabled = true))
            }
            val newChartState = state.chartState.copy(activeIndicators = current)
            state.copy(chartState = recalculateIndicators(newChartState, state.candles))
        }
    }

    fun updateIndicator(config: IndicatorConfig) {
        _uiState.update { state ->
            val current = state.chartState.activeIndicators.toMutableList()
            val index = current.indexOfFirst { it.type == config.type }
            if (index >= 0) {
                current[index] = config
            }
            val newChartState = state.chartState.copy(activeIndicators = current)
            state.copy(chartState = recalculateIndicators(newChartState, state.candles))
        }
    }

    // ── Drawing Tool Management ──
    fun toggleDrawingPalette() {
        _uiState.update { it.copy(showDrawingPalette = !it.showDrawingPalette, showIndicatorSheet = false) }
    }

    fun selectDrawingTool(tool: DrawingToolType) {
        _uiState.update { state ->
            state.copy(
                chartState = state.chartState.copy(
                    activeDrawingTool = tool,
                    currentDrawing = null
                )
            )
        }
    }

    fun addDrawingPoint(point: ChartPoint) {
        _uiState.update { state ->
            val chartState = state.chartState
            val tool = chartState.activeDrawingTool
            if (tool == DrawingToolType.NONE) return@update state

            val currentDrawing = chartState.currentDrawing
            val requiredPoints = when (tool) {
                DrawingToolType.HORIZONTAL_LINE -> 1
                DrawingToolType.TRENDLINE, DrawingToolType.FIBONACCI, DrawingToolType.RECTANGLE -> 2
                DrawingToolType.CHANNEL -> 3
                DrawingToolType.NONE -> 0
            }

            if (currentDrawing == null) {
                // Start a new drawing
                val newDrawing = DrawingData(
                    toolType = tool,
                    points = listOf(point),
                    color = getDrawingColor(tool),
                    isComplete = requiredPoints == 1
                )
                if (requiredPoints == 1) {
                    // Single-point tool (horizontal line) — complete immediately
                    val updatedDrawings = chartState.drawings + newDrawing
                    state.copy(chartState = chartState.copy(
                        drawings = updatedDrawings,
                        currentDrawing = null
                    ))
                } else {
                    state.copy(chartState = chartState.copy(currentDrawing = newDrawing))
                }
            } else {
                // Add point to existing drawing
                val updatedPoints = currentDrawing.points + point
                if (updatedPoints.size >= requiredPoints) {
                    // Drawing complete
                    val completedDrawing = currentDrawing.copy(points = updatedPoints, isComplete = true)
                    val updatedDrawings = chartState.drawings + completedDrawing
                    state.copy(chartState = chartState.copy(
                        drawings = updatedDrawings,
                        currentDrawing = null
                    ))
                } else {
                    state.copy(chartState = chartState.copy(
                        currentDrawing = currentDrawing.copy(points = updatedPoints)
                    ))
                }
            }
        }
    }

    fun clearDrawings() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(drawings = emptyList(), currentDrawing = null))
        }
    }

    // ── Indicator Computation ──
    private fun recalculateIndicators(chartState: ChartState, candles: List<Candle>): ChartState {
        if (candles.isEmpty()) return chartState

        val results = mutableMapOf<IndicatorType, Any>()
        for (config in chartState.activeIndicators) {
            if (!config.enabled) continue
            val result: Any? = when (config.type) {
                IndicatorType.SMA -> IndicatorCalculator.calculateSMA(candles, config.period)
                IndicatorType.EMA -> IndicatorCalculator.calculateEMA(candles, config.period)
                IndicatorType.RSI -> IndicatorCalculator.calculateRSI(candles, config.period)
                IndicatorType.MACD -> IndicatorCalculator.calculateMACD(candles, config.period, config.secondaryPeriod)
                IndicatorType.BOLLINGER_BANDS -> IndicatorCalculator.calculateBollingerBands(candles, config.period, config.multiplier)
                IndicatorType.VWAP -> IndicatorCalculator.calculateVWAP(candles)
                IndicatorType.SUPERTREND -> IndicatorCalculator.calculateSupertrend(candles, config.period, config.multiplier)
                IndicatorType.ATR -> IndicatorCalculator.calculateATR(candles, config.period)
                IndicatorType.STOCHASTIC -> IndicatorCalculator.calculateStochastic(candles, config.period, config.secondaryPeriod)
                IndicatorType.ICHIMOKU -> IndicatorCalculator.calculateIchimoku(candles, config.period, config.secondaryPeriod)
                IndicatorType.CVD -> IndicatorCalculator.calculateCVD(candles)
            }
            if (result != null) results[config.type] = result
        }

        val smc = IndicatorCalculator.detectSMC(candles)
        val lastClose = candles.last().close
        val strikeStep = if (lastClose > 10000) 100.0 else if (lastClose > 1000) 50.0 else 10.0
        val baseStrike = (kotlin.math.round(lastClose / strikeStep)) * strikeStep

        val fno = chartState.fnoLevels ?: FnoOverlayLevels(
            callWall = baseStrike + strikeStep * 3,
            putWall = baseStrike - strikeStep * 3,
            gammaFlip = baseStrike - strikeStep * 0.5,
            maxPain = baseStrike - strikeStep,
            callWallGex = 1.45e9,
            putWallGex = -1.18e9,
            totalNetGex = 2.7e8
        )

        return chartState.copy(
            indicatorResults = results,
            smcAnalysis = smc,
            fnoLevels = fno
        )
    }

    fun toggleVolumeProfile() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(showVolumeProfile = !state.chartState.showVolumeProfile))
        }
    }

    fun toggleFnoOverlay() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(showFnoOverlay = !state.chartState.showFnoOverlay))
        }
    }

    fun toggleSmcOverlay() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(showSmcOverlay = !state.chartState.showSmcOverlay))
        }
    }

    private fun getDrawingColor(tool: DrawingToolType): Long = when (tool) {
        DrawingToolType.TRENDLINE -> 0xFF42A5F5
        DrawingToolType.HORIZONTAL_LINE -> 0xFFFFB74D
        DrawingToolType.FIBONACCI -> 0xFFCE93D8
        DrawingToolType.RECTANGLE -> 0xFF81C784
        DrawingToolType.CHANNEL -> 0xFF4FC3F7
        DrawingToolType.NONE -> 0xFFFFFFFF
    }

    // ── Data Fetching ──
    private fun fetchHistoricalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val to = System.currentTimeMillis()
                val from = to - TimeUnit.HOURS.toMillis(24)

                val historicalCandles = getHistoricalCandlesUseCase.execute(
                    symbol = _uiState.value.symbol,
                    timeframe = _uiState.value.selectedTimeframe,
                    from = from,
                    to = to,
                    type = type
                )
                _uiState.update { state ->
                    val newChartState = recalculateIndicators(
                        state.chartState.copy(candles = historicalCandles),
                        historicalCandles
                    )
                    state.copy(candles = historicalCandles, isLoading = false, chartState = newChartState)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun listenForLivePrices() {
        listenForLiveTicksUseCase.execute("http://65.2.28.48:8080/")
            .onEach { tick ->
                listenForLiveTicksUseCase.processTick(tick)
                _uiState.update { currentState ->
                    val updatedCandles = currentState.candles.toMutableList()
                    if (updatedCandles.isNotEmpty()) {
                        val lastCandle = updatedCandles.last()
                        if (tick.timestamp >= lastCandle.closeTime) {
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
                        val newChartState = recalculateIndicators(
                            currentState.chartState.copy(candles = updatedCandles),
                            updatedCandles
                        )
                        currentState.copy(candles = updatedCandles, chartState = newChartState)
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}

