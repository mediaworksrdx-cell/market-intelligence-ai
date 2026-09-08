package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.model.StockQuote
import com.marketintelligence.ai.domain.model.Holding
import com.marketintelligence.ai.data.source.local.TransactionEntity
import javax.inject.Inject

class CalculatePortfolioUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionEntity>,
        quotes: Map<String, StockQuote>
    ): List<Holding> {
        val holdingsMap = mutableMapOf<String, Holding>()
        
        // Group and sort transactions chronologically
        transactions.groupBy { it.symbol }.forEach { (symbol, rawTxs) ->
            val sortedTxs = rawTxs.sortedBy { it.timestamp }
            
            var currentQty = 0.0
            var currentAvgPrice = 0.0

            for (tx in sortedTxs) {
                if (tx.type.equals("BUY", ignoreCase = true)) {
                    val newQty = currentQty + tx.quantity
                    if (newQty > 0) {
                        currentAvgPrice = (currentQty * currentAvgPrice + tx.quantity * tx.price) / newQty
                    }
                    currentQty = newQty
                } else if (tx.type.equals("SELL", ignoreCase = true)) {
                    currentQty = (currentQty - tx.quantity).coerceAtLeast(0.0)
                    if (currentQty == 0.0) {
                        currentAvgPrice = 0.0
                    }
                }
            }

            if (currentQty > 0) {
                val upper = symbol.uppercase()
                val quote = quotes[symbol] 
                    ?: quotes[upper] 
                    ?: quotes[upper.removeSuffix(".NS").removeSuffix(".BO")]
                    ?: quotes["$upper.NS"]
                val currentPrice = quote?.price ?: currentAvgPrice
                val currentValue = currentPrice * currentQty
                val currentInvestedValue = currentAvgPrice * currentQty
                val totalPnl = currentValue - currentInvestedValue
                val todayPnl = (quote?.change ?: 0.0) * currentQty
                
                val market = when {
                    symbol.endsWith(".NS") || symbol.endsWith(".BO") || symbol.contains("NIFTY") || symbol.contains("SENSEX") -> MarketType.IN
                    symbol.contains("DFM") || symbol.contains("ADX") -> MarketType.UAE
                    else -> MarketType.US
                }
                
                holdingsMap[symbol] = Holding(
                    symbol = symbol,
                    quantity = currentQty,
                    avgPrice = currentAvgPrice,
                    investedValue = currentInvestedValue,
                    currentValue = currentValue,
                    totalPnl = totalPnl,
                    todayPnl = todayPnl,
                    market = market
                )
            }
        }
        return holdingsMap.values.toList()
    }
}
