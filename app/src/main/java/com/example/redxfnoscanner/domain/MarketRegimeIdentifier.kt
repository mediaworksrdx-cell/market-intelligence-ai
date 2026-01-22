package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.FnoData

enum class MarketRegime {
    TRENDING,
    RANGE_BOUND,
    VOLATILITY_DRIVEN
}

class MarketRegimeIdentifier {
    fun identifyRegime(fnoData: FnoData, optionChainAnalyzer: OptionChainAnalyzer): MarketRegime {
        val optionChain = fnoData.optionChains.firstOrNull() ?: return MarketRegime.RANGE_BOUND

        val pcr = optionChainAnalyzer.calculatePCR(optionChain)
        val buildupAnalysis = optionChainAnalyzer.analyzeBuildup(optionChain)
        val averageIV = optionChain.options.map { it.impliedVolatility }.average()

        val longBuildups = buildupAnalysis.count { it.buildupType == BuildupType.LONG_BUILDUP }
        val shortBuildups = buildupAnalysis.count { it.buildupType == BuildupType.SHORT_BUILDUP }

        return when {
            // High IV suggests volatility
            averageIV > 30 -> MarketRegime.VOLATILITY_DRIVEN
            
            // Strong directional bias suggests a trending market
            pcr > 1.2 || pcr < 0.8 || (longBuildups > shortBuildups * 1.5) -> MarketRegime.TRENDING
            
            // Otherwise, assume a range-bound market
            else -> MarketRegime.RANGE_BOUND
        }
    }
}
