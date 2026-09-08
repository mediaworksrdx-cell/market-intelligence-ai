package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.data.model.*
import com.marketintelligence.ai.domain.model.OptionChain
import com.marketintelligence.ai.domain.engine.*
import com.marketintelligence.ai.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FnoWorkflowUseCase @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val regimeEngine: MarketRegimeEngine,
    private val strategyEngine: StrategySelectionEngine,
    private val positionEngine: PositionConstructionEngine,
    private val riskEngine: RiskManagementEngine
) {
    suspend fun generateSignal(symbol: String): Result<AiTradeSignal> {
        return try {
            // 1. Get Live Data
            val optionChain: OptionChain = marketDataRepository.getOptionChain(symbol).first().getOrThrow()

            // 2. Run the AI Engine Pipeline
            val regime = regimeEngine.detect(symbol)
            val strategyName = strategyEngine.select(regime)
            val legs = positionEngine.construct(strategyName, optionChain)
            val riskParams = riskEngine.calculate(legs)
            
            // 3. Consolidate into the final AI Trade Signal
            val netPremium = legs.sumOf { if (it.action == TradeAction.BUY) -it.premium else it.premium }
            val signal = AiTradeSignal(
                underlyingSymbol = symbol,
                strategyName = strategyName,
                marketRegime = regime,
                optionLegs = legs,
                entryPrice = netPremium,
                target = netPremium * 2, // Placeholder
                stopLoss = netPremium * 0.5, // Placeholder
                confidenceScore = 0.85, // Placeholder
                riskParameters = riskParams
            )
            Result.success(signal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
