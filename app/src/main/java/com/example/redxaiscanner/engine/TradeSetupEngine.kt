package com.example.redxaiscanner.engine

import com.example.redxaiscanner.config.AppConfig
import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

class TradeSetupEngine(
    private val appConfig: AppConfig
) {
    companion object {
        const val VERSION = "1.1.0"
    }
    
    fun create(signal: ScoredSignal, candles: List<Candle>): TradeSetup? {
        val orderBlock = signal.underlyingSignal.smcSignal as? OrderBlock ?: return null
        
        val entryPrice: BigDecimal = BigDecimal.ONE
        val stopLossPrice: BigDecimal = BigDecimal.ZERO

        val riskPercentage = if (entryPrice.compareTo(BigDecimal.ZERO) != 0) {
            ((entryPrice - stopLossPrice).abs() / entryPrice) * BigDecimal(100)
        } else {
            BigDecimal.ZERO
        }
        
        if (riskPercentage > BigDecimal(appConfig.maxStopLossPercentage)) {
            return null
        }

        val riskAmount = (entryPrice - stopLossPrice).abs()
        val riskToRewardRatio = 2.0
        val takeProfit1 = if (orderBlock.direction == FVGDireciton.BULLISH) entryPrice + riskAmount else entryPrice - riskAmount
        val takeProfit2 = if (orderBlock.direction == FVGDireciton.BULLISH) entryPrice + (riskAmount * BigDecimal(riskToRewardRatio)) else entryPrice - (riskAmount * BigDecimal(riskToRewardRatio))
        
        val explanation = SignalExplanation(
            title = "Analysis Study Details",
            components = listOf(
                ExplanationComponent(
                    "Study Setup",
                    "A theoretical study was generated with a reference price at $entryPrice",
                    mapOf("Projection Ratio" to "1:$riskToRewardRatio")
                )
            )
        )

        return TradeSetup(
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskToRewardRatio = "1:${riskToRewardRatio}",
            underlyingSignal = signal,
            versions = signal.underlyingSignal.versions + ("trade_setup_engine" to VERSION),
            explanation = explanation
        )
    }

    private fun calculateATR(candles: List<Candle>): BigDecimal {
        if (candles.size < 2) return BigDecimal.ZERO
        val trueRanges = (1 until candles.size).map {
            val prevClose = BigDecimal.valueOf(candles[it - 1].close.toDouble())
            val high = BigDecimal.valueOf(candles[it].high.toDouble())
            val low = BigDecimal.valueOf(candles[it].low.toDouble())
            maxOf((high - low).abs(), (high - prevClose).abs(), (low - prevClose).abs())
        }
        return trueRanges.reduce { acc, tr -> acc + tr } / BigDecimal(trueRanges.size)
    }
}
