package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.FnoData

data class StrategyEvaluation(val strategy: OptionStrategy, val score: Double)

class StrategySelector {

    fun selectStrategy(
        marketRegime: MarketRegime,
        fnoData: FnoData,
        optionChainAnalyzer: OptionChainAnalyzer
    ): OptionStrategy? {
        val potentialStrategies = strategyMap[marketRegime] ?: return null

        val evaluations = potentialStrategies.map {
            val score = evaluateStrategy(it, fnoData, optionChainAnalyzer)
            StrategyEvaluation(it, score)
        }

        return evaluations.maxByOrNull { it.score }?.strategy
    }

    private fun evaluateStrategy(
        strategy: OptionStrategy,
        fnoData: FnoData,
        optionChainAnalyzer: OptionChainAnalyzer
    ): Double {
        // Placeholder for a more sophisticated AI scoring model.
        // This could involve a combination of rule-based filters and a machine learning model.

        val pcr = optionChainAnalyzer.calculatePCR(fnoData.optionChains.first())

        return when (strategy) {
            OptionStrategy.BULL_CALL_SPREAD -> if (pcr > 1.0) 0.8 else 0.2
            OptionStrategy.BEAR_PUT_SPREAD -> if (pcr < 1.0) 0.8 else 0.2
            OptionStrategy.IRON_CONDOR -> if (fnoData.spotPrice.price in 500.0..1000.0) 0.9 else 0.1
            else -> 0.5
        }
    }
}
