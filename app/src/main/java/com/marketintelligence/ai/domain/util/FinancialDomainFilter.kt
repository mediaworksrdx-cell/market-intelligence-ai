package com.marketintelligence.ai.domain.util

import java.util.Locale

object FinancialDomainFilter {

    private val FINANCIAL_KEYWORDS = setOf(
        // Market & Asset types
        "nifty", "sensex", "banknifty", "finnifty", "stock", "stocks", "share", "shares", "equity", "equities",
        "market", "markets", "indices", "index", "nse", "bse", "nasdaq", "nyse", "spx", "dow", "crypto",
        "btc", "eth", "forex", "commodity", "gold", "silver", "crude", "oil", "midcap", "vix", "india vix",

        // Top Companies & Tickers
        "reliance", "ril", "tcs", "tata consultancy", "infy", "infosys", "hdfc", "hdfcbank", "icici",
        "icicibank", "sbi", "sbin", "state bank", "tata steel", "tatasteel", "tata motors", "tatamotors",
        "airtel", "bhartiartl", "bharti airtel", "larsen", "l&t", "lt", "itc", "wipro", "maruti",
        "titan", "sunpharma", "bajfinance", "bajaj finance", "adanient", "adani", "hindunilvr", "hul",
        "zomato", "paytm", "jio", "apple", "aapl", "microsoft", "msft", "google", "googl", "amazon",
        "amzn", "tesla", "tsla", "nvidia", "nvda", "meta", "dfmgi", "emaar", "dewa",

        // Technical Analysis & SMC Studies
        "technical", "technicals", "chart", "charts", "candlestick", "candle", "pattern", "trend",
        "support", "resistance", "breakout", "breakdown", "smc", "order block", "orderblock", "fvg",
        "fair value gap", "liquidity", "liquidity sweep", "choch", "bos", "change of character",
        "break of structure", "swing high", "swing low", "pullback", "volume", "rsi", "macd", "ema",
        "sma", "moving average", "bollinger", "atr", "supertrend", "vwap", "price action", "invalidation",
        "projection", "study zone", "demand zone", "supply zone", "imbalance", "mitigation", "premium", "discount",

        // Fundamental Analysis & Valuation Metrics
        "fundamental", "fundamentals", "valuation", "pe", "pe ratio", "p/e", "pb", "pb ratio", "p/b",
        "eps", "roe", "roce", "return on capital", "return on equity", "book value", "market cap",
        "market capitalization", "large cap", "mid cap", "small cap", "debt", "debt to equity", "debt/equity",
        "cash flow", "free cash flow", "fcf", "operating cash flow", "dividend", "dividend yield", "yield",
        "payout ratio", "ebitda", "ebit", "operating margin", "net margin", "gross margin", "pat",
        "profit after tax", "pbt", "capex", "promoter", "fii", "dii", "shareholding", "balance sheet",
        "income statement", "annual report", "financial statements", "financial report", "working capital",

        // Company Results & Earnings
        "result", "results", "quarterly", "quarter", "q1", "q2", "q3", "q4", "earning", "earnings",
        "revenue", "profit", "net profit", "loss", "guidance", "sales", "turnover", "audit", "yoy", "qoq",

        // F&O & Derivatives
        "fno", "futures", "options", "option", "option chain", "strike", "expiry", "call", "put",
        "pcr", "put-call ratio", "put call ratio", "max pain", "open interest", "oi", "call wall",
        "put wall", "gamma", "theta", "vega", "delta", "greeks", "iv", "implied volatility", "buildup",
        "long buildup", "short buildup", "short covering", "long unwinding", "straddle", "strangle",
        "iron condor", "spread", "bull call spread", "bear put spread",

        // Portfolio, Personal Finance & Investment Education
        "portfolio", "holding", "holdings", "pnl", "profit and loss", "allocation", "investment",
        "investments", "invest", "investing", "investor", "investors", "returns", "risk", "rebalance",
        "sip", "systematic investment plan", "mutual fund", "mutual funds", "etf", "etfs", "compound",
        "compounding", "dollar cost averaging", "dca", "lump sum", "diversification", "diversify",
        "asset allocation", "emergency fund", "wealth", "rule of 72", "cagr", "passive investing",
        "active investing", "index fund", "index funds", "fixed deposit", "fd", "debt fund",
        "capital preservation", "financial freedom", "risk management", "1% risk", "position size",
        "position sizing", "drawdown", "loss aversion", "fomo", "revenge trading", "disposition effect",
        "trading psychology", "discipline", "bias", "behavioral bias",

        // General Financial Terms & Macro
        "inflation", "interest rate", "interest rates", "gdp", "rbi", "fed", "fiscal", "monetary",
        "sector", "banking", "it", "pharma", "auto", "fmcg", "metal", "energy", "realty", "trade",
        "trading", "trader", "traders", "broker", "demat"
    )

    fun isFinancialQuery(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT).trim()
        if (lower.isBlank()) return false

        // 1. Single word or ticker check (e.g. "RELIANCE", "TCS", "INFY", "BTC", "SIP", "NIFTY")
        val words = lower.split("\\s+".toRegex())
        if (words.size <= 2 && lower.length in 2..15 && words.all { w -> w.all { it.isLetterOrDigit() || it == '.' } }) {
            return true
        }

        // 2. Multi-word phrase matches
        return FINANCIAL_KEYWORDS.any { keyword ->
            if (keyword.contains(" ")) {
                lower.contains(keyword)
            } else {
                lower.contains(Regex("\\b${Regex.escape(keyword)}\\b"))
            }
        }
    }

    const val REDIRECTION_MESSAGE =
        "I am your Market Intelligence AI Mentor, dedicated strictly to financial markets, stock and index analysis, company quarterly results, fundamentals, and investment education. Please submit a question regarding financial markets, company performance, or investment concepts."
}
