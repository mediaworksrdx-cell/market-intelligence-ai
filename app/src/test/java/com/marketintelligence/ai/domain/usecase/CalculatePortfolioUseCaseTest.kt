package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.data.source.local.TransactionEntity
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.model.StockQuote
import org.junit.Assert.*
import org.junit.Test

class CalculatePortfolioUseCaseTest {

    private val useCase = CalculatePortfolioUseCase()

    @Test
    fun testPortfolioWACAccountingWithBuyAndSell() {
        val txs = listOf(
            // Buy 100 @ 10 on day 1
            TransactionEntity(id = 1, symbol = "TATAMOTORS.NS", quantity = 100.0, price = 10.0, type = "BUY", timestamp = 1000L),
            // Sell 100 @ 20 on day 2
            TransactionEntity(id = 2, symbol = "TATAMOTORS.NS", quantity = 100.0, price = 20.0, type = "SELL", timestamp = 2000L),
            // Buy 10 @ 100 on day 3
            TransactionEntity(id = 3, symbol = "TATAMOTORS.NS", quantity = 10.0, price = 100.0, type = "BUY", timestamp = 3000L)
        )

        val quotes = mapOf(
            "TATAMOTORS.NS" to StockQuote("TATAMOTORS.NS", 110.0, 10.0, 10.0)
        )

        val holdings = useCase(txs, quotes)
        assertEquals(1, holdings.size)

        val holding = holdings.first()
        assertEquals("TATAMOTORS.NS", holding.symbol)
        assertEquals(10.0, holding.quantity, 0.001)
        assertEquals(100.0, holding.avgPrice, 0.001) // Correct average price after full liquidation and re-buy
        assertEquals(1000.0, holding.investedValue, 0.001)
        assertEquals(1100.0, holding.currentValue, 0.001)
        assertEquals(100.0, holding.totalPnl, 0.001)
        assertEquals(MarketType.IN, holding.market)
    }

    @Test
    fun testHoldingFallbackWhenQuoteMissing() {
        val txs = listOf(
            TransactionEntity(id = 1, symbol = "AAPL", quantity = 5.0, price = 150.0, type = "BUY", timestamp = 1000L)
        )

        // Empty quotes map
        val holdings = useCase(txs, emptyMap())
        assertEquals(1, holdings.size)

        val holding = holdings.first()
        assertEquals("AAPL", holding.symbol)
        assertEquals(5.0, holding.quantity, 0.001)
        assertEquals(150.0, holding.avgPrice, 0.001)
        assertEquals(750.0, holding.investedValue, 0.001)
        assertEquals(750.0, holding.currentValue, 0.001) // Fallback gracefully to invested value
        assertEquals(0.0, holding.totalPnl, 0.001)
    }
}
