package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.source.remote.GeminiService
import com.example.marketintelligence.domain.engine.ScannerEngine
import com.example.marketintelligence.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GeminiScannerEngineImpl @Inject constructor(
    private val geminiService: GeminiService
) : ScannerEngine {

    override val engineName: String = "Standard (Gemini)"

    override suspend fun scanSymbol(symbol: String, timeframe: String): AIAnalysisResult {
        return geminiService.analyzeStock(symbol, timeframe)
    }

    override fun monitorSymbols(symbols: List<String>): Flow<AIAnalysisResult> = flow {
        // Mock continuous scanning logic
        for (symbol in symbols) {
            emit(geminiService.analyzeStock(symbol, "1H")) 
            kotlinx.coroutines.delay(2000)
        }
    }
}
