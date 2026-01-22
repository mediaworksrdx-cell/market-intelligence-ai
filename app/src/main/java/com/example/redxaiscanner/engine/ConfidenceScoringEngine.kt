package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal
import kotlin.math.roundToInt

class ConfidenceScoringEngine {

    // Define the weights for each component of the score
    private val weights = mapOf(
        "structural_alignment" to 0.40, // 40%
        "fvg_quality" to 0.25,          // 25%
        "pattern_strength" to 0.15,     // 15%
        "volume_confirmation" to 0.20   // 20%
    )

    /**
     * Scores a validated ConfluenceSignal based on multiple quantitative factors.
     *
     * @param signal The signal to score.
     * @param candles The list of candles for the timeframe, needed for volume/volatility analysis.
     * @return A ScoredSignal containing the normalized score and a breakdown.
     */
    fun score(signal: ConfluenceSignal, candles: List<Candle>): ScoredSignal {
        val breakdown = mutableMapOf<String, Int>()

        // 1. Structural Alignment (already validated, so it gets a high base score)
        breakdown["structural_alignment"] = 100

        // 2. FVG Quality Score
        val fvgScore = signal.fvgSignal?.strength?.let { (it * 100).toInt() } ?: 0
        breakdown["fvg_quality"] = fvgScore

        // 3. Pattern Strength Score
        val patternScore = signal.patternSignal?.strengthScore?.let { (it * 100).toInt() } ?: 0
        breakdown["pattern_strength"] = patternScore

        // 4. Volume Confirmation Score
        val volumeScore = calculateVolumeScore(signal.smcSignal, candles)
        breakdown["volume_confirmation"] = volumeScore

        // Calculate the final weighted score
        val finalScore = (breakdown["structural_alignment"]!! * weights["structural_alignment"]!!) +
                         (breakdown["fvg_quality"]!! * weights["fvg_quality"]!!) +
                         (breakdown["pattern_strength"]!! * weights["pattern_strength"]!!) +
                         (breakdown["volume_confirmation"]!! * weights["volume_confirmation"]!!)

        return ScoredSignal(
            confidenceScore = finalScore.roundToInt().coerceIn(0, 100),
            scoreBreakdown = breakdown,
            underlyingSignal = signal
        )
    }

    /**
     * Calculates a score based on the volume profile around an SMC event (e.g., Order Block).
     * A strong event should be accompanied by significant volume.
     */
    private fun calculateVolumeScore(smcSignal: Any, candles: List<Candle>): Int {
        val signalTimestamp = when (smcSignal) {
            is OrderBlock -> smcSignal.timestamp
            is MarketStructureEvent -> smcSignal.timestamp
            else -> return 0
        }

        val signalCandle = candles.find { it.timestamp == signalTimestamp } ?: return 0
        
        // Calculate the average volume over a recent lookback period
        val lookback = 20
        val startIndex = (candles.indexOf(signalCandle) - lookback).coerceAtLeast(0)
        val relevantCandles = candles.subList(startIndex, candles.indexOf(signalCandle))
        if(relevantCandles.isEmpty()) return 50 // Not enough data, return neutral

        val averageVolume = relevantCandles.map { it.volume }.reduce { acc, v -> acc + v } / BigDecimal(relevantCandles.size)
        if (averageVolume == BigDecimal.ZERO) return 50

        // Score based on how much the signal candle's volume exceeds the recent average
        val volumeRatio = (signalCandle.volume.toDouble() / averageVolume.toDouble()).coerceIn(0.0, 3.0) // Cap at 3x for normalization
        
        return (volumeRatio / 3.0 * 100).roundToInt()
    }
}