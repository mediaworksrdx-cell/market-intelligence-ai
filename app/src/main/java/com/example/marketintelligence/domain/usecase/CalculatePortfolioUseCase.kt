package com.example.marketintelligence.domain.usecase

import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.StockQuote
import com.example.marketintelligence.data.source.local.TransactionEntity
import com.example.marketintelligence.domain.model.Holding
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
                val invested = txs.filter { it.type == "BUY" }.sumOf { it.price * it.quantity }
                val buyQuantity = txs.filter { it.type == "BUY" }.sumOf { it.quantity }
                val avgPrice = if (buyQuantity > 0.0) invested / buyQuantity else 0.0
                
                quotes[symbol]?.let { quote ->
                    val currentValue = quote.price * quantity
                    val totalPnl = currentValue - invested
                    val todayPnl = (quote.change) * quantity
                    
                    val market = when {
                        symbol.endsWith(".NS") -> MarketType.IN
                        else -> MarketType.US
                    }
                    
                    holdingsMap[symbol] = Holding(symbol, quantity, avgPrice, invested, currentValue, totalPnl, todayPnl, market)
                }
            }
        }
        return holdingsMap.values.toList()
    }
}
