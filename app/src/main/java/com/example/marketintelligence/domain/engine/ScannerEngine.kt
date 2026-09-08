package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.engine.Engine
import com.example.marketintelligence.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.Flow

interface ScannerEngine : Engine {
    /**
     * Performs a single scan on the given symbol and timeframe.
     */
    suspend fun scanSymbol(symbol: String, timeframe: String): AIAnalysisResult

    /**
     * Continuously monitors a list of symbols and emits results as they are found.
     */
    fun monitorSymbols(symbols: List<String>): Flow<AIAnalysisResult>
}
