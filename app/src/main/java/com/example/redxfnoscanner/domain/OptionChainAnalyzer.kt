package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.Option
import com.example.redxfnoscanner.data.OptionChain
import kotlin.math.abs

enum class BuildupType {
    LONG_BUILDUP,
    SHORT_BUILDUP,
    SHORT_COVERING,
    LONG_UNWINDING,
    NEUTRAL
}

data class OIAnalysis(val buildupType: BuildupType, val strikePrice: Double, val optionType: String)

/**
 * Implements Module 3 (Positioning), Module 5 (Intelligence), and Module 6 (Gamma).
 */
class OptionChainAnalyzer {

    // Module 3.2: Buildup Classification Engine
    fun analyzeBuildup(optionChain: OptionChain): List<OIAnalysis> {
        return optionChain.options.mapNotNull { option ->
            val buildup = when {
                option.priceChange > 0 && option.changeInOpenInterest > 0 -> BuildupType.LONG_BUILDUP
                option.priceChange < 0 && option.changeInOpenInterest > 0 -> BuildupType.SHORT_BUILDUP
                option.priceChange > 0 && option.changeInOpenInterest < 0 -> BuildupType.SHORT_COVERING
                option.priceChange < 0 && option.changeInOpenInterest < 0 -> BuildupType.LONG_UNWINDING
                else -> BuildupType.NEUTRAL
            }
            if (buildup != BuildupType.NEUTRAL) OIAnalysis(buildup, option.strikePrice, option.type) else null
        }
    }

    // Module 4.3: PCR Analytics
    fun calculateTotalOI(optionChain: OptionChain): Pair<Int, Int> {
        val callOI = optionChain.options.filter { it.type == "CE" }.sumOf { it.openInterest }
        val putOI = optionChain.options.filter { it.type == "PE" }.sumOf { it.openInterest }
        return Pair(callOI, putOI)
    }

    fun calculatePCR(optionChain: OptionChain): Double {
        val (callOI, putOI) = calculateTotalOI(optionChain)
        return if (callOI > 0) putOI.toDouble() / callOI.toDouble() else 0.0
    }

    // Module 5.2: Max Pain Engine
    fun findMaxPain(optionChain: OptionChain): Double {
        if (optionChain.options.isEmpty()) return 0.0
        
        val strikes = optionChain.options.map { it.strikePrice }.distinct().sorted()
        var minTotalLoss = Double.MAX_VALUE
        var maxPainStrike = 0.0
        
        for (strike in strikes) {
            var totalLoss = 0.0
            
            // Calculate loss for writers if expiry is at 'strike'
            optionChain.options.forEach { option ->
                if (option.type == "CE") {
                    val intrinsicValue = maxOf(0.0, strike - option.strikePrice)
                    totalLoss += intrinsicValue * option.openInterest
                } else if (option.type == "PE") {
                    val intrinsicValue = maxOf(0.0, option.strikePrice - strike)
                    totalLoss += intrinsicValue * option.openInterest
                }
            }
            
            if (totalLoss < minTotalLoss) {
                minTotalLoss = totalLoss
                maxPainStrike = strike
            }
        }
        return maxPainStrike
    }

    // Module 5.1: OI Cluster Analyzer (Liquidity Magnet Zones)
    fun findSupportAndResistance(optionChain: OptionChain): Pair<Double, Double> {
        val support = optionChain.options.filter { it.type == "PE" }.maxByOrNull { it.openInterest }?.strikePrice ?: 0.0
        val resistance = optionChain.options.filter { it.type == "CE" }.maxByOrNull { it.openInterest }?.strikePrice ?: 0.0
        return Pair(support, resistance)
    }
    
    // Module 6: Gamma & Dealer Positioning (Simplified)
    fun estimateNetGammaState(optionChain: OptionChain, spotPrice: Double): String {
        // Heuristic: If Spot > Max Pain + 2% -> Dealers might be short gamma (accelerating move)
        // If Spot near Max Pain -> Dealers are long gamma (pinning)
        val maxPain = findMaxPain(optionChain)
        val deviation = (spotPrice - maxPain) / maxPain
        
        return when {
            abs(deviation) < 0.01 -> "LONG_GAMMA (Pinning Likely)"
            deviation > 0.03 -> "SHORT_GAMMA (Vol Expansion Up)"
            deviation < -0.03 -> "SHORT_GAMMA (Vol Expansion Down)"
            else -> "NEUTRAL"
        }
    }
}
