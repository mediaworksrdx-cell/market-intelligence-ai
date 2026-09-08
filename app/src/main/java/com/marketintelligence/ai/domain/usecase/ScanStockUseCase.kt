package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.domain.engine.EngineRouter
import com.marketintelligence.ai.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScanStockUseCase @Inject constructor(
    private val engineRouter: EngineRouter
) {
    suspend operator fun invoke(symbol: String, timeframe: String = "1D"): AIAnalysisResult? {
        // Get the currently active scanner engine from the router
        val engine = engineRouter.activeScannerEngine.first()
        
        // Use the engine to scan the stock
        // Note: The original engine might return a specific type, so we adapt it here to the domain model
        // In a real scenario, the engine interface should return the domain model directly.
        // Assuming scanSymbol returns AIAnalysisResult
        return engine?.scanSymbol(symbol, timeframe)
    }
}
