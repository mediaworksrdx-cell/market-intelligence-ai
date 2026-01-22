package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.PatternEngine
import com.example.redxaiscanner.engine.PatternResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanForPatternsUseCase(
    private val marketDataRepository: MarketDataRepository,
    private val patternEngine: PatternEngine
) {

    /**
     * Scans for chart patterns for a given list of symbols and timeframe.
     *
     * @param symbols The list of market symbols to scan.
     * @param timeframe The timeframe to analyze.
     * @return A Flow emitting a map where the key is the symbol and the value is a list of detected patterns.
     */
    fun scan(symbols: List<String>, timeframe: Timeframe): Flow<Map<String, List<PatternResult>>> {
        return marketDataRepository.getCandles(symbols, timeframe)
            .map { symbolCandleMap ->
                // Process the map to analyze each list of candles
                symbolCandleMap.mapValues { (_, candles) ->
                    if (candles.isNotEmpty()) {
                        patternEngine.analyze(candles)
                    } else {
                        emptyList()
                    }
                }
            }
    }
}