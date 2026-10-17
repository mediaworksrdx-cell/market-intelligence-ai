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
        assertEquals("INDIAN_STOCK", holding.assetCategory)
        assertEquals("INR", holding.currency)
        assertEquals("₹", holding.currencySymbol)
        assertEquals(1.0, holding.fxRateToInr, 0.001)
        assertEquals(1100.0, holding.convertedCurrentValue, 0.001)
    }

    @Test
    fun testCryptoClassificationCurrencyAndFxConversion() {
        val txs = listOf(
            TransactionEntity(id = 1, symbol = "BTC", quantity = 0.5, price = 60000.0, type = "BUY", timestamp = 1000L)
        )

        val quotes = mapOf(
            "BTC" to StockQuote("BTC", 70000.0, 2000.0, 2.94)
        )

        val holdings = useCase(txs, quotes, usdToInrRate = 83.45)
        assertEquals(1, holdings.size)

        val holding = holdings.first()
        assertEquals("BTC", holding.symbol)
        assertEquals(0.5, holding.quantity, 0.0001)
        assertEquals(60000.0, holding.avgPrice, 0.01)
        assertEquals(30000.0, holding.investedValue, 0.01) // 0.5 * 60,000 = $30,000 USD
        assertEquals(35000.0, holding.currentValue, 0.01)  // 0.5 * 70,000 = $35,000 USD
        assertEquals(5000.0, holding.totalPnl, 0.01)       // +$5,000 USD
        assertEquals("CRYPTO", holding.assetCategory)
        assertEquals("USD", holding.currency)
        assertEquals("$", holding.currencySymbol)
        assertEquals(83.45, holding.fxRateToInr, 0.001)
        // Converted to INR: $35,000 * 83.45 = ₹2,920,750 INR
        assertEquals(2920750.0, holding.convertedCurrentValue, 0.01)
        assertEquals(2503500.0, holding.convertedInvestedValue, 0.01)
        assertEquals(417250.0, holding.convertedTotalPnl, 0.01)
    }

    @Test
    fun testIndianAndCryptoSeparationInSinglePortfolio() {
        val txs = listOf(
            TransactionEntity(id = 1, symbol = "RELIANCE.NS", quantity = 10.0, price = 2500.0, type = "BUY", timestamp = 1000L),
            TransactionEntity(id = 2, symbol = "ETH", quantity = 2.0, price = 2500.0, type = "BUY", timestamp = 1000L)
        )

        val quotes = mapOf(
            "RELIANCE.NS" to StockQuote("RELIANCE.NS", 2600.0, 100.0, 4.0),
            "ETH" to StockQuote("ETH", 3000.0, 500.0, 20.0)
        )

        val holdings = useCase(txs, quotes, usdToInrRate = 83.50)
        assertEquals(2, holdings.size)

        val relHolding = holdings.find { it.symbol == "RELIANCE.NS" }!!
        assertEquals("INR", relHolding.currency)
        assertEquals("₹", relHolding.currencySymbol)
        assertEquals(26000.0, relHolding.currentValue, 0.01) // ₹26,000
        assertEquals(1.0, relHolding.fxRateToInr, 0.001)
        assertEquals(26000.0, relHolding.convertedCurrentValue, 0.01)

        val ethHolding = holdings.find { it.symbol == "ETH" }!!
        assertEquals("USD", ethHolding.currency)
        assertEquals("$", ethHolding.currencySymbol)
        assertEquals(6000.0, ethHolding.currentValue, 0.01) // $6,000
        assertEquals(83.50, ethHolding.fxRateToInr, 0.001)
        assertEquals(501000.0, ethHolding.convertedCurrentValue, 0.01) // $6,000 * 83.50 = ₹501,000
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
        assertEquals("US_STOCK", holding.assetCategory)
        assertEquals("USD", holding.currency)
        assertEquals("$", holding.currencySymbol)
    }
}
