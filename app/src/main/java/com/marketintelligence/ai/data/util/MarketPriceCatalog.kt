package com.marketintelligence.ai.data.util

import com.marketintelligence.ai.domain.repository.MarketRepository

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
            clean in listOf("NIFTY", "NIFTY50") -> 23600.0
            clean in listOf("BANKNIFTY", "NIFTYBANK") -> 50400.0
            clean in listOf("FINNIFTY") -> 21500.0
            clean in listOf("SENSEX") -> 77200.0
            clean in listOf("MIDCPNIFTY") -> 12250.0
            clean in listOf("NIFTYNEXT50") -> 68500.0
            clean in listOf("NIFTYIT") -> 35600.0

            // Top Indian Equities
            clean in listOf("RELIANCE") -> 1300.0
            clean in listOf("HDFCBANK", "HDFC") -> 1720.0
            clean in listOf("TCS") -> 4100.0
            clean in listOf("ICICI", "ICICIBANK") -> 1120.0
            clean in listOf("SBIN", "SBI") -> 830.0
            clean in listOf("INFY", "INFOSYS") -> 1850.0
            clean in listOf("BHARTIARTL", "AIRTEL") -> 1410.0
            clean in listOf("TATAMOTORS", "TATAMTR") -> 980.0
            clean in listOf("ITC") -> 435.0
            clean in listOf("LT", "LARSEN") -> 3540.0
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

    fun getMarketType(symbol: String): com.marketintelligence.ai.domain.model.MarketType {
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
                com.marketintelligence.ai.domain.model.MarketType.US
            clean in listOf("DFMGI", "ADX", "ADI", "EMAAR", "DEWA", "FAB", "ALDAR", "EMIRATESNBD") ->
                com.marketintelligence.ai.domain.model.MarketType.UAE
            else ->
                com.marketintelligence.ai.domain.model.MarketType.IN
        }
    }

    fun getAllKnownAssets(): List<com.marketintelligence.ai.domain.model.SearchResult> = listOf(
        // India Indices & Stocks
        com.marketintelligence.ai.domain.model.SearchResult("NIFTY 50", "NIFTY 50 Index", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("BANKNIFTY", "Nifty Bank Index", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("FINNIFTY", "Nifty Financial Services", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("SENSEX", "BSE SENSEX", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("MIDCPNIFTY", "NIFTY Midcap 50", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("RELIANCE.NS", "Reliance Industries Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("HDFCBANK.NS", "HDFC Bank Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("TCS.NS", "Tata Consultancy Services", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("ICICIBANK.NS", "ICICI Bank Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("SBIN.NS", "State Bank of India", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("INFY.NS", "Infosys Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("BHARTIARTL.NS", "Bharti Airtel Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("TATAMOTORS.NS", "Tata Motors Ltd", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("ITC.NS", "ITC Limited", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("LT.NS", "Larsen & Toubro Ltd", "STOCK"),

        // US Indices & Stocks
        com.marketintelligence.ai.domain.model.SearchResult("SPX", "S&P 500 Index", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("NDX", "NASDAQ 100", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("DJI", "Dow Jones Industrial", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("AAPL", "Apple Inc.", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("NVDA", "NVIDIA Corporation", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("MSFT", "Microsoft Corporation", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("GOOGL", "Alphabet Inc.", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("AMZN", "Amazon.com Inc.", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("TSLA", "Tesla Inc.", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("META", "Meta Platforms Inc.", "STOCK"),

        // UAE Indices & Stocks
        com.marketintelligence.ai.domain.model.SearchResult("DFMGI", "DFM General Index", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("ADX", "Abu Dhabi Securities Exchange", "INDEX"),
        com.marketintelligence.ai.domain.model.SearchResult("EMAAR", "Emaar Properties PJSC", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("DEWA", "Dubai Electricity & Water", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("FAB", "First Abu Dhabi Bank", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("ALDAR", "Aldar Properties", "STOCK"),
        com.marketintelligence.ai.domain.model.SearchResult("EMIRATESNBD", "Emirates NBD Bank", "STOCK"),

        // Cryptocurrencies
        com.marketintelligence.ai.domain.model.SearchResult("BTC", "Bitcoin", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("ETH", "Ethereum", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("SOL", "Solana", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("DOGE", "Dogecoin", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("BNB", "Binance Coin", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("SHIB", "Shiba Inu", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("XRP", "XRP Ripple", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("ADA", "Cardano", "CRYPTO"),
        com.marketintelligence.ai.domain.model.SearchResult("AVAX", "Avalanche", "CRYPTO")
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
        } catch (_: Exception) {}

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
        } catch (_: Exception) {}

        // 3. Fallback to catalog
        return getFallbackPrice(symbol)
    }
}
