package com.marketintelligence.redxaiscanner.domain.usecase

import com.marketintelligence.redxaiscanner.domain.model.Timeframe
import com.marketintelligence.redxaiscanner.domain.repository.MarketDataRepository
import com.marketintelligence.redxaiscanner.engine.DataIntegrityEngine
import com.marketintelligence.redxaiscanner.engine.TradeSetup
import com.marketintelligence.redxaiscanner.engine.TradeSetupEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GenerateTradeSetupUseCase(
    private val validateSignalWithAIUseCase: ValidateSignalWithAIUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val dataIntegrityEngine: DataIntegrityEngine,
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
                
                val integrityReport = dataIntegrityEngine.validate(symbolCandles)
                if (!integrityReport.isValid) {
                    continue
                }

                val setups = results
                    .filter { it.underlyingSignal.underlyingSignal.integrityHash == integrityReport.dataHash }
                    .mapNotNull { result -> 
                        tradeSetupEngine.create(
                            result.underlyingSignal, 
                            symbolCandles, 
                            result.underlyingSignal.underlyingSignal.liquidityZones
                        ) 
                    }
                
                if (setups.isNotEmpty()) {
                    finalSetups.getOrPut(symbol) { mutableListOf() }.addAll(setups)
                }
            }
            finalSetups
        }
    }
}
