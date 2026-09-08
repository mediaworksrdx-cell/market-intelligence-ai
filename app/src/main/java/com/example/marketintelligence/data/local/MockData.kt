package com.example.marketintelligence.data.local

import com.example.marketintelligence.domain.model.*

object MockData {

    val INDICES_IN = listOf(
        IndexData("NIFTY 50", "NIFTY 50", 22500.0, 22350.0, 150.0, 0.67, MarketType.IN, 256265L),
        IndexData("BANKNIFTY", "BANK NIFTY", 48000.0, 47500.0, 500.0, 1.04, MarketType.IN, 260105L),
        IndexData("FINNIFTY", "FIN NIFTY", 21500.0, 21400.0, 100.0, 0.47, MarketType.IN, 257801L),
        IndexData("SENSEX", "SENSEX", 74000.0, 73550.0, 450.0, 0.61, MarketType.IN, 265L),
        IndexData("MIDCPNIFTY", "NIFTY MIDCAP 50", 12250.0, 12150.0, 100.0, 0.85, MarketType.IN, 257033L),
        IndexData("NIFTY NEXT 50", "NIFTY NEXT 50", 68500.0, 68150.0, 350.0, 0.52, MarketType.IN, 256521L),
        IndexData("NIFTY IT", "NIFTY IT SECTOR", 35600.0, 35720.0, -120.0, -0.35, MarketType.IN, 257289L)
    )

    val WATCHLIST_INITIAL = listOf(
        StockData("RELIANCE.NS", "Reliance Industries", 2950.0, 2920.0, 30.0, 1.0, "5M", MarketType.IN, 738561L),
        StockData("HDFCBANK.NS", "HDFC Bank", 1550.0, 1560.0, -10.0, -0.6, "8M", MarketType.IN, 341249L),
        StockData("INFY.NS", "Infosys", 1450.0, 1435.0, 15.0, 1.0, "4M", MarketType.IN, 408065L),
        StockData("TCS.NS", "Tata Consultancy", 4000.0, 3980.0, 20.0, 0.5, "3M", MarketType.IN, 2953213L),
        StockData("ICICIBANK.NS", "ICICI Bank", 1120.0, 1105.0, 15.0, 1.35, "6M", MarketType.IN, 1270529L),
        StockData("BHARTIARTL.NS", "Bharti Airtel", 1410.0, 1404.0, 6.0, 0.43, "3M", MarketType.IN, 2714625L),
        StockData("TATAMOTORS.NS", "Tata Motors", 980.0, 983.0, -3.0, -0.31, "7M", MarketType.IN, 884737L),
        StockData("ITC.NS", "ITC Limited", 435.0, 434.0, 1.0, 0.23, "9M", MarketType.IN, 424961L),
        StockData("SBIN.NS", "State Bank of India", 825.0, 818.0, 7.0, 0.86, "8M", MarketType.IN, 779521L),
        StockData("LT.NS", "Larsen & Toubro", 3540.0, 3510.0, 30.0, 0.85, "2M", MarketType.IN, 2939649L)
    )

    val CRYPTO_INITIAL = listOf(
        CryptoData(id = "bitcoin", symbol = "BTC", name = "Bitcoin", price = 67450.0, changePercent = 2.14, marketCap = 1_320_000_000_000L),
        CryptoData(id = "ethereum", symbol = "ETH", name = "Ethereum", price = 3520.0, changePercent = 1.62, marketCap = 422_000_000_000L),
        CryptoData(id = "dogecoin", symbol = "DOGE", name = "Dogecoin", price = 0.125, changePercent = 3.85, marketCap = 18_200_000_000L),
        CryptoData(id = "solana", symbol = "SOL", name = "Solana", price = 148.50, changePercent = 4.21, marketCap = 68_500_000_000L),
        CryptoData(id = "binancecoin", symbol = "BNB", name = "BNB", price = 585.0, changePercent = -0.45, marketCap = 87_000_000_000L),
        CryptoData(id = "shiba-inu", symbol = "SHIB", name = "Shiba Inu", price = 0.0000185, changePercent = 2.90, marketCap = 10_900_000_000L),
        CryptoData(id = "cardano", symbol = "ADA", name = "Cardano", price = 0.48, changePercent = 1.15, marketCap = 17_100_000_000L),
        CryptoData(id = "ripple", symbol = "XRP", name = "XRP", price = 0.52, changePercent = -0.80, marketCap = 29_000_000_000L),
        CryptoData(id = "avalanche-2", symbol = "AVAX", name = "Avalanche", price = 32.40, changePercent = 2.30, marketCap = 12_800_000_000L)
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
