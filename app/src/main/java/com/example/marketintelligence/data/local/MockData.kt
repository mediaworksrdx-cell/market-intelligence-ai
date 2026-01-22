package com.example.marketintelligence.data.local

import com.example.marketintelligence.domain.model.*

object MockData {

    val INDICES_IN = listOf(
        IndexData("NIFTY 50", "NIFTY 50", 22500.0, 150.0, 0.67, MarketType.IN),
        IndexData("BANKNIFTY", "BANK NIFTY", 48000.0, 500.0, 1.04, MarketType.IN),
        IndexData("FINNIFTY", "FIN NIFTY", 21500.0, 100.0, 0.47, MarketType.IN),
        IndexData("SENSEX", "SENSEX", 74000.0, 450.0, 0.61, MarketType.IN)
    )

    val WATCHLIST_INITIAL = listOf(
        StockData("RELIANCE.NS", "Reliance Industries", 2950.0, 30.0, 1.0, "5M", MarketType.IN),
        StockData("HDFCBANK.NS", "HDFC Bank", 1550.0, -10.0, -0.6, "8M", MarketType.IN),
        StockData("INFY.NS", "Infosys", 1450.0, 15.0, 1.0, "4M", MarketType.IN),
        StockData("TCS.NS", "Tata Consultancy", 4000.0, 20.0, 0.5, "3M", MarketType.IN)
    )

    val PORTFOLIO_INITIAL = listOf(
        PortfolioItem("RELIANCE.NS", "Reliance Industries", 10.0, 2800.0, 2950.0, MarketType.IN, AssetType.STOCK, 85, "Strong Buy"),
        PortfolioItem("BTC-USD", "Bitcoin", 0.1, 60000.0, 65000.0, MarketType.US, AssetType.CRYPTO, 90, "Bullish")
    )
    
    val TRAINING_MODULES = listOf(
        TrainingModule("1", "SMC Basics", "45m", false, listOf("Order Blocks", "FVG")),
        TrainingModule("2", "Advanced Patterns", "1h 30m", false, listOf("Wyckoff", "Eliott Wave")),
        TrainingModule("3", "Risk Management", "1h", true, listOf("Position Sizing", "Hedging"))
    )
    
    val ECONOMIC_EVENTS = listOf(
        EconomicEvent("1", "14:30", "USD", "Fed Interest Rate Decision", "HIGH", "5.50%", "5.50%", "5.50%"),
        EconomicEvent("2", "18:00", "INR", "RBI Monetary Policy", "HIGH", "6.50%", "6.50%", "6.50%"),
        EconomicEvent("3", "19:00", "USD", "Non-Farm Payroll", "HIGH", "275K", "198K", "229K"),
        EconomicEvent("4", "20:00", "EUR", "ECB Main Refinancing Rate", "MEDIUM", "4.50%", "4.50%", "4.50%")
    )

    val IPO_DATA = listOf(
        IPOData("TCHW", "TechWave Inc.", "Mar 10", "Mar 14", "250-260", 80.0, 30.0, "OPEN"),
        IPOData("GRNF", "GreenFuture Energy", "Mar 15", "Mar 19", "400-420", 120.0, 28.0, "UPCOMING"),
        IPOData("HLTH", "HealthSphere AI", "Mar 1", "Mar 5", "300-310", 0.0, 0.0, "CLOSED")
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
        NotificationItem("3", NotificationType.EVENT, "Fed Interest Rate decision at 2:30 PM.", "10:00 AM", Sentiment.NEUTRAL, true, eventData = ECONOMIC_EVENTS[0]),
        NotificationItem("4", NotificationType.IPO, "TechWave IPO closes today. GMP at 30%.", "9:15 AM", Sentiment.POSITIVE, true, ipoData = IPO_DATA[0])
    )
    
    val MOCK_FO_CONTRACTS = listOf(
        FutureContract("28MAR24", 22550.0, 0.68, "+5.2K", "2.1M", 22540.0, 10.0),
        FutureContract("25APR24", 22680.0, 0.70, "+3.1K", "1.5M", 22670.0, 20.0),
        FutureContract("30MAY24", 22800.0, 0.72, "+1.5K", "1.1M", 22790.0, 30.0)
    )

    val MOCK_FO_DATA_IN = FOSymbolData(
        symbol = "NIFTY 50",
        exchange = "NSE",
        price = 22500.0,
        changePercent = 0.67,
        pcr = 0.95,
        pcrSignal = "Neutral to Bullish",
        maxPain = 22400.0,
        iv = 14.5,
        ivRank = 45.0,
        ivPercentile = 60.0,
        trend = "Bullish",
        bias = "Positive",
        buildup = "Long Buildup",
        lotSize = 50,
        contracts = MOCK_FO_CONTRACTS
    )
    
    val STRATEGIES = listOf(
        OptionStrategy("1", "Bull Call Spread", "Moderately Bullish", 5000.0, 2000.0, 22600.0, 65.0, 25.0, listOf("Buy 22500 CE", "Sell 22700 CE"), listOf(PayoffPoint(22000.0, -1000.0), PayoffPoint(23000.0, 5000.0))),
        OptionStrategy("2", "Iron Condor", "Neutral", 3000.0, 7000.0, 22500.0, 75.0, 15.0, listOf("Sell 22500 CE", "Sell 22500 PE"), listOf(PayoffPoint(22000.0, -2000.0), PayoffPoint(23000.0, -2000.0))),
        OptionStrategy("3", "Long Straddle", "High Volatility", 10000.0, 4000.0, 22300.0, 40.0, 50.0, listOf("Buy 22500 CE", "Buy 22500 PE"), listOf(PayoffPoint(22000.0, 1000.0), PayoffPoint(23000.0, 1000.0)))
    )
}
