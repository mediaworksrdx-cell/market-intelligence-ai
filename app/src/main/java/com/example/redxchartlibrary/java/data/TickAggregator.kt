package com.example.redxchartlibrary.data

import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.model.Tick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TickAggregator(private val timeFrameMillis: Long) {

    private var currentCandle: Candle? = null
    private val _candleFlow = MutableStateFlow<Candle?>(null)
    val candleFlow: StateFlow<Candle?> = _candleFlow

    fun addTick(tick: Tick) {
        val tickTimeBucket = tick.timestamp - (tick.timestamp % timeFrameMillis)

        if (currentCandle == null || tickTimeBucket > currentCandle!!.timestamp) {
            // Close previous candle and start a new one
            currentCandle?.let {
                // Optional: emit final version of the previous candle
            }
            currentCandle = Candle(
                timestamp = tickTimeBucket,
                open = tick.price,
                high = tick.price,
                low = tick.price,
                close = tick.price,
                volume = 0
            )
        } else {
            // Update the current candle
            currentCandle = currentCandle!!.copy(
                high = maxOf(currentCandle!!.high, tick.price),
                low = minOf(currentCandle!!.low, tick.price),
                close = tick.price,
                // volume will be updated later
            )
        }

        _candleFlow.value = currentCandle
    }
}