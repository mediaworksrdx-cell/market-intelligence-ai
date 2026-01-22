package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.audit.AuditLogger
import com.example.redxaiscanner.config.AppConfig
import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.ConfidenceScoringEngine
import com.example.redxaiscanner.engine.ScoredSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ScoreConfidenceUseCase(
    private val findConfluenceUseCase: FindConfluenceUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val scoringEngine: ConfidenceScoringEngine,
    private val auditLogger: AuditLogger, // Injected for audit logging
    private val appConfig: AppConfig // Injected for configuration-driven control
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
                        val scoredSignal = scoringEngine.score(signal, symbolCandles)
                        val passed = scoredSignal.confidenceScore >= appConfig.signalScoreThreshold
                        auditLogger.logSignalEvaluation(scoredSignal, passed) // Log every evaluation
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