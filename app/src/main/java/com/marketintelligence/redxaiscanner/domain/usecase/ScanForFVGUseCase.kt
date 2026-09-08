package com.marketintelligence.redxaiscanner.domain.usecase

import com.marketintelligence.redxaiscanner.domain.model.Timeframe
import com.marketintelligence.redxaiscanner.domain.repository.MarketDataRepository
import com.marketintelligence.redxaiscanner.engine.FVGEngine
import com.marketintelligence.redxaiscanner.engine.FVGResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanForFVGUseCase(
    private val marketDataRepository: MarketDataRepository,
    private val fvgEngine: FVGEngine
) {

    /**
     * Scans for Fair Value Gaps (FVGs) for a given list of symbols and timeframe.
     *
     * @param symbols The list of market symbols to scan.
     * @param timeframe The timeframe to analyze.
     * @return A Flow emitting a map where the key is the symbol and the value is a list of detected FVGs.
     */
    fun scan(symbols: List<String>, timeframe: Timeframe): Flow<Map<String, List<FVGResult>>> {
        return marketDataRepository.getCandles(symbols, timeframe)
            .map { symbolCandleMap ->
                symbolCandleMap.mapValues { (_, candles) ->
                    if (candles.size >= 3) {
                        fvgEngine.analyze(candles)
                    } else {
                        emptyList()
                    }
                }
            }
    }
}