package com.example.redxchartlibrary.data

import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.model.Tick
import com.example.redxchartlibrary.util.TimeFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.ConcurrentHashMap

class TickAggregator {

    private val formingCandles = ConcurrentHashMap<String, Candle>()

    private val _formingCandleFlow = MutableSharedFlow<Candle>(replay = 10)
    val formingCandleFlow: SharedFlow<Candle> = _formingCandleFlow

    private val _completedCandleFlow = MutableSharedFlow<Candle>(replay = 10)
    val completedCandleFlow: SharedFlow<Candle> = _completedCandleFlow

    fun addTick(tick: Tick, timeframe: TimeFrame) {
        val key = "${tick.symbol}-${timeframe.identifier}"
        val timeFrameMillis = timeframe.duration
        val tickTimeBucket = tick.timestamp - (tick.timestamp % timeFrameMillis)

        val currentCandle = formingCandles[key]

        if (currentCandle == null || tickTimeBucket > currentCandle.openTime) {
            // Finalize the old candle
            currentCandle?.let { 
                _completedCandleFlow.tryEmit(it)
            }
            
            // Start a new candle
            val newCandle = Candle(
                symbol = tick.symbol,
                timeframe = timeframe.identifier,
                openTime = tickTimeBucket,
                closeTime = tickTimeBucket + timeFrameMillis,
                open = tick.price,
                high = tick.price,
                low = tick.price,
                close = tick.price,
                volume = tick.volume
            )
            formingCandles[key] = newCandle
            _formingCandleFlow.tryEmit(newCandle)

        } else {
            // Update the existing forming candle
            val updatedCandle = currentCandle.copy(
                high = maxOf(currentCandle.high, tick.price),
                low = minOf(currentCandle.low, tick.price),
                close = tick.price,
                volume = currentCandle.volume + tick.volume
            )
            formingCandles[key] = updatedCandle
            _formingCandleFlow.tryEmit(updatedCandle)
        }
    }
}
