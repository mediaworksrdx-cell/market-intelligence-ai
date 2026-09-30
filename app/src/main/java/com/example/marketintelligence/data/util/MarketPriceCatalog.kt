package com.example.marketintelligence.data.util

import android.util.Log
import com.example.marketintelligence.domain.repository.MarketRepository

object MarketPriceCatalog {

    fun getFallbackPrice(symbol: String): Double {
        val clean = symbol.uppercase()
            .removeSuffix(".NS")
            .removeSuffix(".BO")
            .removeSuffix("USDT")
            .removeSuffix("-USD")
            .replace("-", "")
            .replace(" ", "")
            .trim()

        return when {
            // Cryptocurrencies
            clean in listOf("BTC", "BITCOIN") -> 78800.0
            clean in listOf("ETH", "ETHEREUM") -> 2495.0
            clean in listOf("SOL", "SOLANA") -> 103.5
            clean in listOf("BNB", "BINANCECOIN") -> 755.0
            clean in listOf("DOGE", "DOGECOIN") -> 0.090
            clean in listOf("SHIB", "SHIBAINU") -> 0.0000185
            clean in listOf("XRP", "RIPPLE") -> 0.55
            clean in listOf("ADA", "CARDANO") -> 0.48
            clean in listOf("AVAX", "AVALANCHE") -> 32.40

            // Indian Indices
            clean in listOf("NIFTY", "NIFTY50") -> 22716.20
            clean in listOf("BANKNIFTY", "NIFTYBANK") -> 54259.95
            clean in listOf("FINNIFTY") -> 24648.50
            clean in listOf("SENSEX") -> 72529.07
            clean in listOf("MIDCPNIFTY") -> 12250.0
            clean in listOf("NIFTYNEXT50") -> 68500.0
            clean in listOf("NIFTYIT") -> 35600.0

            // Top Indian Equities
            clean in listOf("RELIANCE") -> 1182.00
            clean in listOf("HDFCBANK", "HDFC") -> 722.70
            clean in listOf("TCS") -> 2032.40
            clean in listOf("ICICI", "ICICIBANK") -> 1292.20
            clean in listOf("SBIN", "SBI") -> 964.70
            clean in listOf("INFY", "INFOSYS") -> 1015.40
            clean in listOf("BHARTIARTL", "AIRTEL") -> 1771.20
            clean in listOf("TATAMOTORS", "TATAMTR") -> 280.95
            clean in listOf("ITC") -> 265.10
            clean in listOf("LT", "LARSEN") -> 3749.10
            clean in listOf("AXISBANK", "AXIS") -> 1180.0
            clean in listOf("KOTAKBANK", "KOTAK") -> 1780.0
            clean in listOf("MARUTI") -> 12400.0
            clean in listOf("SUNPHARMA") -> 1680.0
            clean in listOf("TITAN") -> 3450.0
            clean in listOf("WIPRO") -> 530.0
            clean in listOf("BAJFINANCE") -> 6900.0
            clean in listOf("ADANIENT") -> 2950.0
            clean in listOf("HINDUNILVR") -> 2450.0

            // US Tech & Global Indices
            clean in listOf("SPX", "SP500") -> 5500.0
            clean in listOf("NDX", "NASDAQ") -> 19200.0
            clean in listOf("AAPL", "APPLE") -> 225.0
            clean in listOf("MSFT", "MICROSOFT") -> 440.0
            clean in listOf("GOOGL", "GOOG", "GOOGLE") -> 175.0
            clean in listOf("AMZN", "AMAZON") -> 185.0
            clean in listOf("TSLA", "TESLA") -> 210.0
            clean in listOf("NVDA", "NVIDIA") -> 118.0
            clean in listOf("META") -> 500.0

            // UAE Indices & Stocks
            clean in listOf("DFMGI") -> 4850.0
            clean in listOf("ADX", "ADI") -> 9250.0
            clean in listOf("EMAAR") -> 8.45
            clean in listOf("DEWA") -> 2.48
            clean in listOf("FAB") -> 13.10
            clean in listOf("ALDAR") -> 5.60
            clean in listOf("EMIRATESNBD") -> 17.80

            else -> 1250.0
        }
    }

    fun getMarketType(symbol: String): com.example.marketintelligence.domain.model.MarketType {
        val clean = symbol.uppercase()
            .removeSuffix(".NS")
            .removeSuffix(".BO")
            .removeSuffix("USDT")
            .removeSuffix("-USD")
            .replace("-", "")
            .replace(" ", "")
            .trim()

        return when {
            clean in listOf("SPX", "SP500", "NDX", "NASDAQ", "DJI", "DOW", "AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "TSLA", "NVDA", "META") ->
                com.example.marketintelligence.domain.model.MarketType.US
            clean in listOf("DFMGI", "ADX", "ADI", "EMAAR", "DEWA", "FAB", "ALDAR", "EMIRATESNBD") ->
                com.example.marketintelligence.domain.model.MarketType.UAE
            else ->
                com.example.marketintelligence.domain.model.MarketType.IN
        }
    }

    fun getAllKnownAssets(): List<com.example.marketintelligence.domain.model.SearchResult> = listOf(
        // India Indices & Stocks
        com.example.marketintelligence.domain.model.SearchResult("NIFTY 50", "NIFTY 50 Index", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("BANKNIFTY", "Nifty Bank Index", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("FINNIFTY", "Nifty Financial Services", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("SENSEX", "BSE SENSEX", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("MIDCPNIFTY", "NIFTY Midcap 50", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("RELIANCE.NS", "Reliance Industries Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("HDFCBANK.NS", "HDFC Bank Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("TCS.NS", "Tata Consultancy Services", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("ICICIBANK.NS", "ICICI Bank Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("SBIN.NS", "State Bank of India", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("INFY.NS", "Infosys Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("BHARTIARTL.NS", "Bharti Airtel Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("TATAMOTORS.NS", "Tata Motors Ltd", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("ITC.NS", "ITC Limited", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("LT.NS", "Larsen & Toubro Ltd", "STOCK"),

        // US Indices & Stocks
        com.example.marketintelligence.domain.model.SearchResult("SPX", "S&P 500 Index", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("NDX", "NASDAQ 100", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("DJI", "Dow Jones Industrial", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("AAPL", "Apple Inc.", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("NVDA", "NVIDIA Corporation", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("MSFT", "Microsoft Corporation", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("GOOGL", "Alphabet Inc.", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("AMZN", "Amazon.com Inc.", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("TSLA", "Tesla Inc.", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("META", "Meta Platforms Inc.", "STOCK"),

        // UAE Indices & Stocks
        com.example.marketintelligence.domain.model.SearchResult("DFMGI", "DFM General Index", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("ADX", "Abu Dhabi Securities Exchange", "INDEX"),
        com.example.marketintelligence.domain.model.SearchResult("EMAAR", "Emaar Properties PJSC", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("DEWA", "Dubai Electricity & Water", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("FAB", "First Abu Dhabi Bank", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("ALDAR", "Aldar Properties", "STOCK"),
        com.example.marketintelligence.domain.model.SearchResult("EMIRATESNBD", "Emirates NBD Bank", "STOCK"),

        // Cryptocurrencies
        com.example.marketintelligence.domain.model.SearchResult("BTC", "Bitcoin", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("ETH", "Ethereum", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("SOL", "Solana", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("DOGE", "Dogecoin", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("BNB", "Binance Coin", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("SHIB", "Shiba Inu", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("XRP", "XRP Ripple", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("ADA", "Cardano", "CRYPTO"),
        com.example.marketintelligence.domain.model.SearchResult("AVAX", "Avalanche", "CRYPTO")
    )

    suspend fun resolveBasePrice(symbol: String, marketRepository: MarketRepository): Double {
        val clean = symbol.uppercase()
            .removeSuffix(".NS")
            .removeSuffix(".BO")
            .removeSuffix("USDT")
            .removeSuffix("-USD")
            .replace("-", "")
            .replace(" ", "")
            .trim()

        // 1. Try Indian market live prices
        try {
            val livePrices = marketRepository.getLivePrices()
            val matched = livePrices.firstOrNull { lp ->
                val lpClean = lp.symbol.uppercase()
                    .removeSuffix(".NS")
                    .removeSuffix(".BO")
                    .replace("-", "")
                    .replace(" ", "")
                    .trim()
                lpClean == clean || lp.symbol.equals(symbol, ignoreCase = true)
            }
            if (matched != null && matched.ltp > 0.0) {
                return matched.ltp
            }
        } catch (e: Exception) { Log.e("MarketPriceCatalog", "Error: ${e.message}") }

        // 2. Try Crypto live prices
        try {
            val cryptos = marketRepository.getCryptoLivePrices()
            val matchedCrypto = cryptos.firstOrNull { c ->
                val cClean = c.symbol.uppercase().trim()
                cClean == clean || c.symbol.equals(symbol, ignoreCase = true)
            }
            if (matchedCrypto != null && matchedCrypto.price > 0.0) {
                return matchedCrypto.price
            }
        } catch (e: Exception) { Log.e("MarketPriceCatalog", "Error: ${e.message}") }

        // 3. Fallback to catalog
        return getFallbackPrice(symbol)
    }
}
