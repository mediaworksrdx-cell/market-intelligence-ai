package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.Option
import com.example.redxfnoscanner.data.OptionChain

enum class BuildupType {
    LONG_BUILDUP,
    SHORT_BUILDUP,
    SHORT_COVERING,
    LONG_UNWINDING
}

data class OIAnalysis(val buildupType: BuildupType, val strikePrice: Double, val optionType: String)

class OptionChainAnalyzer {

    fun analyzeBuildup(optionChain: OptionChain): List<OIAnalysis> {
        return optionChain.options.mapNotNull { option ->
            val buildup = when {
                option.priceChange > 0 && option.changeInOpenInterest > 0 -> BuildupType.LONG_BUILDUP
                option.priceChange < 0 && option.changeInOpenInterest > 0 -> BuildupType.SHORT_BUILDUP
                option.priceChange > 0 && option.changeInOpenInterest < 0 -> BuildupType.SHORT_COVERING
                option.priceChange < 0 && option.changeInOpenInterest < 0 -> BuildupType.LONG_UNWINDING
                else -> null
            }
            buildup?.let { OIAnalysis(it, option.strikePrice, option.type) }
        }
    }

    fun calculateTotalOI(optionChain: OptionChain): Pair<Int, Int> {
        val callOI = optionChain.options.filter { it.type == "CE" }.sumOf { it.openInterest }
        val putOI = optionChain.options.filter { it.type == "PE" }.sumOf { it.openInterest }
        return Pair(callOI, putOI)
    }

    fun calculatePCR(optionChain: OptionChain): Double {
        val (callOI, putOI) = calculateTotalOI(optionChain)
        return if (callOI > 0) putOI.toDouble() / callOI.toDouble() else 0.0
    }

    fun findMaxPain(optionChain: OptionChain): Double {
        // Placeholder for max pain calculation
        return 0.0
    }

    fun findSupportAndResistance(optionChain: OptionChain): Pair<Double, Double> {
        val support = optionChain.options.filter { it.type == "PE" }.maxByOrNull { it.openInterest }?.strikePrice ?: 0.0
        val resistance = optionChain.options.filter { it.type == "CE" }.maxByOrNull { it.openInterest }?.strikePrice ?: 0.0
        return Pair(support, resistance)
    }
}
