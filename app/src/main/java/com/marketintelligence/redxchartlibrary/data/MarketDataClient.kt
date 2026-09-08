package com.marketintelligence.redxchartlibrary.data

import com.marketintelligence.ai.data.source.remote.MarketApiService
import com.marketintelligence.ai.data.source.remote.MarketDataSocket
import com.marketintelligence.redxchartlibrary.data.local.CandleRepository
import com.marketintelligence.redxchartlibrary.model.Tick
import com.marketintelligence.redxchartlibrary.util.TimeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDataClient @Inject constructor(
    private val apiService: MarketApiService,
    private val marketDataSocket: MarketDataSocket,
    private val tickAggregator: TickAggregator,
    private val candleRepository: CandleRepository
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var liveStreamJob: Job? = null

    val formingCandleFlow = tickAggregator.formingCandleFlow

    init {
        // Persist completed candles
        coroutineScope.launch {
            tickAggregator.completedCandleFlow.collect { candle ->
                candleRepository.saveCandles(listOf(candle))
            }
        }
    }

    suspend fun fetchHistoricalCandles(symbol: String, timeframe: TimeFrame) {
        val to = System.currentTimeMillis()
        val from = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.timeInMillis
        try {
            val candles = apiService.getCandles(symbol, timeframe.identifier, from, to, "stock")
            candleRepository.saveCandles(candles.map {
                com.marketintelligence.redxchartlibrary.model.Candle(
                    symbol = symbol,
                    timeframe = timeframe.identifier,
                    openTime = it.openTime,
                    open = it.open,
                    high = it.high,
                    low = it.low,
                    close = it.close,
                    volume = it.volume,
                    closeTime = it.closeTime,
                    isClosed = it.isClosed
                )
            })
        } catch (e: Exception) {
            // Log error
        }
    }

    fun startLiveStream(symbol: String, timeframe: TimeFrame) {
        if (liveStreamJob?.isActive == true) return

        liveStreamJob = coroutineScope.launch {
            marketDataSocket.connect(symbol)
                .catch { e ->
                    // Log error
                }
                .collect { livePrice ->
                    if (livePrice.symbol.equals(symbol, ignoreCase = true)) {
                        val tick = Tick(
                            symbol = livePrice.symbol,
                            timestamp = System.currentTimeMillis(),
                            price = livePrice.ltp,
                            volume = 0.0
                        )
                        tickAggregator.addTick(tick, timeframe)
                    }
                }
        }
    }

    fun stopLiveStream() {
        liveStreamJob?.cancel()
        liveStreamJob = null
    }

    fun getHistoricalCandles(symbol: String, timeframe: String) = candleRepository.getCandles(symbol, timeframe)
}
