package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.engine.ProprietaryChartEngine
import javax.inject.Inject

class ChartEngineFactory @Inject constructor(
    private val proprietaryEngine: ProprietaryChartEngine
) {
    fun getEngine(name: String): ChartEngine {
        return proprietaryEngine
    }
}
