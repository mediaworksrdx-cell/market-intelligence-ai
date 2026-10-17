package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.model.StockQuote
import com.marketintelligence.ai.domain.model.Holding
import com.marketintelligence.ai.data.source.local.TransactionEntity
import javax.inject.Inject

class CalculatePortfolioUseCase @Inject constructor() {

    operator fun invoke(
        transactions: List<TransactionEntity>,
        quotes: Map<String, StockQuote>,
        usdToInrRate: Double = 83.45,
        aedToInrRate: Double = 22.72
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
                val cleanUpper = upper
                    .removeSuffix(".NS")
                    .removeSuffix(".BO")
                    .removeSuffix("-USD")
                    .removeSuffix("USDT")
                    .trim()

                val quote = quotes[symbol] 
                    ?: quotes[upper] 
                    ?: quotes[cleanUpper]
                    ?: quotes["$cleanUpper.NS"]
                    ?: quotes["${cleanUpper}USDT"]
                    ?: quotes["$cleanUpper-USD"]
                    ?: quotes.entries.find { it.key.equals(cleanUpper, ignoreCase = true) }?.value

                val currentPrice = quote?.price?.takeIf { it > 0.0 } ?: currentAvgPrice
                val currentValue = currentPrice * currentQty
                val currentInvestedValue = currentAvgPrice * currentQty
                val totalPnl = currentValue - currentInvestedValue
                val todayPnl = (quote?.change ?: 0.0) * currentQty
                
                // Asset Classification & Currency Identification
                val isCrypto = upper.endsWith("USDT") || upper.endsWith("-USD") ||
                    cleanUpper in setOf(
                        "BTC", "BITCOIN", "ETH", "ETHEREUM", "SOL", "SOLANA", "BNB", "BINANCECOIN",
                        "DOGE", "DOGECOIN", "SHIB", "SHIBAINU", "XRP", "RIPPLE", "ADA", "CARDANO",
                        "AVAX", "AVALANCHE", "DOT", "MATIC", "LINK", "UNI", "LTC", "NEAR", "ATOM"
                    )

                val isIndian = upper.endsWith(".NS") || upper.endsWith(".BO") ||
                    upper.contains("NIFTY") || upper.contains("SENSEX") ||
                    cleanUpper in setOf(
                        "RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "BHARTIARTL",
                        "TATAMOTORS", "TATASTEEL", "ITC", "LT", "AXISBANK", "KOTAKBANK", "MARUTI",
                        "SUNPHARMA", "TITAN", "WIPRO", "BAJFINANCE", "ADANIENT", "HINDUNILVR", "HPCL"
                    )

                val isUae = upper.contains("DFM") || upper.contains("ADX") || upper.endsWith(".AE")

                val market: MarketType
                val assetCategory: String
                val currency: String
                val currencySymbol: String
                val fxRate: Double

                when {
                    isCrypto -> {
                        market = MarketType.US
                        assetCategory = "CRYPTO"
                        currency = "USD"
                        currencySymbol = "$"
                        fxRate = usdToInrRate
                    }
                    isIndian -> {
                        market = MarketType.IN
                        assetCategory = "INDIAN_STOCK"
                        currency = "INR"
                        currencySymbol = "₹"
                        fxRate = 1.0
                    }
                    isUae -> {
                        market = MarketType.UAE
                        assetCategory = "UAE_STOCK"
                        currency = "AED"
                        currencySymbol = "AED "
                        fxRate = aedToInrRate
                    }
                    else -> {
                        market = MarketType.US
                        assetCategory = "US_STOCK"
                        currency = "USD"
                        currencySymbol = "$"
                        fxRate = usdToInrRate
                    }
                }

                val convertedCur = currentValue * fxRate
                val convertedInv = currentInvestedValue * fxRate
                val convertedTotPnl = totalPnl * fxRate
                val convertedTodPnl = todayPnl * fxRate

                holdingsMap[symbol] = Holding(
                    symbol = symbol,
                    quantity = currentQty,
                    avgPrice = currentAvgPrice,
                    investedValue = currentInvestedValue,
                    currentValue = currentValue,
                    totalPnl = totalPnl,
                    todayPnl = todayPnl,
                    market = market,
                    assetCategory = assetCategory,
                    currency = currency,
                    currencySymbol = currencySymbol,
                    fxRateToInr = fxRate,
                    convertedCurrentValue = convertedCur,
                    convertedInvestedValue = convertedInv,
                    convertedTotalPnl = convertedTotPnl,
                    convertedTodayPnl = convertedTodPnl
                )
            }
        }
        return holdingsMap.values.toList()
    }
}
