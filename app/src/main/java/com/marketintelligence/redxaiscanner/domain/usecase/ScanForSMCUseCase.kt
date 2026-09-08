package com.marketintelligence.redxaiscanner.domain.usecase

import com.marketintelligence.redxaiscanner.domain.model.Timeframe
import com.marketintelligence.redxaiscanner.domain.repository.MarketDataRepository
import com.marketintelligence.redxaiscanner.engine.SMCAnalysisResult
import com.marketintelligence.redxaiscanner.engine.SMCEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanForSMCUseCase(
    private val marketDataRepository: MarketDataRepository,
    private val smcEngine: SMCEngine
) {

    /**
     * Scans for Smart Money Concepts (SMC) for a given list of symbols and timeframe.
     *
     * @param symbols The list of market symbols to scan.
     * @param timeframe The timeframe to analyze.
     * @return A Flow emitting a map where the key is the symbol and the value is the full SMC analysis.
     */
    fun scan(symbols: List<String>, timeframe: Timeframe): Flow<Map<String, SMCAnalysisResult>> {
        return marketDataRepository.getCandles(symbols, timeframe)
            .map { symbolCandleMap ->
                symbolCandleMap.mapValues { (_, candles) ->
                    // The engine performs the heavy lifting of analysis
                    smcEngine.analyze(candles)
                }
            }
    }
}