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
    private val symbol: String,
    private val timeframe: String,
    private val candleDataSource: CandleDataSource,
    private val coroutineScope: CoroutineScope
) {

    private val _currentCandle = MutableStateFlow<Candle?>(null)
    val currentCandle = _currentCandle.asStateFlow()

    private val timeFrameMillis = timeframe.toTimeFrame().toMillis()

    init {
        coroutineScope.launch {
            _currentCandle.value = candleDataSource.getLatestCandle(symbol, timeframe)
        }
    }

    private val tickMutex = kotlinx.coroutines.sync.Mutex()

    fun processTick(tick: Tick) {
        coroutineScope.launch {
            tickMutex.withLock {
                val candle = _currentCandle.value

                if (candle == null || candle.isClosed) {
                    // Create a new candle
                    val newCandle = createNewCandle(tick)
                    candleDataSource.insertCandles(listOf(newCandle))
                    _currentCandle.value = newCandle
                } else {
                    // Update the current candle
                    if (tick.timestamp >= candle.closeTime) {
                        // Time to close the current candle and create a new one
                        candleDataSource.closeCandle(candle.openTime, symbol, timeframe)
                        val newCandle = createNewCandle(tick)
                        candleDataSource.insertCandles(listOf(newCandle))
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
                        _currentCandle.value = updatedCandle
                    }
                }
            }
        }
    }

    private fun createNewCandle(tick: Tick): Candle {
        val openTime = (tick.timestamp / timeFrameMillis) * timeFrameMillis
        return Candle(
            symbol = symbol,
            timeframe = timeframe,
            openTime = openTime,
            open = tick.price,
            high = tick.price,
            low = tick.price,
            close = tick.price,
            volume = tick.volume,
            closeTime = openTime + timeFrameMillis,
            isClosed = false
        )
    }
}
