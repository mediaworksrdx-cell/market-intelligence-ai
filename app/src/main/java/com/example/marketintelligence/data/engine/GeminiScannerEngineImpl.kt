package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AIAnalysisResult
import com.example.marketintelligence.domain.engine.ScannerEngine
import com.example.marketintelligence.domain.repository.MarketDataRepository
import com.example.marketintelligence.data.source.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GeminiScannerEngineImpl @Inject constructor(
    private val geminiService: GeminiService,
    private val marketDataRepository: MarketDataRepository
) : ScannerEngine {

    override val engineName: String = "Standard (Gemini)"

    override fun scan(symbol: String, timeframe: String): Flow<Result<AIAnalysisResult>> = flow {
        try {
            val quoteResult = marketDataRepository.getQuote(symbol).first()
            val currentPrice = quoteResult.getOrNull()?.price
            
            val analysisResult = geminiService.scanStockWithGemini(symbol, timeframe, currentPrice)
            emit(Result.success(analysisResult))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
