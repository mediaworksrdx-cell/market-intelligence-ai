package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.DataIntegrityEngine
import com.example.redxaiscanner.engine.TradeSetup
import com.example.redxaiscanner.engine.TradeSetupEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GenerateTradeSetupUseCase(
    private val validateSignalWithAIUseCase: ValidateSignalWithAIUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val dataIntegrityEngine: DataIntegrityEngine, // Injected for pre-analysis checks
    private val tradeSetupEngine: TradeSetupEngine
) {

    private val entryTimeframe = Timeframe.ONE_HOUR

    fun getTradeSetups(symbols: List<String>): Flow<Map<String, List<TradeSetup>>> {
        val validatedSignalsFlow = validateSignalWithAIUseCase.getValidatedSignals(symbols)
        val candlesFlow = marketDataRepository.getCandles(symbols, entryTimeframe)

        return combine(validatedSignalsFlow, candlesFlow) { validatedResults, candles ->
            val finalSetups = mutableMapOf<String, MutableList<TradeSetup>>()

            for ((symbol, results) in validatedResults) {
                val symbolCandles = candles[symbol] ?: continue
                
                // --- GOVERNANCE CONTROL: Data Integrity Check ---
                val integrityReport = dataIntegrityEngine.validate(symbolCandles)
                if (!integrityReport.isValid) {
                    // Log this failure for auditing
                    println("AUDIT REJECTION: Data for $symbol failed integrity check. Issues: ${integrityReport.issues}")
                    continue // Skip analysis for this symbol
                }

                val setups = results
                    .filter { it.underlyingSignal.underlyingSignal.integrityHash == integrityReport.dataHash } // Ensure signal was from this exact data
                    .mapNotNull { result -> tradeSetupEngine.create(result.underlyingSignal, symbolCandles) }
                
                if (setups.isNotEmpty()) {
                    finalSetups.getOrPut(symbol) { mutableListOf() }.addAll(setups)
                }
            }
            finalSetups
        }
    }
}