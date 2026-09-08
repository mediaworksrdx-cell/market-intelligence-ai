package com.marketintelligence.redxfnoscanner.domain

import com.marketintelligence.redxfnoscanner.data.FnoData

/**
 * Implements Module 8: Market Regime Classification.
 * Determines the state of the market to filter strategies and set risk parameters.
 */
enum class MarketRegime {
    TRENDING_UP,
    TRENDING_DOWN,
    RANGE_BOUND,
    VOLATILITY_EXPANSION,
    EVENT_RISK,
    TRENDING, // Restored
    VOLATILITY_DRIVEN // Restored
}

class MarketRegimeIdentifier {
    
    // Module 8: Regime Classification Logic
    fun identifyRegime(fnoData: FnoData, optionChainAnalyzer: OptionChainAnalyzer): MarketRegime {
        val optionChain = fnoData.optionChains.firstOrNull() ?: return MarketRegime.RANGE_BOUND

        val pcr = optionChainAnalyzer.calculatePCR(optionChain)
        val buildupAnalysis = optionChainAnalyzer.analyzeBuildup(optionChain)
        
        // IV Analysis (Module 4.1)
        val averageIV = optionChain.options.filter { it.impliedVolatility > 0 }.map { it.impliedVolatility }.average()
        
        val longBuildups = buildupAnalysis.count { it.buildupType == BuildupType.LONG_BUILDUP }
        val shortBuildups = buildupAnalysis.count { it.buildupType == BuildupType.SHORT_BUILDUP }
        val shortCovering = buildupAnalysis.count { it.buildupType == BuildupType.SHORT_COVERING }
        val longUnwinding = buildupAnalysis.count { it.buildupType == BuildupType.LONG_UNWINDING }

        // Logic Hierarchy (Module 12)
        return when {
            // 1. Volatility check first
            averageIV > 25.0 -> MarketRegime.VOLATILITY_EXPANSION
            
            // 2. Strong Directional Bias via PCR & Buildups
            pcr > 1.3 && (longBuildups + shortCovering > shortBuildups + longUnwinding) -> MarketRegime.TRENDING_UP
            pcr < 0.7 && (shortBuildups + longUnwinding > longBuildups + shortCovering) -> MarketRegime.TRENDING_DOWN
            
            // 3. Default to Range
            else -> MarketRegime.RANGE_BOUND
        }
    }
}
