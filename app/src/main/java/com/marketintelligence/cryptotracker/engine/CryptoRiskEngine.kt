package com.marketintelligence.cryptotracker.engine

class CryptoRiskEngine(
    private val minRiskReward: Double = 2.0,
    private val maxRiskPercent: Double = 2.0
) {
    data class RiskCalculation(
        val entry: Double,
        val stopLoss: Double,
        val tp1: Double,
        val tp2: Double,
        val tp3: Double,
        val riskRewardRatio: Double,
        val riskPercent: Double,
        val isAcceptable: Boolean
    )

    fun calculate(
        direction: SetupDirection,
        fvg: EnrichedFVG?,
        smcAnalysis: CryptoSMCAnalysis,
        currentPrice: Double
    ): RiskCalculation {
        val entry: Double
        var stopLoss: Double

        if (direction == SetupDirection.LONG) {
            val fvgMidpoint = fvg?.let { (it.top + it.bottom) / 2 }
            entry = fvgMidpoint ?: fvg?.top ?: currentPrice
            val fvgBottom = fvg?.bottom ?: currentPrice
            val lastSwingLow = smcAnalysis.swingPoints.lastOrNull { it.type == SwingType.LOW }?.price ?: currentPrice
            stopLoss = minOf(fvgBottom, lastSwingLow)
            stopLoss -= stopLoss * 0.001 // 0.1% buffer

            val bslLevels = smcAnalysis.liquidityLevels
                .filter { it.side == LiquiditySide.BUY && it.price > entry }
                .sortedBy { it.price }

            val riskAmount = entry - stopLoss
            val tp1 = bslLevels.getOrNull(0)?.price ?: (entry + riskAmount * 2)
            val tp2 = bslLevels.getOrNull(1)?.price ?: (entry + riskAmount * 3)
            val tp3 = bslLevels.getOrNull(2)?.price ?: (entry + riskAmount * 4)

            val rewardAmount = tp1 - entry
            val riskRewardRatio = if (riskAmount > 0) rewardAmount / riskAmount else 0.0
            val riskPercent = if (entry > 0) (riskAmount / entry) * 100 else 0.0
            val isAcceptable = riskRewardRatio >= minRiskReward && riskPercent <= maxRiskPercent

            return RiskCalculation(
                entry = entry,
                stopLoss = stopLoss,
                tp1 = tp1,
                tp2 = tp2,
                tp3 = tp3,
                riskRewardRatio = riskRewardRatio,
                riskPercent = riskPercent,
                isAcceptable = isAcceptable
            )
        } else {
            val fvgMidpoint = fvg?.let { (it.top + it.bottom) / 2 }
            entry = fvgMidpoint ?: fvg?.bottom ?: currentPrice
            val fvgTop = fvg?.top ?: currentPrice
            val lastSwingHigh = smcAnalysis.swingPoints.lastOrNull { it.type == SwingType.HIGH }?.price ?: currentPrice
            stopLoss = maxOf(fvgTop, lastSwingHigh)
            stopLoss += stopLoss * 0.001 // 0.1% buffer

            val sslLevels = smcAnalysis.liquidityLevels
                .filter { it.side == LiquiditySide.SELL && it.price < entry }
                .sortedByDescending { it.price }

            val riskAmount = stopLoss - entry
            val tp1 = sslLevels.getOrNull(0)?.price ?: (entry - riskAmount * 2)
            val tp2 = sslLevels.getOrNull(1)?.price ?: (entry - riskAmount * 3)
            val tp3 = sslLevels.getOrNull(2)?.price ?: (entry - riskAmount * 4)

            val rewardAmount = entry - tp1
            val riskRewardRatio = if (riskAmount > 0) rewardAmount / riskAmount else 0.0
            val riskPercent = if (entry > 0) (riskAmount / entry) * 100 else 0.0
            val isAcceptable = riskRewardRatio >= minRiskReward && riskPercent <= maxRiskPercent

            return RiskCalculation(
                entry = entry,
                stopLoss = stopLoss,
                tp1 = tp1,
                tp2 = tp2,
                tp3 = tp3,
                riskRewardRatio = riskRewardRatio,
                riskPercent = riskPercent,
                isAcceptable = isAcceptable
            )
        }
    }
}
