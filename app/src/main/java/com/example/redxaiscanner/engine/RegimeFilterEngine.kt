package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

/**
 * Defines the overall market condition or "regime".
 */
enum class MarketRegime {
    BULL_TREND,
    BEAR_TREND,
    RANGING,
    HIGH_VOLATILITY,
    LOW_VOLATILITY
}

/**
 * Analyzes the macro market environment to determine the current regime.
 * This provides high-level context to filter out signals that are not
 * suitable for the current market conditions.
 */
class RegimeFilterEngine {
    companion object {
        const val VERSION = "1.0.0"
    }

    /**
     * @param candles A longer-term list of candles (e.g., 200 periods of 4H data).
     * @return A set of active market regimes.
     */
    fun getRegime(candles: List<Candle>): Set<MarketRegime> {
        if (candles.size < 50) return setOf(MarketRegime.RANGING) // Not enough data

        val regimes = mutableSetOf<MarketRegime>()

        // Simplified Trend Analysis (e.g., using a 50-period moving average)
        val movingAverage = candles.takeLast(50).map { it.close }.reduce { acc, p -> acc + p } / BigDecimal(50)
        val currentPrice = candles.last().close

        if (currentPrice > movingAverage * BigDecimal("1.05")) {
            regimes.add(MarketRegime.BULL_TREND)
        } else if (currentPrice < movingAverage * BigDecimal("0.95")) {
            regimes.add(MarketRegime.BEAR_TREND)
        } else {
            regimes.add(MarketRegime.RANGING)
        }
        
        // TODO: Add volatility analysis (e.g., using ATR or Bollinger Bands width)

        return regimes
    }
}