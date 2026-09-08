package com.marketintelligence.redxaiscanner.domain.usecase

import com.marketintelligence.redxaiscanner.audit.AuditLogger
import com.marketintelligence.redxaiscanner.config.AppConfig
import com.marketintelligence.redxaiscanner.domain.model.Timeframe
import com.marketintelligence.redxaiscanner.domain.repository.MarketDataRepository
import com.marketintelligence.redxaiscanner.engine.ConfidenceScoringEngine
import com.marketintelligence.redxaiscanner.engine.ScoredSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ScoreConfidenceUseCase(
    private val findConfluenceUseCase: FindConfluenceUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val scoringEngine: ConfidenceScoringEngine,
    private val auditLogger: AuditLogger,
    private val appConfig: AppConfig
) {

    private val entryTimeframe = Timeframe.ONE_HOUR

    fun getScoredSignals(symbols: List<String>): Flow<Map<String, List<ScoredSignal>>> {
        val confluenceSignalsFlow = findConfluenceUseCase.find(symbols)
        val candlesFlow = marketDataRepository.getCandles(symbols, entryTimeframe)

        return combine(confluenceSignalsFlow, candlesFlow) { confluenceSignals, candles ->
            val finalSignals = mutableMapOf<String, MutableList<ScoredSignal>>()

            for ((symbol, signals) in confluenceSignals) {
                val symbolCandles = candles[symbol] ?: continue
                
                val scoredAndFiltered = signals
                    .map { signal -> 
                        val scoredSignal = scoringEngine.score(signal, symbolCandles, signal.regime, signal.liquidityZones)
                        val passed = scoredSignal.confidenceScore >= appConfig.signalScoreThreshold
                        auditLogger.logSignalEvaluation(scoredSignal, passed)
                        if (passed) scoredSignal else null
                    }
                    .filterNotNull()
                
                if (scoredAndFiltered.isNotEmpty()) {
                    finalSignals.getOrPut(symbol) { mutableListOf() }.addAll(scoredAndFiltered)
                }
            }
            finalSignals
        }
    }
}
