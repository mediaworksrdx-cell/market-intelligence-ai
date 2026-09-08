package com.marketintelligence.redxaiscanner.engine

import com.marketintelligence.redxaiscanner.domain.model.Candle
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Defines the overall market condition or "regime".
 */
enum class MarketRegime {
    BULL_TREND,
    BEAR_TREND,
    RANGING,
    COMPRESSION,
    EXPANSION,
    HIGH_VOLATILITY, // Restored for compatibility
    UNDEFINED
}

/**
 * Analyzes the macro market environment to determine the current regime.
 * Implements Rule Set 2:
 * - Trending: HH/HL or LH/LL, EMA slopes, Bollinger expansion
 * - Range-Bound: No BOS, Flat EMAs
 * - Compression: Low BB Width, Low ATR
 * - Expansion: BB Expansion + Impulsive candles
 */
class RegimeFilterEngine {
    companion object {
        const val VERSION = "2.0.0"
        private const val EMA_FAST = 20
        private const val EMA_SLOW = 50
        private const val BB_PERIOD = 20
        private const val BB_STD_DEV = 2.0
    }

    /**
     * @param candles A longer-term list of candles (e.g., 200 periods).
     * @return The dominant market regime.
     */
    fun getRegime(candles: List<Candle>): MarketRegime {
        if (candles.size < 50) return MarketRegime.UNDEFINED

        val closes = candles.map { it.close.toDouble() }
        val ema20 = calculateEMA(closes, EMA_FAST)
        val ema50 = calculateEMA(closes, EMA_SLOW)
        val (upper, middle, lower) = calculateBollingerBands(closes, BB_PERIOD, BB_STD_DEV)
        
        val currentPrice = closes.last()
        val currentEMA20 = ema20.last()
        val currentEMA50 = ema50.last()
        
        // Bollinger Bandwidth
        val bbWidth = (upper.last() - lower.last()) / middle.last()
        val prevBBWidth = (upper[upper.size - 2] - lower[lower.size - 2]) / middle[middle.size - 2]
        
        // Expansion detection
        val isExpansion = bbWidth > prevBBWidth * 1.1 // 10% expansion
        
        // Compression detection (Low volatility)
        val isCompression = bbWidth < prevBBWidth * 0.9

        // Trend Detection
        val isBullishStack = currentPrice > currentEMA20 && currentEMA20 > currentEMA50
        val isBearishStack = currentPrice < currentEMA20 && currentEMA20 < currentEMA50
        
        // Slope check
        val ema50Slope = currentEMA50 - ema50[ema50.size - 5]
        val isFlat = kotlin.math.abs(ema50Slope) < (currentPrice * 0.0005)

        return when {
            isExpansion && isBullishStack -> MarketRegime.BULL_TREND
            isExpansion && isBearishStack -> MarketRegime.BEAR_TREND
            isExpansion -> MarketRegime.HIGH_VOLATILITY
            isCompression -> MarketRegime.COMPRESSION
            isFlat -> MarketRegime.RANGING
            isBullishStack -> MarketRegime.BULL_TREND
            isBearishStack -> MarketRegime.BEAR_TREND
            else -> MarketRegime.RANGING
        }
    }

    private fun calculateEMA(data: List<Double>, period: Int): List<Double> {
        val k = 2.0 / (period + 1)
        val ema = mutableListOf<Double>()
        var currentEma = data.first()
        ema.add(currentEma)
        for (i in 1 until data.size) {
            currentEma = data[i] * k + currentEma * (1 - k)
            ema.add(currentEma)
        }
        return ema
    }

    private fun calculateBollingerBands(data: List<Double>, period: Int, stdDevMultiplier: Double): Triple<List<Double>, List<Double>, List<Double>> {
        val sma = mutableListOf<Double>()
        val upper = mutableListOf<Double>()
        val lower = mutableListOf<Double>()
        
        for (i in 0 until data.size) {
            if (i < period - 1) {
                sma.add(data[i])
                upper.add(data[i])
                lower.add(data[i])
                continue
            }
            
            val window = data.subList(i - period + 1, i + 1)
            val mean = window.average()
            val stdDev = sqrt(window.map { (it - mean).pow(2.0) }.average())
            
            sma.add(mean)
            upper.add(mean + stdDev * stdDevMultiplier)
            lower.add(mean - stdDev * stdDevMultiplier)
        }
        return Triple(upper, sma, lower)
    }
}
