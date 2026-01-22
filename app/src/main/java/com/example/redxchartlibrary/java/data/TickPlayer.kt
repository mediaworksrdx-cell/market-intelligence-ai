package com.example.redxchartlibrary.data

import com.example.redxchartlibrary.model.Tick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class TickPlayer {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var replayJob: Job? = null

    private val _tickFlow = MutableSharedFlow<Tick>()
    val tickFlow: SharedFlow<Tick> = _tickFlow

    var isPlaying = false
        private set

    fun startReplay(speedMultiplier: Int = 1) {
        if (isPlaying) return
        isPlaying = true
        
        replayJob = coroutineScope.launch {
            // Simulate fetching historical ticks
            val historicalTicks = generateHistoricalTicks()
            
            historicalTicks.forEach { tick ->
                _tickFlow.emit(tick)
                delay((1000 / speedMultiplier).toLong()) // Control replay speed
            }
            isPlaying = false
        }
    }

    fun stopReplay() {
        replayJob?.cancel()
        isPlaying = false
    }

    private fun generateHistoricalTicks(): List<Tick> {
        // In a real app, this would fetch data from a repository or network source.
        val ticks = mutableListOf<Tick>()
        var price = 150f
        val startTime = System.currentTimeMillis() - 1000 * 60 * 60 // 1 hour ago
        for (i in 0 until 5000) {
            price += (Math.random() * 0.5 - 0.25).toFloat()
            ticks.add(Tick(startTime + i * 100, price))
        }
        return ticks
    }
}