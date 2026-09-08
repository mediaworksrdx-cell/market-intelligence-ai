package com.marketintelligence.ai.domain.engine

import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject
import javax.inject.Singleton

data class CorporateAction(
    val symbol: String,
    val exDate: Long,
    val splitRatio: Double = 1.0,
    val dividendAmount: Double = 0.0
)

@Singleton
class CorporateActionAdjuster @Inject constructor() {

    fun adjustCandles(
        candles: List<Candle>,
        actions: List<CorporateAction>
    ): List<Candle> {
        if (candles.isEmpty() || actions.isEmpty()) return candles

        val sortedActions = actions.sortedByDescending { it.exDate }
        val adjusted = ArrayList<Candle>(candles.size)

        for (candle in candles) {
            var cumulativeSplit = 1.0
            var cumulativeDividend = 0.0

            for (action in sortedActions) {
                if (candle.openTime < action.exDate) {
                    if (action.splitRatio > 0.0) {
                        cumulativeSplit *= action.splitRatio
                    }
                    cumulativeDividend += action.dividendAmount
                }
            }

            if (cumulativeSplit != 1.0 || cumulativeDividend > 0.0) {
                adjusted.add(
                    candle.copy(
                        open = ((candle.open / cumulativeSplit) - cumulativeDividend).coerceAtLeast(0.01),
                        high = ((candle.high / cumulativeSplit) - cumulativeDividend).coerceAtLeast(0.01),
                        low = ((candle.low / cumulativeSplit) - cumulativeDividend).coerceAtLeast(0.01),
                        close = ((candle.close / cumulativeSplit) - cumulativeDividend).coerceAtLeast(0.01),
                        volume = candle.volume * cumulativeSplit
                    )
                )
            } else {
                adjusted.add(candle)
            }
        }
        return adjusted
    }
}
