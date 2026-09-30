package com.example.marketintelligence.ui.analysis

import android.util.Log
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AnalysisUiState(
    val symbol: String = "",
    val selectedTimeframe: String = "1D",
    val candles: List<Candle> = emptyList(),
    val currentPrice: Double = 0.0,
    val priceChange: Double = 0.0,
    val priceChangePercent: Double = 0.0,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val error: String? = null,
    val chartState: ChartState = ChartState(),
    val showIndicatorSheet: Boolean = false,
    val targetIndicatorType: IndicatorType? = null,
    val showDrawingPalette: Boolean = false,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistoricalCandlesUseCase: GetHistoricalCandlesUseCase,
    private val listenForLiveTicksUseCase: ListenForLiveTicksUseCase,
    private val candleRepository: com.example.marketintelligence.data.source.CandleRepository,
    private val marketRepository: com.example.marketintelligence.domain.repository.MarketRepository,
    private val application: android.app.Application
) : ViewModel() {

    // Track cumulative volume per symbol to compute per-tick delta
    // Kite sends volumeTradedToday (cumulative), so we store previous value and subtract
    private val lastCumulativeVolume = java.util.concurrent.ConcurrentHashMap<String, Double>()

    private val rawSymbol: String = savedStateHandle["symbol"] ?: "BTCUSD"
    private val symbol: String = try {
        java.net.URLDecoder.decode(rawSymbol, "UTF-8").trim()
    } catch (_: Exception) {
        rawSymbol.replace("%20", " ").replace("+", " ").trim()
    }
    private val type: String = savedStateHandle["type"] ?: "CRYPTO"

    private val _uiState = MutableStateFlow(AnalysisUiState(symbol = symbol))
    val uiState = _uiState.asStateFlow()

    init {
        val savedDrawings = ChartState.loadDrawings(application, symbol)
        _uiState.update { it.copy(chartState = it.chartState.copy(drawings = savedDrawings)) }
        
        viewModelScope.launch {
            uiState.map { it.chartState.drawings }.distinctUntilChanged().collect { drawings ->
                ChartState.saveDrawings(application, symbol, drawings)
            }
        }

        fetchHistoricalData()
        listenForLivePrices()
        startPeriodicCryptoPolling()
        startPeriodicLivePricePolling()
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

    // ── Cursor Mode ──
    fun setCursorMode(mode: CursorMode) {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(cursorMode = mode))
        }
    }

    // ── Indicator Management ──
    fun toggleIndicatorSheet() {
        _uiState.update { it.copy(showIndicatorSheet = !it.showIndicatorSheet, showDrawingPalette = false, targetIndicatorType = null) }
    }

    fun openIndicatorSettings(type: IndicatorType? = null) {
        _uiState.update { it.copy(showIndicatorSheet = true, showDrawingPalette = false, targetIndicatorType = type) }
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
            val candles = if (state.candles.isNotEmpty()) state.candles else state.chartState.candles
            val newChartState = state.chartState.copy(activeIndicators = current, candles = candles)
            state.copy(candles = candles, chartState = recalculateIndicators(newChartState, candles))
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

    fun moveDrawingPoint(drawingId: String, pointIndex: Int, newPoint: ChartPoint) {
        _uiState.update { state ->
            state.chartState.pushDrawingState()
            val updatedDrawings = state.chartState.drawings.map { drawing ->
                if (drawing.id == drawingId && pointIndex in drawing.points.indices) {
                    val updatedPoints = drawing.points.toMutableList()
                    updatedPoints[pointIndex] = newPoint
                    drawing.copy(points = updatedPoints)
                } else {
                    drawing
                }
            }
            state.copy(
                chartState = state.chartState.copy(drawings = updatedDrawings)
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
                DrawingToolType.VERTICAL_LINE, DrawingToolType.TEXT_ANNOTATION, DrawingToolType.HORIZONTAL_LINE -> 1
                DrawingToolType.TRENDLINE, DrawingToolType.FIBONACCI, DrawingToolType.RECTANGLE, DrawingToolType.RAY -> 2
                DrawingToolType.CHANNEL, DrawingToolType.FIBONACCI_EXTENSION, DrawingToolType.PITCHFORK -> 3
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
                    chartState.pushDrawingState()
                    // Single-point tool (horizontal line) — complete immediately
                    val updatedDrawings = chartState.drawings + newDrawing
                    state.copy(chartState = chartState.copy(
                        drawings = updatedDrawings,
                        currentDrawing = null,
                        activeDrawingTool = DrawingToolType.NONE,
                        selectedDrawingId = newDrawing.id
                    ))
                } else {
                    state.copy(chartState = chartState.copy(currentDrawing = newDrawing))
                }
            } else {
                // Add point to existing drawing
                val updatedPoints = currentDrawing.points + point
                if (updatedPoints.size >= requiredPoints) {
                    chartState.pushDrawingState()
                    // Drawing complete
                    val completedDrawing = currentDrawing.copy(points = updatedPoints, isComplete = true)
                    val updatedDrawings = chartState.drawings + completedDrawing
                    state.copy(chartState = chartState.copy(
                        drawings = updatedDrawings,
                        currentDrawing = null,
                        activeDrawingTool = DrawingToolType.NONE,
                        selectedDrawingId = completedDrawing.id
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
            state.chartState.pushDrawingState()
            state.copy(chartState = state.chartState.copy(drawings = emptyList(), currentDrawing = null, selectedDrawingId = null))
        }
    }

    fun undoDrawing() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.undo())
        }
    }

    fun redoDrawing() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.redo())
        }
    }

    fun selectDrawing(drawingId: String?) {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(selectedDrawingId = drawingId))
        }
    }

    fun deleteDrawing(drawingId: String) {
        _uiState.update { state ->
            state.chartState.pushDrawingState()
            val updated = state.chartState.drawings.filter { it.id != drawingId }
            state.copy(chartState = state.chartState.copy(drawings = updated, selectedDrawingId = null))
        }
    }

    fun continueDrawing(drawing: DrawingData) {
        _uiState.update { state ->
            val remaining = state.chartState.drawings.filter { it.id != drawing.id }
            state.copy(
                chartState = state.chartState.copy(
                    drawings = remaining,
                    currentDrawing = drawing.copy(isComplete = false),
                    activeDrawingTool = drawing.toolType,
                    selectedDrawingId = null
                )
            )
        }
    }

    fun toggleVolume() {
        _uiState.update { state ->
            state.copy(chartState = state.chartState.copy(showVolume = !state.chartState.showVolume))
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
                IndicatorType.MACD -> IndicatorCalculator.calculateMACD(candles, config.period, config.secondaryPeriod, config.tertiaryPeriod)
                IndicatorType.BOLLINGER_BANDS -> IndicatorCalculator.calculateBollingerBands(candles, config.period, config.multiplier)
                IndicatorType.VWAP -> IndicatorCalculator.calculateVWAP(candles)
                IndicatorType.SUPERTREND -> IndicatorCalculator.calculateSupertrend(candles, config.period, config.multiplier)
                IndicatorType.ATR -> IndicatorCalculator.calculateATR(candles, config.period)
                IndicatorType.STOCHASTIC -> IndicatorCalculator.calculateStochastic(candles, config.period, config.secondaryPeriod)
                IndicatorType.ICHIMOKU -> IndicatorCalculator.calculateIchimoku(candles, config.period, config.secondaryPeriod, config.tertiaryPeriod)
                IndicatorType.CVD -> IndicatorCalculator.calculateCVD(candles)
                IndicatorType.PARABOLIC_SAR -> IndicatorCalculator.calculateParabolicSAR(candles)
                IndicatorType.ADX -> IndicatorCalculator.calculateADX(candles, config.period)
                IndicatorType.OBV -> IndicatorCalculator.calculateOBV(candles)
                IndicatorType.CCI -> IndicatorCalculator.calculateCCI(candles, config.period)
                IndicatorType.WILLIAMS_R -> IndicatorCalculator.calculateWilliamsR(candles, config.period)
                IndicatorType.MFI -> IndicatorCalculator.calculateMFI(candles, config.period)
            }
            if (result != null) results[config.type] = result
        }

        val smc = IndicatorCalculator.detectSMC(candles)
        val lastClose = candles.last().close
        val strikeStep = if (lastClose > 5000) 100.0 else 50.0
        val baseStrike = (kotlin.math.round(lastClose / strikeStep)) * strikeStep

        val fno = chartState.fnoLevels ?: FnoOverlayLevels(
            callWall = baseStrike + strikeStep * 3,
            putWall = baseStrike - strikeStep * 3,
            gammaFlip = baseStrike - strikeStep * 0.5,
            maxPain = baseStrike - strikeStep,
            callWallGex = 1.45e9,
            putWallGex = -1.18e9,
            totalNetGex = 2.7e8,
            enabled = false
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
        else -> 0xFFFFFFFF
    }

    // ── Data Fetching ──
    private fun fetchHistoricalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isError = false, errorMessage = null) }
            
            var hasError = false
            val fetchedCandles = try {
                val to = System.currentTimeMillis()
                val tf = _uiState.value.selectedTimeframe.trim()
                val from = when {
                    tf.equals("1D", ignoreCase = true) || tf.equals("1W", ignoreCase = true) || tf == "1M" -> {
                        to - (3L * 365 * 24 * 3600 * 1000L) // 3 FULL YEARS of historical candles!
                    }
                    tf.equals("4H", ignoreCase = true) || tf.equals("1H", ignoreCase = true) || tf.equals("60m", ignoreCase = true) -> {
                        to - (180L * 24 * 3600 * 1000L) // 180 days
                    }
                    tf.equals("15m", ignoreCase = true) || tf.equals("30m", ignoreCase = true) -> {
                        to - (60L * 24 * 3600 * 1000L) // 60 days
                    }
                    tf.equals("5m", ignoreCase = true) -> {
                        to - (60L * 24 * 3600 * 1000L) // 60 days
                    }
                    tf == "1m" -> {
                        to - (30L * 24 * 3600 * 1000L) // 30 days
                    }
                    else -> to - (3L * 365 * 24 * 3600 * 1000L) // Default 3 years!
                }

                getHistoricalCandlesUseCase.execute(
                    symbol = _uiState.value.symbol,
                    timeframe = _uiState.value.selectedTimeframe,
                    from = from,
                    to = to,
                    type = type
                )
            } catch (e: Exception) {
                hasError = true
                emptyList()
            }

            if (fetchedCandles.isEmpty()) {
                hasError = true
            }

            val historicalCandles = fetchedCandles

            val clean = _uiState.value.symbol.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD").trim()
            val isCryptoAsset = type.equals("CRYPTO", ignoreCase = true) || clean in listOf("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE", "SHIB", "BITCOIN", "ETHEREUM")
            var liveCryptoSpot: Double? = null
            var liveCryptoPct: Double? = null
            if (isCryptoAsset) {
                try {
                    val cryptos = marketRepository.getCryptoLivePrices()
                    val matched = cryptos.find { c ->
                        val cSym = c.symbol.trim().uppercase()
                        val cId = c.id.trim().lowercase()
                        cSym == clean || cSym == _uiState.value.symbol.uppercase() || cId == clean.lowercase() ||
                        "${cSym}USDT" == clean || "${cSym}USD" == clean || "${clean}USDT" == cSym ||
                        (clean.equals("BITCOIN", ignoreCase = true) && cSym == "BTC") ||
                        (clean.equals("ETHEREUM", ignoreCase = true) && cSym == "ETH")
                    }
                    if (matched != null && matched.price > 0.0) {
                        liveCryptoSpot = matched.price
                        liveCryptoPct = matched.changePercent
                    }
                } catch (e: Exception) { Log.e("AnalysisVM", "Error: ${e.message}") }
            }

            val lastClose = liveCryptoSpot ?: historicalCandles.lastOrNull()?.close ?: 0.0
            val prevClose = if (historicalCandles.size > 1) historicalCandles[historicalCandles.size - 2].close else historicalCandles.firstOrNull()?.open ?: lastClose
            val chg = if (liveCryptoSpot != null && liveCryptoPct != null) (liveCryptoSpot * liveCryptoPct) / 100.0 else (lastClose - prevClose)
            val chgPct = liveCryptoPct ?: (if (prevClose > 0) (chg / prevClose) * 100.0 else 0.0)

            _uiState.update { state ->
                val newChartState = recalculateIndicators(
                    state.chartState.copy(candles = historicalCandles),
                    historicalCandles
                )
                val activePrice = if (state.currentPrice > 0.0) state.currentPrice else lastClose
                val activeChange = if (state.currentPrice > 0.0) state.priceChange else chg
                val activeChangePct = if (state.currentPrice > 0.0) state.priceChangePercent else chgPct
                state.copy(
                    candles = historicalCandles,
                    currentPrice = activePrice,
                    priceChange = activeChange,
                    priceChangePercent = activeChangePct,
                    isLoading = false,
                    isError = hasError,
                    errorMessage = if (hasError) "Unable to load chart data. Please check your connection." else null,
                    chartState = newChartState
                )
            }
        }
    }

    private fun getTimeframeDurationMs(timeframe: String): Long {
        val tf = timeframe.trim()
        return when {
            tf == "1M" || tf.equals("1mo", ignoreCase = true) -> 30L * 24 * 3600 * 1000L
            tf.equals("1W", ignoreCase = true) || tf.equals("1w", ignoreCase = true) -> 7L * 24 * 3600 * 1000L
            tf.equals("1D", ignoreCase = true) || tf.equals("1d", ignoreCase = true) -> 24 * 3600 * 1000L
            tf.equals("4H", ignoreCase = true) || tf.equals("4h", ignoreCase = true) -> 4L * 3600 * 1000L
            tf.equals("1H", ignoreCase = true) || tf.equals("1h", ignoreCase = true) || tf.equals("60m", ignoreCase = true) -> 3600 * 1000L
            tf.equals("30m", ignoreCase = true) -> 30L * 60 * 1000L
            tf.equals("15m", ignoreCase = true) -> 15L * 60 * 1000L
            tf.equals("5m", ignoreCase = true) -> 5L * 60 * 1000L
            tf.equals("1m", ignoreCase = true) -> 60 * 1000L
            else -> 24 * 3600 * 1000L
        }
    }

    private fun listenForLivePrices() {
        val endpoint = com.marketintelligence.ai.BuildConfig.WS_BASE_URL.ifBlank { com.marketintelligence.ai.BuildConfig.BASE_URL }
        listenForLiveTicksUseCase.execute(endpoint)
            .catch { /* suppress connection retry glitches */ }
            .filter { tick ->
                val cur = _uiState.value.symbol.trim()
                val cleanCur = cur.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD")
                val tickSym = tick.symbol.trim().uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD")
                cur.equals(tick.symbol, ignoreCase = true) ||
                cleanCur.equals(tickSym, ignoreCase = true) ||
                tick.symbol.equals(cur.removeSuffix(".NS"), ignoreCase = true) ||
                "${tick.symbol}.NS".equals(cur, ignoreCase = true)
            }
            .onEach { tick ->
                listenForLiveTicksUseCase.processTick(tick)
                
                // Compute volume delta from cumulative volumeTradedToday
                val volKey = tick.symbol.uppercase()
                val prevCumVol = lastCumulativeVolume[volKey] ?: 0.0
                val tickVolumeDelta = if (tick.volume > prevCumVol && prevCumVol > 0.0) {
                    tick.volume - prevCumVol
                } else {
                    tick.volume.coerceAtLeast(0.0)
                }
                if (tick.volume > 0.0) {
                    lastCumulativeVolume[volKey] = tick.volume
                }

                _uiState.update { currentState ->
                    var updatedCandles = currentState.candles.toMutableList()
                    if (updatedCandles.isEmpty()) {
                        val tfDuration = getTimeframeDurationMs(currentState.selectedTimeframe)
                        val newOpenTime = (tick.timestamp / tfDuration) * tfDuration
                        val newCloseTime = newOpenTime + tfDuration
                        updatedCandles.add(Candle(
                            symbol = currentState.symbol,
                            timeframe = currentState.selectedTimeframe,
                            openTime = newOpenTime,
                            open = tick.price,
                            high = tick.price,
                            low = tick.price,
                            close = tick.price,
                            volume = tickVolumeDelta,
                            closeTime = newCloseTime,
                            isClosed = false
                        ))
                    }
                    if (updatedCandles.isNotEmpty()) {
                        val lastCandle = updatedCandles.last()
                        val tfDuration = getTimeframeDurationMs(currentState.selectedTimeframe)
                        val candleCloseTime = if (lastCandle.closeTime > lastCandle.openTime) {
                            lastCandle.closeTime
                        } else {
                            lastCandle.openTime + tfDuration
                        }

                        val shouldSpawnNew = lastCandle.isClosed || tick.timestamp >= candleCloseTime

                        if (shouldSpawnNew) {
                            // Seal previous candle if not already closed
                            if (!lastCandle.isClosed) {
                                val closedLastCandle = lastCandle.copy(isClosed = true, closeTime = candleCloseTime)
                                updatedCandles[updatedCandles.lastIndex] = closedLastCandle
                            }

                            // Spawn new candle for the active timeframe
                            // Floor openTime to timeframe boundary for proper alignment
                            val newOpenTime = (tick.timestamp / tfDuration) * tfDuration
                            val newCloseTime = newOpenTime + tfDuration
                            val newCandle = Candle(
                                symbol = currentState.symbol,
                                timeframe = currentState.selectedTimeframe,
                                openTime = newOpenTime,
                                open = tick.price,
                                high = tick.price,
                                low = tick.price,
                                close = tick.price,
                                volume = tickVolumeDelta,
                                closeTime = newCloseTime,
                                isClosed = false
                            )
                            updatedCandles.add(newCandle)

                            // Persist sealed candle and new candle to Room DB
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    candleRepository.insertCandles(listOf(newCandle))
                                } catch (e: Exception) { Log.e("AnalysisVM", "Error: ${e.message}") }
                            }
                        } else {
                            // Update existing candle in real time
                            val updatedLastCandle = lastCandle.copy(
                                high = maxOf(lastCandle.high, tick.price),
                                low = minOf(lastCandle.low, tick.price),
                                close = tick.price,
                                volume = lastCandle.volume + tickVolumeDelta,
                                closeTime = candleCloseTime
                            )
                            updatedCandles[updatedCandles.lastIndex] = updatedLastCandle

                            // Persist update to Room DB
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    candleRepository.updateCandle(
                                        openTime = updatedLastCandle.openTime,
                                        symbol = updatedLastCandle.symbol,
                                        timeframe = updatedLastCandle.timeframe,
                                        high = updatedLastCandle.high,
                                        low = updatedLastCandle.low,
                                        close = updatedLastCandle.close,
                                        volume = updatedLastCandle.volume
                                    )
                                } catch (e: Exception) { Log.e("AnalysisVM", "Error: ${e.message}") }
                            }
                        }
                        val newChartState = recalculateIndicators(
                            currentState.chartState.copy(candles = updatedCandles),
                            updatedCandles
                        )
                        val lastClose = tick.price
                        val prevClose = if (updatedCandles.size > 1) updatedCandles[updatedCandles.size - 2].close else updatedCandles.first().open
                        val chg = if (currentState.priceChangePercent != 0.0) (lastClose * currentState.priceChangePercent) / 100.0 else (lastClose - prevClose)
                        val chgPct = if (currentState.priceChangePercent != 0.0) currentState.priceChangePercent else (if (prevClose > 0) (chg / prevClose) * 100.0 else 0.0)
                        currentState.copy(
                            candles = updatedCandles,
                            currentPrice = lastClose,
                            priceChange = chg,
                            priceChangePercent = chgPct,
                            chartState = newChartState
                        )
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startPeriodicCryptoPolling() {
        val clean = symbol.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD").trim()
        val isCrypto = type.equals("CRYPTO", ignoreCase = true) || clean in listOf("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE", "SHIB", "BITCOIN", "ETHEREUM")
        if (isCrypto) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                while (isActive) {
                    try {
                        val cryptos = marketRepository.getCryptoLivePrices()
                        if (cryptos.isNotEmpty()) {
                            val matched = cryptos.find { c ->
                                val cSym = c.symbol.trim().uppercase()
                                val cId = c.id.trim().lowercase()
                                cSym == clean ||
                                cSym == symbol.uppercase() ||
                                cId == clean.lowercase() ||
                                "${cSym}USDT" == clean ||
                                "${cSym}USD" == clean ||
                                "${clean}USDT" == cSym ||
                                (clean.equals("BITCOIN", ignoreCase = true) && cSym == "BTC") ||
                                (clean.equals("ETHEREUM", ignoreCase = true) && cSym == "ETH")
                            }
                            if (matched != null && matched.price > 0.0) {
                                val livePrice = matched.price
                                val livePct = matched.changePercent
                                val liveChg = (livePrice * livePct) / 100.0

                                _uiState.update { state ->
                                    val curList = state.candles.toMutableList()
                                    if (curList.isNotEmpty()) {
                                        val lastIdx = curList.lastIndex
                                        val last = curList[lastIdx]
                                        val now = System.currentTimeMillis()
                                        val tfDuration = getTimeframeDurationMs(state.selectedTimeframe)
                                        val candleCloseTime = if (last.closeTime > last.openTime) last.closeTime else last.openTime + tfDuration

                                        if (last.isClosed || now >= candleCloseTime) {
                                            // Seal previous candle if not already closed
                                            if (!last.isClosed) {
                                                curList[lastIdx] = last.copy(isClosed = true, closeTime = candleCloseTime)
                                            }
                                            // Spawn a new live candle
                                            val newOpenTime = (now / tfDuration) * tfDuration
                                            val newCloseTime = newOpenTime + tfDuration
                                            val tickSize = 1.0
                                            curList.add(Candle(
                                                symbol = state.symbol,
                                                timeframe = state.selectedTimeframe,
                                                openTime = newOpenTime,
                                                open = livePrice,
                                                high = livePrice,
                                                low = livePrice,
                                                close = livePrice,
                                                volume = tickSize,
                                                closeTime = newCloseTime,
                                                isClosed = false
                                            ))
                                        } else {
                                            // Update existing live candle
                                            val tickSize = 1.0
                                            curList[lastIdx] = last.copy(
                                                close = livePrice,
                                                high = maxOf(last.high, livePrice),
                                                low = minOf(last.low, livePrice),
                                                volume = last.volume + tickSize
                                            )
                                        }
                                    }
                                    val newChartState = recalculateIndicators(state.chartState.copy(candles = curList), curList)
                                    state.copy(
                                        candles = curList,
                                        currentPrice = livePrice,
                                        priceChange = liveChg,
                                        priceChangePercent = livePct,
                                        chartState = newChartState
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) { Log.e("AnalysisVM", "Error: ${e.message}") }
                    kotlinx.coroutines.delay(2000L) // 2-second real-time spot price refresh
                }
            }
        }
    }

    /**
     * Polls /live-prices endpoint for indices and stocks (non-crypto) every 2 seconds
     * to keep the Analysis header price/change/changePercent always live and up-to-date.
     */
    private fun startPeriodicLivePricePolling() {
        val clean = symbol.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD").trim()
        val isCrypto = type.equals("CRYPTO", ignoreCase = true) || clean in listOf("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE", "SHIB", "BITCOIN", "ETHEREUM")
        if (isCrypto) return // crypto is already handled by startPeriodicCryptoPolling

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isActive) {
                try {
                    val prices = marketRepository.getLivePrices()
                    if (prices.isNotEmpty()) {
                        val currentSym = _uiState.value.symbol.trim()
                        val cleanSym = currentSym.uppercase().replace(" ", "").removeSuffix(".NS").removeSuffix(".BO").trim()
                        val matchedPrice = prices.find { p ->
                            val pClean = p.symbol.uppercase().replace(" ", "").removeSuffix(".NS").removeSuffix(".BO").trim()
                            pClean == cleanSym ||
                            p.symbol.equals(currentSym, ignoreCase = true) ||
                            p.symbol.equals(cleanSym, ignoreCase = true) ||
                            p.symbol.equals("$cleanSym.NS", ignoreCase = true) ||
                            currentSym.uppercase().equals("${p.symbol.removeSuffix(".NS")}", ignoreCase = true)
                        }
                        if (matchedPrice != null && matchedPrice.ltp > 0.0) {
                            val resolvedChangePercent = if (kotlin.math.abs(matchedPrice.changePercent) < 0.0001 && kotlin.math.abs(matchedPrice.change) > 0.0) {
                                 val prevClose = matchedPrice.ltp - matchedPrice.change
                                 if (prevClose > 0.0) (matchedPrice.change / prevClose) * 100.0 else matchedPrice.changePercent
                            } else {
                                matchedPrice.changePercent
                            }
                            val resolvedChange = if (kotlin.math.abs(matchedPrice.change) < 0.0001 && kotlin.math.abs(resolvedChangePercent) > 0.0) {
                                (matchedPrice.ltp * resolvedChangePercent) / 100.0
                            } else {
                                matchedPrice.change
                            }
                            _uiState.update { state ->
                                val curList = state.candles.toMutableList()
                                if (curList.isNotEmpty()) {
                                    val lastIdx = curList.lastIndex
                                    val last = curList[lastIdx]
                                    val now = System.currentTimeMillis()
                                    val tfDuration = getTimeframeDurationMs(state.selectedTimeframe)
                                    val candleCloseTime = if (last.closeTime > last.openTime) last.closeTime else last.openTime + tfDuration

                                    if (last.isClosed || now >= candleCloseTime) {
                                        // Seal previous candle if not already closed
                                        if (!last.isClosed) {
                                            curList[lastIdx] = last.copy(isClosed = true, closeTime = candleCloseTime)
                                        }
                                        // Spawn a new live candle
                                        val newOpenTime = (now / tfDuration) * tfDuration
                                        val newCloseTime = newOpenTime + tfDuration
                                        curList.add(Candle(
                                            symbol = state.symbol,
                                            timeframe = state.selectedTimeframe,
                                            openTime = newOpenTime,
                                            open = matchedPrice.ltp,
                                            high = matchedPrice.ltp,
                                            low = matchedPrice.ltp,
                                            close = matchedPrice.ltp,
                                            volume = 0.0,
                                            closeTime = newCloseTime,
                                            isClosed = false
                                        ))
                                    } else {
                                        // Update existing live candle
                                        curList[lastIdx] = last.copy(
                                            close = matchedPrice.ltp,
                                            high = maxOf(last.high, matchedPrice.ltp),
                                            low = minOf(last.low, matchedPrice.ltp)
                                        )
                                    }
                                }
                                val newChartState = recalculateIndicators(state.chartState.copy(candles = curList), curList)
                                state.copy(
                                    candles = curList,
                                    currentPrice = matchedPrice.ltp,
                                    priceChange = resolvedChange,
                                    priceChangePercent = resolvedChangePercent,
                                    chartState = newChartState
                                )
                            }
                        }
                    }
                } catch (e: Exception) { Log.e("AnalysisVM", "Error: ${e.message}") }
                kotlinx.coroutines.delay(1000L) // 1-second real-time polling
            }
        }
    }

}

