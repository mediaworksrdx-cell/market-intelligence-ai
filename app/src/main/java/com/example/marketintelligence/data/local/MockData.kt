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
        IndexData("DJI", "Dow Jones Industrial", 39500.0, 39350.0, 150.0, 0.38, MarketType.US, null),
        IndexData("VIX", "Volatility Index", 18.40, 18.65, -0.25, -1.25, MarketType.US, null)
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
        NotificationItem(
            id = "notif_news_1",
            type = NotificationType.NEWS,
            title = "RBI MPC Policy: Repo Rate Kept Unchanged at 6.50% Amid Robust GDP Outlook",
            time = "10:00 AM",
            sentiment = Sentiment.POSITIVE,
            read = false,
            content = "The Reserve Bank of India Monetary Policy Committee (MPC) has kept the policy repo rate unchanged at 6.50% for the 10th consecutive review, maintaining a status quo in benchmark lending rates.\n\nKey MPC Takeaways:\n• Real GDP Growth: Projected firmly at 7.2% for FY2024-25, supported by resilient investment demand and improving rural purchasing power.\n• CPI Inflation: FY25 retail inflation projection retained at 4.5%, with core inflation hovering near multi-year lows.\n• Policy Stance: The MPC remains focused on 'withdrawal of accommodation' to ensure retail inflation durably aligns with the 4% target.\n• Financial System: Banking sector balance sheets remain robust with gross NPA ratios at a multi-decade low of 2.8%.\n\nSectoral Impact:\n• Banking & Financials: Favorable margin trajectory for corporate lenders (HDFC Bank, ICICI Bank, SBI).\n• Real Estate & Auto: Consumer credit rates remain steady, supporting festive quarter booking volumes.",
            summary = "RBI keeps repo rate unchanged at 6.50%; projects FY25 GDP growth at 7.2% and inflation at 4.5%.",
            source = "Reserve Bank of India / Economic Times",
            category = "Monetary Policy",
            actionSymbol = "BANKNIFTY",
            actionType = "INDEX"
        ),
        NotificationItem(
            id = "notif_event_1",
            type = NotificationType.EVENT,
            title = "US Federal Reserve FOMC Interest Rate Decision & Monetary Outlook",
            time = "11:30 PM",
            sentiment = Sentiment.NEUTRAL,
            read = false,
            eventData = EconomicEvent(
                id = "evt_fomc_1",
                time = "02:00 PM EST (11:30 PM IST)",
                currency = "USD",
                event = "Federal Funds Target Rate",
                impact = "HIGH",
                actual = "5.00%",
                forecast = "5.00%",
                previous = "5.25%",
                description = "The Federal Open Market Committee lowered the target range for the federal funds rate by 25 bps to 4.75%-5.00%. Fed Chair Jerome Powell reaffirmed that the US economy is expanding at a solid pace, but risks to labor and inflation are moving into balance.\n\nKey Market Reaction:\n• US 10-Year Treasury Yield slipped 4 bps to 4.21%\n• DXY Dollar Index eased to 101.35\n• Gold spot surged to $2,654/oz on lower opportunity cost of capital.",
                affectedMarkets = "Global Indices, Gold (XAU/USD), DXY, Treasury Yields"
            ),
            content = "Federal Open Market Committee announces rate cut with Powell signaling measured future easing based on employment stability.",
            summary = "Fed reduces policy rate by 25 bps to 5.00%; yields soften as dollar index eases.",
            source = "Federal Reserve System",
            category = "Central Banks",
            actionSymbol = "SPX",
            actionType = "INDEX"
        ),
        NotificationItem(
            id = "notif_ipo_1",
            type = NotificationType.IPO,
            title = "TechWave Cloud Ltd IPO: Subscription Closes Today, GMP Surges to +38%",
            time = "09:15 AM",
            sentiment = Sentiment.POSITIVE,
            read = false,
            ipoData = IPOData(
                symbol = "TECHWAVE",
                name = "TechWave Cloud Solutions Ltd",
                openDate = "03 Oct 2026",
                closeDate = "07 Oct 2026 (Today)",
                priceBand = "₹475 - ₹500",
                gmp = 190.0,
                gmpPercent = 38.0,
                status = "CLOSING TODAY",
                issueSize = "₹1,850 Cr",
                lotSize = "30 Shares (₹15,000)",
                listingDate = "12 Oct 2026",
                description = "TechWave Cloud provides enterprise AI orchestration and scalable multi-cloud infrastructure to Fortune 500 enterprises across North America, Europe, and Asia.\n\nFinancial Highlights:\n• FY24 Revenue grew 46% YoY to ₹1,240 Cr\n• EBITDA margin expanded 310 bps to 22.4%\n• Net Debt-free balance sheet with ₹420 Cr cash reserves.\n\nBrokerage Verdict:\n• Institutional consensus is 'SUBSCRIBE FOR LISTING GAINS & LONG TERM'. Estimated listing price stands at ₹690 per share (listing gain of ₹190 / 38%).",
                subscriptionStatus = "QIB: 42.5x • NII: 28.1x • Retail: 14.8x (Total: 26.4x)"
            ),
            content = "TechWave Cloud IPO issue closes today with overwhelming institutional bidding. Grey Market Premium indicates listing at ~₹690.",
            summary = "TechWave Cloud closes today with 26.4x total subscription and GMP at +38%.",
            source = "NSE / Prime Database",
            category = "Mainboard IPO",
            actionSymbol = "TECHWAVE",
            actionType = "STOCK"
        ),
        NotificationItem(
            id = "notif_news_2",
            type = NotificationType.NEWS,
            title = "Global Tech Rally: Semiconductor & AI Stocks Surge on Cloud Capex Upgrades",
            time = "08:30 AM",
            sentiment = Sentiment.POSITIVE,
            read = true,
            content = "Global technology equities spearheaded a broad market rally after major cloud hyperscalers announced increased capital expenditure for generative AI compute servers.\n\nKey Market Developments:\n• Semiconductor giant Nvidia gained 3.8%, lifting the Philadelphia Semiconductor Index by 2.6%.\n• Apple and Microsoft gained over 1.5% each on enterprise software expansion.\n• Indian IT majors (TCS, Infosys, HCL Tech) expected to see strong traction in digital transformation contracts.\n• Crude oil dipped 1.5% to $74.20/bbl, providing macro support to emerging market valuations.",
            summary = "Semiconductor and AI giants rally as hyperscalers boost cloud data center capex.",
            source = "Reuters Market Desk",
            category = "Global Markets",
            actionSymbol = "INFY.NS",
            actionType = "STOCK"
        ),
        NotificationItem(
            id = "notif_event_2",
            type = NotificationType.EVENT,
            title = "India Consumer Price Index (CPI) Inflation Moderates to 3.65%",
            time = "Yesterday",
            sentiment = Sentiment.POSITIVE,
            read = true,
            eventData = EconomicEvent(
                id = "evt_cpi_in",
                time = "05:30 PM IST",
                currency = "INR",
                event = "India Retail CPI Inflation YoY",
                impact = "HIGH",
                actual = "3.65%",
                forecast = "3.80%",
                previous = "3.92%",
                description = "India headline retail inflation eased to 3.65% in the latest release, remaining well below the central bank target ceiling of 4.0% for the second consecutive month.\n\nHighlights:\n• Food inflation declined to 5.42% from 6.62% aided by kharif crop arrivals.\n• Fuel and light index contracted 1.2% YoY.\n• Core inflation (excluding food and energy) held stable at 3.3%.\n\nImpact on Markets: Sovereign bond yields declined with benchmark 10-year G-Sec yield settling at 6.94%.",
                affectedMarkets = "Nifty 50, Bank Nifty, Sovereign Bonds, INR/USD"
            ),
            content = "India retail CPI cools to 3.65% driven by easing vegetable and food prices.",
            summary = "Retail inflation dips to 3.65%, providing monetary easing room for early 2025.",
            source = "Ministry of Statistics (MoSPI)",
            category = "Macro Indicators",
            actionSymbol = "NIFTY 50",
            actionType = "INDEX"
        ),
        NotificationItem(
            id = "notif_ipo_2",
            type = NotificationType.IPO,
            title = "Nova BioPharma IPO: Opens Tomorrow with Strong ₹285 Cr Anchor Book",
            time = "Yesterday",
            sentiment = Sentiment.POSITIVE,
            read = true,
            ipoData = IPOData(
                symbol = "NOVABIO",
                name = "Nova BioPharma Technologies Ltd",
                openDate = "08 Oct 2026 (Tomorrow)",
                closeDate = "10 Oct 2026",
                priceBand = "₹320 - ₹340",
                gmp = 85.0,
                gmpPercent = 25.0,
                status = "OPENS TOMORROW",
                issueSize = "₹950 Cr",
                lotSize = "44 Shares (₹14,960)",
                listingDate = "15 Oct 2026",
                description = "Nova BioPharma is a research-driven specialty active pharmaceutical ingredient (API) manufacturer supplying oncology and cardiovascular therapies to regulated US and European markets.\n\nAnchor Allocation:\n• Successfully raised ₹285 Cr from institutional investors including marquee global sovereign and mutual funds at ₹340 per share.\n• Current GMP stands at ₹85 (+25.0%), indicating an expected listing price around ₹425.",
                subscriptionStatus = "Anchor Book: 100% Subscribed"
            ),
            content = "Nova BioPharma opens for subscription tomorrow. Issue size ₹950 Cr with GMP at ₹85 (+25%).",
            summary = "Nova BioPharma ₹950 Cr IPO opens tomorrow with 25% grey market premium.",
            source = "BSE / Merchant Bankers",
            category = "Pharma IPO",
            actionSymbol = "NOVABIO",
            actionType = "STOCK"
        ),
        NotificationItem(
            id = "notif_ai_1",
            type = NotificationType.AI_SIGNAL,
            title = "AI Scanner Signal: High-Confidence Bullish Order Block on BANKNIFTY",
            time = "12:45 PM",
            sentiment = Sentiment.POSITIVE,
            read = false,
            aiResult = MOCK_AI_RESULT,
            content = "A significant bullish order block was formed on the 1H timeframe after sweeping sell-side liquidity. Current price action is mitigating the demand zone with institutional buying volume.",
            summary = "Institutional Bullish Order Block identified on BANKNIFTY with 1:3.5 risk/reward.",
            source = "Aarka AI Quant Engine",
            category = "Smart Money Concepts",
            actionSymbol = "BANKNIFTY",
            actionType = "INDEX"
        )
    )
}
