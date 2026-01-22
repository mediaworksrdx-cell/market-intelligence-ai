package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle

/**
 * Holds the result of a single backtested trade.
 *
 * @param outcome The result of the trade (e.g., Hit_TP2, Hit_SL).
 * @param entryTimestamp The timestamp when the trade would have been entered.
 * @param exitTimestamp The timestamp when the trade would have been closed.
 * @param tradeSetup The original TradeSetup that was tested.
 */
data class BacktestResult(
    val outcome: TradeOutcome,
    val entryTimestamp: Long,
    val exitTimestamp: Long,
    val tradeSetup: TradeSetup
)

enum class TradeOutcome {
    HIT_TP2, HIT_TP1, HIT_SL, NOT_TRIGGERED
}

/**
 * A deterministic engine to evaluate a trade setup against historical price data.
 */
class BacktestingEngine {
    fun run(setup: TradeSetup, futureCandles: List<Candle>): BacktestResult {
        var entryTimestamp: Long? = null

        for (candle in futureCandles) {
            val isBullish = setup.underlyingSignal.underlyingSignal.higherTimeframeBias == MarketBias.BULLISH

            // Check for entry trigger
            if (entryTimestamp == null) {
                if ((isBullish && candle.low <= setup.entryPrice) || (!isBullish && candle.high >= setup.entryPrice)) {
                    entryTimestamp = candle.timestamp
                } else {
                    continue // Not in a trade yet
                }
            }

            // Check for exit conditions
            if ((isBullish && candle.high >= setup.takeProfit2) || (!isBullish && candle.low <= setup.takeProfit2)) {
                return BacktestResult(TradeOutcome.HIT_TP2, entryTimestamp, candle.timestamp, setup)
            }
            if ((isBullish && candle.high >= setup.takeProfit1) || (!isBullish && candle.low <= setup.takeProfit1)) {
                // For simplicity, we assume we exit fully at TP2. A real engine might scale out.
            }
            if ((isBullish && candle.low <= setup.stopLossPrice) || (!isBullish && candle.high >= setup.stopLossPrice)) {
                return BacktestResult(TradeOutcome.HIT_SL, entryTimestamp, candle.timestamp, setup)
            }
        }
        
        return BacktestResult(TradeOutcome.NOT_TRIGGERED, 0L, 0L, setup)
    }
}