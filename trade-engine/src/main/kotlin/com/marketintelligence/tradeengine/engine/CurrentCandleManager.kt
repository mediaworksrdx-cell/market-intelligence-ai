package com.marketintelligence.tradeengine.engine

import com.marketintelligence.tradeengine.models.Candle
import com.marketintelligence.tradeengine.models.Tick
import com.marketintelligence.tradeengine.repository.CandleDataSource
import com.marketintelligence.tradeengine.util.TimeFrame
import com.marketintelligence.tradeengine.util.toTimeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class CurrentCandleManager(
    private var symbol: String = "",
    private var timeframe: String = "1m",
    private val candleDataSource: CandleDataSource,
    private val coroutineScope: CoroutineScope
) {

    private val _currentCandle = MutableStateFlow<Candle?>(null)
    val currentCandle = _currentCandle.asStateFlow()

    private val candlesMap = java.util.concurrent.ConcurrentHashMap<String, Candle>()

    init {
        if (symbol.isNotBlank()) {
            coroutineScope.launch {
                _currentCandle.value = candleDataSource.getLatestCandle(symbol, timeframe)
            }
        }
    }

    fun setTrackedInstrument(symbol: String, timeframe: String) {
        this.symbol = symbol
        this.timeframe = timeframe
        coroutineScope.launch {
            _currentCandle.value = candleDataSource.getLatestCandle(symbol, timeframe)
        }
    }

    fun processTick(tick: Tick) {
        processTick(tick, this.timeframe)
    }

    fun processTick(tick: Tick, overrideTimeframe: String) {
        coroutineScope.launch {
            val actualSymbol = tick.symbol.ifBlank { symbol }
            val actualTimeframe = overrideTimeframe.ifBlank { "1m" }
            val timeFrameMillis = actualTimeframe.toTimeFrame().toMillis()
            val key = "${actualSymbol.uppercase()}_${actualTimeframe.uppercase()}"

            var candle = candlesMap[key] ?: _currentCandle.value?.takeIf { it.symbol.equals(actualSymbol, ignoreCase = true) }
            if (candle == null) {
                candle = candleDataSource.getLatestCandle(actualSymbol, actualTimeframe)
            }

            if (candle == null || candle.isClosed) {
                // Create a new candle
                val newCandle = createNewCandle(tick, actualSymbol, actualTimeframe, timeFrameMillis)
                candleDataSource.insertCandles(listOf(newCandle))
                candlesMap[key] = newCandle
                _currentCandle.value = newCandle
            } else {
                // Update the current candle
                if (tick.timestamp >= candle.closeTime) {
                    // Time to close the current candle and create a new one
                    candleDataSource.closeCandle(candle.openTime, actualSymbol, actualTimeframe)
                    val newCandle = createNewCandle(tick, actualSymbol, actualTimeframe, timeFrameMillis)
                    candleDataSource.insertCandles(listOf(newCandle))
                    candlesMap[key] = newCandle
                    _currentCandle.value = newCandle
                } else {
                    // Update the existing candle
                    val updatedCandle = candle.copy(
                        high = maxOf(candle.high, tick.price),
                        low = minOf(candle.low, tick.price),
                        close = tick.price,
                        volume = candle.volume + tick.volume
                    )
                    candleDataSource.updateCandle(
                        updatedCandle.openTime,
                        updatedCandle.symbol,
                        updatedCandle.timeframe,
                        updatedCandle.high,
                        updatedCandle.low,
                        updatedCandle.close,
                        updatedCandle.volume
                    )
                    candlesMap[key] = updatedCandle
                    _currentCandle.value = updatedCandle
                }
            }
        }
    }

    private fun createNewCandle(tick: Tick, sym: String, tf: String, tfMillis: Long): Candle {
        val openTime = (tick.timestamp / tfMillis) * tfMillis
        return Candle(
            symbol = sym,
            timeframe = tf,
            openTime = openTime,
            open = tick.price,
            high = tick.price,
            low = tick.price,
            close = tick.price,
            volume = tick.volume,
            closeTime = openTime + tfMillis,
            isClosed = false
        )
    }
}
