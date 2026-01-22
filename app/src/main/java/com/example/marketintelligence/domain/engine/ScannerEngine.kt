package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.AIAnalysisResult
import kotlinx.coroutines.flow.Flow

/**
 * A common interface for any AI Scanner Engine.
 * This contract ensures that all engines are interchangeable.
 */
interface ScannerEngine : Engine {
    /**
     * The unique name of this engine, used for selection in settings.
     */
    override val engineName: String

    /**
     * Performs an AI analysis on a given stock symbol.
     *
     * @param symbol The stock symbol to analyze.
     * @param timeframe The chart timeframe.
     * @return A Flow emitting the Result of the AIAnalysisResult.
     */
    fun scan(symbol: String, timeframe: String): Flow<Result<AIAnalysisResult>>
}
