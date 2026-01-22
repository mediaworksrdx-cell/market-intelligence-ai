package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.AIValidationEngine
import com.example.redxaiscanner.engine.AIValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ValidateSignalWithAIUseCase(
    private val scoreConfidenceUseCase: ScoreConfidenceUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val aiValidationEngine: AIValidationEngine,
    private val isAiValidationEnabled: Boolean = true // This can be controlled by a feature flag
) {

    private val entryTimeframe = Timeframe.ONE_HOUR

    /**
     * Fetches scored signals and runs them through the AI validation layer.
     * If AI validation is disabled, it simply passes the original signals through.
     *
     * @param symbols The list of market symbols to analyze.
     * @return A Flow emitting a map of symbols to their AI-validated results.
     */
    fun getValidatedSignals(symbols: List<String>): Flow<Map<String, List<AIValidationResult>>> {
        val scoredSignalsFlow = scoreConfidenceUseCase.getScoredSignals(symbols)
        val candlesFlow = marketDataRepository.getCandles(symbols, entryTimeframe)

        return combine(scoredSignalsFlow, candlesFlow) { scoredSignals, candles ->
            val finalResults = mutableMapOf<String, MutableList<AIValidationResult>>()

            for ((symbol, signals) in scoredSignals) {
                val symbolCandles = candles[symbol] ?: continue
                
                val validatedSignals = signals.map { signal ->
                    if (isAiValidationEnabled) {
                        aiValidationEngine.validate(signal, symbolCandles)
                    } else {
                        // If AI is off, create a "pass-through" result that always validates
                        AIValidationResult(isValidated = true, aiConfidenceScore = signal.confidenceScore, underlyingSignal = signal)
                    }
                }
                .filter { it.isValidated } // Only pass signals the AI has approved
                
                if (validatedSignals.isNotEmpty()) {
                    finalResults.getOrPut(symbol) { mutableListOf() }.addAll(validatedSignals)
                }
            }
            finalResults
        }
    }
}