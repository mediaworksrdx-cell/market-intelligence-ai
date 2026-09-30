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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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
    val engineRouter: EngineRouter,
    private val candleRepository: com.marketintelligence.ai.data.source.CandleRepository,
    private val marketRepository: com.marketintelligence.ai.domain.repository.MarketRepository
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
        startPeriodicCryptoPolling()
        startPeriodicLivePricePolling()
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
            _uiState.update { it.copy(isLoading = true, error = null, isError = false, errorMessage = null) }
            
            var hasError = false
            try {
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

                val fetchedCandles = try {
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
                    } catch (_: Exception) {}
                }

                val lastClose = liveCryptoSpot ?: historicalCandles.lastOrNull()?.close ?: 0.0
                val prevClose = if (historicalCandles.size > 1) historicalCandles[historicalCandles.size - 2].close else historicalCandles.firstOrNull()?.open ?: lastClose
                val chg = if (liveCryptoSpot != null && liveCryptoPct != null) (liveCryptoSpot * liveCryptoPct) / 100.0 else (lastClose - prevClose)
                val chgPct = liveCryptoPct ?: (if (prevClose > 0) (chg / prevClose) * 100.0 else 0.0)

                _uiState.update {
                    val activePrice = if (it.currentPrice > 0.0) it.currentPrice else lastClose
                    val activeChange = if (it.currentPrice > 0.0) it.priceChange else chg
                    val activeChangePct = if (it.currentPrice > 0.0) it.priceChangePercent else chgPct
                    it.copy(
                        candles = historicalCandles,
                        currentPrice = activePrice,
                        priceChange = activeChange,
                        priceChangePercent = activeChangePct,
                        isLoading = false,
                        isError = hasError,
                        errorMessage = if (hasError) "Unable to load chart data. Please check your connection." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    candles = emptyList(), 
                    isLoading = false,
                    isError = true,
                    errorMessage = "Unable to load chart data. Please check your connection."
                ) }
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
        val endpoint = BuildConfig.WS_BASE_URL.ifBlank { BuildConfig.BASE_URL }
        listenForLiveTicksUseCase.execute(endpoint)
            .conflate()
            .catch { e ->
                Log.e("AnalysisViewModel", "Error in live tick stream: ${e.message}", e)
                _uiState.update { it.copy(error = e.message ?: "Live tick stream error") }
            }
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

                            // Persist to local Room database
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    candleRepository.insertCandles(listOf(newCandle))
                                } catch (_: Exception) {}
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
                            viewModelScope.launch(Dispatchers.IO) {
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
                                } catch (_: Exception) {}
                            }
                        }
                        val lastClose = tick.price
                        val prevClose = if (updatedCandles.size > 1) updatedCandles[updatedCandles.size - 2].close else updatedCandles.first().open
                        val chg = if (currentState.priceChangePercent != 0.0) (lastClose * currentState.priceChangePercent) / 100.0 else (lastClose - prevClose)
                        val chgPct = if (currentState.priceChangePercent != 0.0) currentState.priceChangePercent else (if (prevClose > 0) (chg / prevClose) * 100.0 else 0.0)
                        currentState.copy(
                            candles = updatedCandles,
                            currentPrice = lastClose,
                            priceChange = chg,
                            priceChangePercent = chgPct
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
                while (true) {
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
                                    state.copy(
                                        candles = curList,
                                        currentPrice = livePrice,
                                        priceChange = liveChg,
                                        priceChangePercent = livePct
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
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
            while (true) {
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
                                state.copy(
                                    candles = curList,
                                    currentPrice = matchedPrice.ltp,
                                    priceChange = resolvedChange,
                                    priceChangePercent = resolvedChangePercent
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(1000L) // 1-second real-time polling
            }
        }
    }
}
