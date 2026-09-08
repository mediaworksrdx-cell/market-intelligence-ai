package com.example.marketintelligence.domain.usecase

import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.StockQuote
import com.example.marketintelligence.domain.model.Holding
import com.example.marketintelligence.data.source.local.TransactionEntity
import javax.inject.Inject

class CalculatePortfolioUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionEntity>,
        quotes: Map<String, StockQuote>
    ): List<Holding> {
        val holdingsMap = mutableMapOf<String, Holding>()
        transactions.groupBy { it.symbol }.forEach { (symbol, txs) ->
            val quantity = txs.sumOf { if (it.type == "BUY") it.quantity else -it.quantity }
            if (quantity > 0) {
                // Calculate Average Price based on Buy history
                val totalBuyAmount = txs.filter { it.type == "BUY" }.sumOf { it.price * it.quantity }
                val totalBuyQty = txs.filter { it.type == "BUY" }.sumOf { it.quantity }
                val avgPrice = if (totalBuyQty > 0.0) totalBuyAmount / totalBuyQty else 0.0
                
                // Invested value of CURRENT holding (Cost Basis)
                val currentInvestedValue = quantity * avgPrice
                
                val upper = symbol.uppercase()
                val quote = quotes[symbol] 
                    ?: quotes[upper] 
                    ?: quotes[upper.removeSuffix(".NS").removeSuffix(".BO")]
                    ?: quotes["$upper.NS"]

                val currentPrice = quote?.price ?: avgPrice
                val currentValue = currentPrice * quantity
                val totalPnl = currentValue - currentInvestedValue
                val todayPnl = (quote?.change ?: 0.0) * quantity
                
                val market = when {
                    symbol.endsWith(".NS") || symbol.endsWith(".BO") || symbol.contains("NIFTY") || symbol.contains("SENSEX") -> MarketType.IN
                    symbol.contains("DFM") || symbol.contains("ADX") -> MarketType.UAE
                    else -> MarketType.US
                }
                
                holdingsMap[symbol] = Holding(
                    symbol = symbol,
                    quantity = quantity.toDouble(), // Ensure Double type
                    avgPrice = avgPrice,
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
