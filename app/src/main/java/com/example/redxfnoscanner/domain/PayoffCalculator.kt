package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.Option
import kotlin.math.max

data class PayoffPoint(val price: Double, val profitLoss: Double)

class PayoffCalculator {

    fun calculatePayoffRange(position: Position): List<PayoffPoint> {
        if (position.legs.isEmpty()) return emptyList()

        val strikes = position.legs.map { it.option.strikePrice }
        val minStrike = strikes.minOrNull() ?: 0.0
        val maxStrike = strikes.maxOrNull() ?: 0.0
        val priceRangeStart = minStrike * 0.95
        val priceRangeEnd = maxStrike * 1.05
        val step = (priceRangeEnd - priceRangeStart) / 20

        val payoffPoints = mutableListOf<PayoffPoint>()
        for (i in 0..20) {
            val price = priceRangeStart + i * step
            val profitLoss = calculatePnlAtPrice(position, price)
            payoffPoints.add(PayoffPoint(price, profitLoss))
        }
        return payoffPoints
    }

    fun calculatePnlAtPrice(position: Position, price: Double): Double {
        return position.legs.sumOf { leg ->
            // For a long position (quantity > 0), the cost is the premium paid.
            // For a short position (quantity < 0), the credit is the premium received.
            val costOrCredit = leg.option.lastTradedPrice * leg.quantity

            val payoffAtExpiry = when (leg.option.type) {
                "CE" -> max(0.0, price - leg.option.strikePrice)
                "PE" -> max(0.0, leg.option.strikePrice - price)
                else -> 0.0
            }

            (payoffAtExpiry * leg.quantity) - costOrCredit
        }
    }
}
