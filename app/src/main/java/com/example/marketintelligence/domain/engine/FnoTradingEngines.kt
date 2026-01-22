
package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.*
import com.example.marketintelligence.domain.model.OptionChain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrategySelectionEngine @Inject constructor() {
    fun select(regime: MarketRegime): String {
        return when (regime) {
            MarketRegime.TRENDING_BULLISH -> "Bull Call Spread"
            MarketRegime.TRENDING_BEARISH -> "Bear Put Spread"
            else -> "Iron Condor"
        }
    }
}

@Singleton
class PositionConstructionEngine @Inject constructor() {
    fun construct(strategyName: String, optionChain: OptionChain): List<OptionLeg> {
        return listOf(
            OptionLeg("NIFTY24FEB22000CE", OptionType.CE, 22000.0, "29FEB24", TradeAction.BUY, 50, 150.0),
            OptionLeg("NIFTY24FEB22200CE", OptionType.CE, 22200.0, "29FEB24", TradeAction.SELL, 50, 80.0)
        )
    }
}

@Singleton
class RiskManagementEngine @Inject constructor() {
    fun calculate(legs: List<OptionLeg>): RiskParameters {
        val netPremium = legs.sumOf { 
            if (it.action == TradeAction.BUY) -it.premium * it.quantity else it.premium * it.quantity
        }
        return RiskParameters(
            positionSize = 1,
            capitalRequired = 50000.0,
            marginRequired = 120000.0,
            maxLoss = kotlin.math.abs(netPremium) * 50
        )
    }
}
