package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.engine.ProprietaryChartEngine
import javax.inject.Inject

class ChartEngineFactory @Inject constructor(
    private val proprietaryEngine: ProprietaryChartEngine
) {
    fun getEngine(name: String): ChartEngine {
        return proprietaryEngine
    }
}
