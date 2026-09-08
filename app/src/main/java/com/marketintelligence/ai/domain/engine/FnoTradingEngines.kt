
package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.model.*
import com.marketintelligence.ai.domain.model.OptionChain
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
        val spot = optionChain.underlyingPrice.takeIf { it > 0.0 } ?: 22000.0
        val atmStrike = (kotlin.math.round(spot / 100.0) * 100.0)
        val otmStrike = atmStrike + 200.0
        val lotSize = 50

        return when (strategyName) {
            "Bull Call Spread" -> listOf(
                OptionLeg("${optionChain.symbol} ATM CE", OptionType.CE, atmStrike, "CURRENT", TradeAction.BUY, lotSize, 120.0),
                OptionLeg("${optionChain.symbol} OTM CE", OptionType.CE, otmStrike, "CURRENT", TradeAction.SELL, lotSize, 60.0)
            )
            "Bear Put Spread" -> listOf(
                OptionLeg("${optionChain.symbol} ATM PE", OptionType.PE, atmStrike, "CURRENT", TradeAction.BUY, lotSize, 120.0),
                OptionLeg("${optionChain.symbol} OTM PE", OptionType.PE, atmStrike - 200.0, "CURRENT", TradeAction.SELL, lotSize, 60.0)
            )
            else -> listOf(
                OptionLeg("${optionChain.symbol} ATM CE", OptionType.CE, atmStrike, "CURRENT", TradeAction.BUY, lotSize, 100.0),
                OptionLeg("${optionChain.symbol} OTM CE", OptionType.CE, otmStrike, "CURRENT", TradeAction.SELL, lotSize, 50.0)
            )
        }
    }
}

@Singleton
class RiskManagementEngine @Inject constructor() {
    fun calculate(legs: List<OptionLeg>): RiskParameters {
        val netDebit = legs.sumOf { 
            if (it.action == TradeAction.BUY) it.premium * it.quantity else -it.premium * it.quantity
        }
        val maxRisk = netDebit.coerceAtLeast(1000.0)
        return RiskParameters(
            positionSize = 1,
            capitalRequired = maxRisk * 1.5,
            marginRequired = maxRisk * 2.5,
            maxLoss = maxRisk
        )
    }
}
