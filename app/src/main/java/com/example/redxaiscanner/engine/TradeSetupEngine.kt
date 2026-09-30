package com.example.redxaiscanner.engine

import com.example.redxaiscanner.config.AppConfig
import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal
import java.math.RoundingMode

class TradeSetupEngine(
    private val appConfig: AppConfig
) {
    companion object {
        const val VERSION = "2.0.0"
    }
    
    fun create(signal: ScoredSignal, candles: List<Candle>, liquidityZones: List<LiquidityZone>): TradeSetup? {
        val smcSignal = signal.underlyingSignal.smcSignal
        
        var entryPrice = BigDecimal.ZERO
        var stopLossPrice = BigDecimal.ZERO
        var bias = FVGDireciton.BULLISH

        // Entry & SL Logic (Rule 6/12)
        if (smcSignal is OrderBlock) {
            if (smcSignal.direction == FVGDireciton.BULLISH) {
                entryPrice = smcSignal.top // Proximal
                stopLossPrice = smcSignal.bottom // Distal (Invalidation)
                bias = FVGDireciton.BULLISH
            } else {
                entryPrice = smcSignal.bottom // Proximal
                stopLossPrice = smcSignal.top // Distal
                bias = FVGDireciton.BEARISH
            }
        } else {
            return null
        }

        // Validate Risk (Rule 12)
        if (entryPrice.compareTo(BigDecimal.ZERO) == 0) return null
        val riskAmount = (entryPrice - stopLossPrice).abs()
        if (riskAmount.compareTo(BigDecimal.ZERO) == 0) return null

        val riskPercentage = (riskAmount.divide(entryPrice, 4, RoundingMode.HALF_UP)) * BigDecimal(100)
        if (riskPercentage.toDouble() > appConfig.maxStopLossPercentage) {
            return null
        }

        // Targets based on Liquidity (Rule 4/13)
        // Using compareTo for safety in filter/sort
        val targets = if (bias == FVGDireciton.BULLISH) {
            liquidityZones.filter { it.priceLevel.compareTo(entryPrice) > 0 }.sortedBy { it.priceLevel }
        } else {
            liquidityZones.filter { it.priceLevel.compareTo(entryPrice) < 0 }.sortedByDescending { it.priceLevel }
        }

        val takeProfit1 = if (targets.isNotEmpty()) targets[0].priceLevel else (if(bias == FVGDireciton.BULLISH) entryPrice + riskAmount * BigDecimal("2") else entryPrice - riskAmount * BigDecimal("2"))
        val takeProfit2 = if (targets.size > 1) targets[1].priceLevel else (if(bias == FVGDireciton.BULLISH) entryPrice + riskAmount * BigDecimal("4") else entryPrice - riskAmount * BigDecimal("4"))

        val rr = if (riskAmount.compareTo(BigDecimal.ZERO) != 0) {
            (takeProfit1 - entryPrice).abs().divide(riskAmount, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        // Rule 13: Explainability
        val explanation = SignalExplanation(
            title = "Institutional Study",
            components = listOf(
                ExplanationComponent(
                    "Study Logic",
                    "Key Level at Institutional Block (${if(bias == FVGDireciton.BULLISH) "Discount" else "Premium"})",
                    mapOf("Level" to entryPrice.toPlainString())
                ),
                ExplanationComponent(
                    "Invalidation Level",
                    "Structural Failure below Block Distal Line",
                    mapOf("Level" to stopLossPrice.toPlainString())
                ),
                ExplanationComponent(
                    "Liquidity Projections",
                    if (targets.isNotEmpty()) "Projecting internal/external liquidity pools" else "Projected 1:${rr} ratio based on volatility",
                    mapOf("Level 1" to takeProfit1.toPlainString())
                )
            )
        )

        return TradeSetup(
            entryPrice = entryPrice,
            stopLossPrice = stopLossPrice,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskToRewardRatio = "1:$rr",
            underlyingSignal = signal,
            versions = signal.underlyingSignal.versions + ("trade_setup_engine" to VERSION),
            explanation = explanation
        )
    }
}
