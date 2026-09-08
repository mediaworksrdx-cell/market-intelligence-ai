package com.example.marketintelligence.domain.usecase

import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanStockUseCase @Inject constructor(
    private val engineRouter: EngineRouter
) {
    suspend operator fun invoke(symbol: String, timeframe: String = "1D"): AIAnalysisResult? {
        // Get the currently active scanner engine from the router
        val engine = engineRouter.activeScannerEngine.first()
        
        return engine?.scanSymbol(symbol, timeframe)
    }
}
