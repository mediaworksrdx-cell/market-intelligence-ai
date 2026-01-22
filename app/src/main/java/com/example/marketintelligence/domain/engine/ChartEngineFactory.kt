package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.engine.TradingViewChartEngine
import com.example.marketintelligence.data.engine.ProprietaryChartEngine
import javax.inject.Inject

class ChartEngineFactory @Inject constructor(
    private val tradingViewEngine: TradingViewChartEngine,
    private val proprietaryEngine: ProprietaryChartEngine
) {
    fun getEngine(name: String): ChartEngine {
        return if (name == proprietaryEngine.engineName) {
            proprietaryEngine
        } else {
            tradingViewEngine
        }
    }
}
