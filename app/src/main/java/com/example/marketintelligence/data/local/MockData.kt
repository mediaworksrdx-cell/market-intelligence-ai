package com.example.marketintelligence.data.local

import com.example.marketintelligence.domain.model.*

object MockData {

    val INDICES_IN = listOf(
        IndexData("NIFTY 50", "NIFTY 50", 22716.20, 22780.25, -64.05, -0.28, MarketType.IN, 256265L),
        IndexData("BANKNIFTY", "BANK NIFTY", 54259.95, 54471.65, -211.70, -0.39, MarketType.IN, 260105L),
        IndexData("FINNIFTY", "FIN NIFTY", 24648.50, 24671.65, -23.15, -0.09, MarketType.IN, 257801L),
        IndexData("SENSEX", "SENSEX", 72529.07, 72771.72, -242.65, -0.33, MarketType.IN, 265L)
    )

    val WATCHLIST_INITIAL = listOf(
        StockData("RELIANCE.NS", "Reliance Industries", 1182.00, 1197.60, -15.60, -1.30, "24M", MarketType.IN, 738561L),
        StockData("HDFCBANK.NS", "HDFC Bank", 722.70, 719.05, 3.65, 0.51, "42M", MarketType.IN, 341249L),
        StockData("TCS.NS", "Tata Consultancy", 2032.40, 2070.70, -38.30, -1.85, "3M", MarketType.IN, 2953217L),
        StockData("ICICIBANK.NS", "ICICI Bank", 1292.20, 1302.00, -9.80, -0.75, "6M", MarketType.IN, 1270529L),
        StockData("SBIN.NS", "State Bank of India", 964.70, 962.00, 2.70, 0.28, "8M", MarketType.IN, 779521L),
        StockData("INFY.NS", "Infosys Ltd", 1015.40, 1003.20, 12.20, 1.22, "14M", MarketType.IN, 408065L)
    )

    val INDICES_US = listOf(
        IndexData("SPX", "S&P 500 Index", 5500.0, 5470.0, 30.0, 0.55, MarketType.US, null),
        IndexData("NDX", "NASDAQ 100", 19200.0, 19050.0, 150.0, 0.79, MarketType.US, null),
        IndexData("DJI", "Dow Jones Industrial", 39500.0, 39350.0, 150.0, 0.38, MarketType.US, null)
    )

    val WATCHLIST_US = listOf(
        StockData("AAPL", "Apple Inc.", 225.0, 222.5, 2.5, 1.12, "45M", MarketType.US, null),
        StockData("NVDA", "NVIDIA Corporation", 118.0, 115.0, 3.0, 2.61, "60M", MarketType.US, null),
        StockData("MSFT", "Microsoft Corporation", 440.0, 436.0, 4.0, 0.92, "20M", MarketType.US, null),
        StockData("GOOGL", "Alphabet Inc.", 175.0, 173.0, 2.0, 1.16, "25M", MarketType.US, null),
        StockData("AMZN", "Amazon.com Inc.", 185.0, 183.0, 2.0, 1.09, "30M", MarketType.US, null),
        StockData("TSLA", "Tesla Inc.", 210.0, 204.0, 6.0, 2.94, "50M", MarketType.US, null),
        StockData("META", "Meta Platforms Inc.", 500.0, 492.0, 8.0, 1.63, "15M", MarketType.US, null)
    )

    val INDICES_UAE = listOf(
        IndexData("DFMGI", "DFM General Index", 4850.0, 4820.0, 30.0, 0.62, MarketType.UAE, null),
        IndexData("ADX", "Abu Dhabi Securities Exchange", 9250.0, 9200.0, 50.0, 0.54, MarketType.UAE, null)
    )

    val WATCHLIST_UAE = listOf(
        StockData("EMAAR", "Emaar Properties PJSC", 8.45, 8.35, 0.10, 1.20, "12M", MarketType.UAE, null),
        StockData("DEWA", "Dubai Electricity & Water", 2.48, 2.46, 0.02, 0.81, "8M", MarketType.UAE, null),
        StockData("FAB", "First Abu Dhabi Bank", 13.10, 13.00, 0.10, 0.77, "5M", MarketType.UAE, null),
        StockData("ALDAR", "Aldar Properties", 5.60, 5.52, 0.08, 1.45, "6M", MarketType.UAE, null),
        StockData("EMIRATESNBD", "Emirates NBD Bank", 17.80, 17.65, 0.15, 0.85, "3M", MarketType.UAE, null)
    )

    val CRYPTO_INITIAL = listOf(
        CryptoData(id = "bitcoin", symbol = "BTC", name = "Bitcoin", price = 78800.0, changePercent = -0.55, marketCap = 1_580_000_000_000L),
        CryptoData(id = "ethereum", symbol = "ETH", name = "Ethereum", price = 2495.0, changePercent = -0.15, marketCap = 304_000_000_000L),
        CryptoData(id = "solana", symbol = "SOL", name = "Solana", price = 103.50, changePercent = -0.40, marketCap = 60_500_000_000L),
        CryptoData(id = "binancecoin", symbol = "BNB", name = "BNB", price = 754.0, changePercent = 1.65, marketCap = 100_000_000_000L),
        CryptoData(id = "dogecoin", symbol = "DOGE", name = "Dogecoin", price = 0.090, changePercent = -0.85, marketCap = 14_000_000_000L),
        CryptoData(id = "ripple", symbol = "XRP", name = "XRP", price = 0.55, changePercent = 1.10, marketCap = 31_000_000_000L)
    )

    val PORTFOLIO_INITIAL = listOf(
        PortfolioItem("RELIANCE.NS", "Reliance Industries", 10.0, 2900.0, 2950.0, MarketType.IN, AssetType.STOCK, 85, "BULLISH"),
        PortfolioItem("TCS.NS", "Tata Consultancy", 5.0, 3980.0, 4000.0, MarketType.IN, AssetType.STOCK, 70, "NEUTRAL")
    )

    val MOCK_AI_RESULT = AIAnalysisResult(
        symbol = "BANKNIFTY",
        signal = SignalType.BULLISH,
        confidence = 88,
        alphaScore = 9,
        rrRatio = "1:3.5",
        pattern = "Bullish Order Block",
        patternComplexity = "INSTITUTIONAL",
        timeframe = "1H",
        entryZone = "47800-47850",
        liquidityZone = "48200",
        stopLoss = 47650.0,
        target = listOf("48500", "48800"),
        risk = RiskLevel.MEDIUM,
        rationale = "A significant bullish order block was formed on the 1H timeframe after sweeping liquidity. The current price is mitigating this OB, suggesting a high probability of a bullish reversal.",
        marketStructure = "BULLISH_BOS" ,
        timestamp = "12:45 PM"
    )

    val NOTIFICATIONS_MOCK = listOf(
        NotificationItem("1", NotificationType.AI_SIGNAL, "New BULLISH Signal on BANKNIFTY", "12:45 PM", Sentiment.POSITIVE, false, MOCK_AI_RESULT, eventData = null, ipoData = null),
        NotificationItem("2", NotificationType.NEWS, "Global markets rally on positive inflation data.", "11:30 AM", Sentiment.POSITIVE, false, aiResult = null, eventData = null, ipoData = null),
        NotificationItem("3", NotificationType.EVENT, "Fed Interest Rate decision at 2:30 PM.", "10:00 AM", Sentiment.NEUTRAL, true, eventData = null, ipoData = null),
        NotificationItem("4", NotificationType.IPO, "TechWave IPO closes today. GMP at 30%.", "9:15 AM", Sentiment.POSITIVE, true, aiResult = null, eventData = null, ipoData = null)
    )
}
