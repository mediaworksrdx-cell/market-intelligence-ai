package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.model.BuildupType
import com.example.marketintelligence.domain.model.OptionChain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildupEngine @Inject constructor() {

    /**
     * Analyzes the Option Chain (specifically Futures data if available, or spot + IV)
     * to determine the market buildup.
     * 
     * Logic:
     * - Price Up + OI Up = Long Buildup
     * - Price Down + OI Up = Short Buildup
     * - Price Down + OI Down = Long Unwinding
     * - Price Up + OI Down = Short Covering
     */
    fun analyze(optionChain: OptionChain, priceChange: Double, oiChange: Double): BuildupType {
        // In a real scenario, we would use the cumulative OI change of the active future contract
        // or the aggregate OI change of the option chain.
        
        return when {
            priceChange > 0 && oiChange > 0 -> BuildupType.LONG_BUILDUP
            priceChange < 0 && oiChange > 0 -> BuildupType.SHORT_BUILDUP
            priceChange < 0 && oiChange < 0 -> BuildupType.LONG_UNWINDING
            priceChange > 0 && oiChange < 0 -> BuildupType.SHORT_COVERING
            else -> BuildupType.NEUTRAL
        }
    }
}
