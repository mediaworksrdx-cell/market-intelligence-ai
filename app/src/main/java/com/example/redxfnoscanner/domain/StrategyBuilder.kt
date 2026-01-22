package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.FnoData
import com.example.redxfnoscanner.data.Option

class StrategyBuilder {

    fun build(strategy: OptionStrategy, fnoData: FnoData): Position? {
        return when (strategy) {
            OptionStrategy.BULL_CALL_SPREAD -> buildBullCallSpread(fnoData)
            // Other strategies can be added here
            else -> null
        }
    }

    private fun buildBullCallSpread(fnoData: FnoData): Position? {
        val spotPrice = fnoData.spotPrice.price
        val options = fnoData.optionChains.firstOrNull()?.options ?: return null

        // Find an ATM (At-The-Money) call to buy
        val longCall = options
            .filter { it.type == "CE" && it.strikePrice >= spotPrice }
            .minByOrNull { it.strikePrice } ?: return null

        // Find an OTM (Out-of-The-Money) call to sell
        val shortCall = options
            .filter { it.type == "CE" && it.strikePrice > longCall.strikePrice }
            .minByOrNull { it.strikePrice } ?: return null

        return Position(
            legs = listOf(
                Leg(option = longCall, quantity = 1), // Buy call
                Leg(option = shortCall, quantity = -1) // Sell call
            )
        )
    }
}
