package com.example.redxchartlibrary.data

import com.example.redxchartlibrary.data.remote.ApiService
import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.model.Tick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MarketDataClient(private val apiService: ApiService) {

    private val tickAggregator = TickAggregator(60000) // 1-minute timeframe
    private val tickPlayer = TickPlayer()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var dataCollectionJob: Job? = null

    private val _marketDataFlow = MutableSharedFlow<Candle>()
    val marketDataFlow: Flow<Candle> = _marketDataFlow

    private var webSocket: WebSocket? = null

    init {
        // Collect aggregated candles and push them to the main flow
        coroutineScope.launch {
            tickAggregator.candleFlow.collect { candle ->
                candle?.let { _marketDataFlow.emit(it) }
            }
        }
    }

    fun startLiveStream() {
        stopAllStreams()
        // In a real app, you would parse live ticks here.
        // For now, we simulate live ticks.
        dataCollectionJob = coroutineScope.launch {
            var lastPrice = 200f
            while (true) {
                lastPrice += (Math.random() * 2 - 1).toFloat()
                tickAggregator.addTick(Tick(System.currentTimeMillis(), lastPrice))
                kotlinx.coroutines.delay(1500) // Slower ticks for live data
            }
        }
    }
    
    fun startHistoricalReplay(speedMultiplier: Int) {
        stopAllStreams()
        tickPlayer.startReplay(speedMultiplier)
        dataCollectionJob = coroutineScope.launch {
            tickPlayer.tickFlow.collect { tick ->
                tickAggregator.addTick(tick)
            }
        }
    }

    private fun stopAllStreams() {
        dataCollectionJob?.cancel()
        tickPlayer.stopReplay()
        webSocket?.close(1000, "User switched stream")
    }

    suspend fun getInitialCandles(symbol: String, from: Long, to: Long): List<Candle> {
        // This would fetch historical candles for the initial chart view
        return emptyList() // For now, we start with a clean slate
    }
}