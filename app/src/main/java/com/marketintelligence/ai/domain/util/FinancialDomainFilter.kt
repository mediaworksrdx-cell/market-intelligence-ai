package com.marketintelligence.ai.domain.util

import java.util.Locale

object FinancialDomainFilter {

    private val FINANCIAL_KEYWORDS = setOf(
        // Market & Asset types
        "nifty", "sensex", "banknifty", "finnifty", "stock", "share", "equity", "market", "indices",
        "index", "nse", "bse", "nasdaq", "nyse", "spx", "dow", "crypto", "btc", "eth", "forex",
        "commodity", "gold", "silver", "crude", "oil",

        // Technical Analysis & SMC Studies
        "technical", "technicals", "chart", "candlestick", "pattern", "trend", "support", "resistance",
        "breakout", "breakdown", "smc", "order block", "orderblock", "fvg", "fair value gap",
        "liquidity", "choch", "bos", "change of character", "break of structure", "swing high",
        "swing low", "pullback", "volume", "rsi", "macd", "ema", "sma", "moving average",
        "bollinger", "atr", "supertrend", "vwap", "price action", "invalidation", "projection",
        "study zone", "demand zone", "supply zone", "imbalance", "mitigation", "premium", "discount",

        // Fundamental Analysis & Ratios
        "fundamental", "fundamentals", "valuation", "pe", "p/e", "pb", "p/b", "eps", "roe", "roce",
        "book value", "market cap", "debt", "equity", "cash flow", "free cash flow", "dividend",
        "yield", "ebitda", "ebit", "operating margin", "net margin", "capex", "promoter",
        "fii", "dii", "shareholding", "balance sheet", "income statement", "annual report",

        // Company Results & Earnings
        "result", "results", "quarterly", "quarter", "q1", "q2", "q3", "q4", "earning", "earnings",
        "revenue", "profit", "net profit", "loss", "guidance", "sales", "turnover", "audit",
        "financial report", "financial statements", "yoy", "qoq",

        // F&O & Derivatives
        "fno", "futures", "options", "option chain", "strike", "call", "put", "pcr", "put-call ratio",
        "max pain", "open interest", "oi", "call wall", "put wall", "gamma", "theta", "vega", "iv",
        "implied volatility", "buildup", "straddle", "strangle", "spread",

        // Portfolio & Holdings
        "portfolio", "holding", "holdings", "pnl", "profit and loss", "allocation", "investment",
        "invest", "invested", "returns", "risk", "rebalance",

        // General Financial Terms
        "inflation", "interest rate", "gdp", "rbi", "fed", "fiscal", "monetary", "sector", "banking",
        "it", "pharma", "auto", "fmcg", "metal", "energy", "realty"
    )

    fun isFinancialQuery(query: String): Boolean {
        val lower = query.lowercase(Locale.ROOT).trim()
        if (lower.isBlank()) return false

        // Single word or ticker check (e.g. "RELIANCE", "TCS", "INFY", "BTC")
        val words = lower.split("\\s+".toRegex())
        if (words.size <= 2 && lower.length in 2..15 && words.all { w -> w.all { it.isLetterOrDigit() || it == '.' } }) {
            return true
        }

        // Match against known keywords
        return FINANCIAL_KEYWORDS.any { keyword ->
            if (keyword.contains(" ")) {
                lower.contains(keyword)
            } else {
                lower.contains(Regex("\\b${Regex.escape(keyword)}\\b"))
            }
        }
    }

    const val REDIRECTION_MESSAGE = 
        "I am your Market Intelligence AI Mentor, dedicated strictly to financial markets, technical analysis studies, fundamental evaluation, and company quarterly results. Please ask a market, company, or technical study question to begin."
}
