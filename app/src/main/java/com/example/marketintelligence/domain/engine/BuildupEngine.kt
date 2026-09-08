package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.model.BuildupType
import com.example.marketintelligence.domain.model.OptionChain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildupEngine @Inject constructor() {

    fun analyze(optionChain: OptionChain, priceChange: Double, oiChange: Double): BuildupType {
        return when {
            priceChange > 0 && oiChange > 0 -> BuildupType.LONG_BUILDUP
            priceChange < 0 && oiChange > 0 -> BuildupType.SHORT_BUILDUP
            priceChange < 0 && oiChange < 0 -> BuildupType.LONG_UNWINDING
            priceChange > 0 && oiChange < 0 -> BuildupType.SHORT_COVERING
            else -> BuildupType.NEUTRAL
        }
    }
}
