package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal
import kotlin.math.roundToInt

class ConfidenceScoringEngine {

    // Rule Set 11: Weights
    private val weights = mapOf(
        "confluence_layers" to 0.35,
        "liquidity_proximity" to 0.20,
        "structural_quality" to 0.25,
        "risk_clarity" to 0.20
    )

    /**
     * Scores a validated ConfluenceSignal based on Rule Set 11.
     */
    fun score(signal: ConfluenceSignal, candles: List<Candle>, regime: MarketRegime, liquidityZones: List<LiquidityZone>): ScoredSignal {
        val breakdown = mutableMapOf<String, Int>()

        // 1. Confluence Layers Score (Rule 10: Minimum 4 layers)
        var layers = 0
        if (regime != MarketRegime.UNDEFINED) layers++
        if (signal.smcSignal is OrderBlock) layers++
        if (signal.fvgSignal != null) layers++
        if (signal.patternSignal != null) layers++
        
        breakdown["confluence_layers"] = (layers / 4.0 * 100).coerceAtMost(100.0).toInt()

        // 2. Liquidity Proximity (Rule 11)
        val currentPrice = candles.last().close
        val nearestLiquidity = liquidityZones.minByOrNull { (it.priceLevel - currentPrice).abs() }
        val distToLiq = nearestLiquidity?.let { (it.priceLevel - currentPrice).abs().toDouble() } ?: Double.MAX_VALUE
        val proximityScore = if (distToLiq < currentPrice.toDouble() * 0.01) 90 else 50
        breakdown["liquidity_proximity"] = proximityScore

        // 3. Structural Quality (Volume/Impulse from Order Block)
        breakdown["structural_quality"] = calculateVolumeScore(signal.smcSignal, candles)

        // 4. Risk Clarity (Rule 12: Invalidation point defined)
        val riskScore = if (signal.smcSignal is OrderBlock) 95 else 60
        breakdown["risk_clarity"] = riskScore

        // Penalties (Rule 11)
        var finalScore = 0.0
        weights.forEach { (k, w) -> finalScore += (breakdown[k] ?: 0) * (w ?: 0.0) }

        // Volatility Uncertainty Penalty
        if (regime == MarketRegime.HIGH_VOLATILITY || regime == MarketRegime.UNDEFINED) {
            finalScore *= 0.8
        }
        
        // RSI Check (Rule 8)
        val rsi = calculateRSI(candles)
        val isBullishSignal = signal.smcSignal is OrderBlock && signal.smcSignal.direction == FVGDireciton.BULLISH
        if (isBullishSignal && rsi < 40) finalScore *= 0.9
        if (!isBullishSignal && rsi > 60) finalScore *= 0.9

        return ScoredSignal(
            confidenceScore = finalScore.roundToInt().coerceIn(0, 100),
            scoreBreakdown = breakdown,
            underlyingSignal = signal
        )
    }

    private fun calculateVolumeScore(smcSignal: Any, candles: List<Candle>): Int {
        val signalTimestamp = when (smcSignal) {
            is OrderBlock -> smcSignal.timestamp
            is MarketStructureEvent -> smcSignal.timestamp
            else -> return 50
        }

        val signalCandle = candles.find { it.timestamp == signalTimestamp } ?: return 50
        
        val lookback = 20
        val startIndex = (candles.indexOf(signalCandle) - lookback).coerceAtLeast(0)
        val relevantCandles = candles.subList(startIndex, candles.indexOf(signalCandle))
        if(relevantCandles.isEmpty()) return 50

        val averageVolume = relevantCandles.map { it.volume }.reduce { acc, v -> acc + v } / BigDecimal(relevantCandles.size)
        if (averageVolume == BigDecimal.ZERO) return 50

        val volumeRatio = (signalCandle.volume.toDouble() / averageVolume.toDouble()).coerceIn(0.0, 3.0)
        return (volumeRatio / 3.0 * 100).roundToInt()
    }
    
    private fun calculateRSI(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size <= period) return 50.0
        
        var avgGain = 0.0
        var avgLoss = 0.0
        
        for (i in 1..period) {
            val change = candles[i].close.toDouble() - candles[i-1].close.toDouble()
            if (change > 0) avgGain += change else avgLoss += kotlin.math.abs(change)
        }
        avgGain /= period
        avgLoss /= period
        
        if (avgLoss == 0.0) return 100.0
        
        var rs = avgGain / avgLoss
        var rsi = 100.0 - (100.0 / (1.0 + rs))
        
        for (i in period + 1 until candles.size) {
            val change = candles[i].close.toDouble() - candles[i-1].close.toDouble()
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) kotlin.math.abs(change) else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            
            if (avgLoss == 0.0) {
                rsi = 100.0
            } else {
                rs = avgGain / avgLoss
                rsi = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return rsi
    }
}
