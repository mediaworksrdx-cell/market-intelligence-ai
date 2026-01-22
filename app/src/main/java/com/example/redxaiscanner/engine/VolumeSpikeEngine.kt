package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

data class VolumeSpikeResult(
    val timestamp: Long,
    val volumeRatio: Double // How many times larger the volume is than the average
)

class VolumeSpikeEngine {
    companion object {
        const val VERSION = "1.0.0"
    }

    private val lookbackPeriod = 20
    private val spikeThreshold = 3.0 // Volume must be 3x the average to be a spike

    fun analyze(candles: List<Candle>): List<VolumeSpikeResult> {
        if (candles.size < lookbackPeriod) return emptyList()

        val results = mutableListOf<VolumeSpikeResult>()
        for (i in lookbackPeriod until candles.size) {
            val window = candles.subList(i - lookbackPeriod, i)
            val currentCandle = candles[i]

            val averageVolume = window.map { it.volume }.reduce { acc, v -> acc + v } / BigDecimal(window.size)
            if (averageVolume == BigDecimal.ZERO) continue

            val ratio = currentCandle.volume.toDouble() / averageVolume.toDouble()

            if (ratio >= spikeThreshold) {
                results.add(VolumeSpikeResult(currentCandle.timestamp, ratio))
            }
        }
        return results
    }
}