package com.marketintelligence.ai.ui.portfolio

import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.model.Holding

enum class PortfolioTab(val label: String, val currencyCode: String, val currencySymbol: String) {
    ALL("ALL", "MULTI", ""),
    INDIAN_STOCKS("INDIAN STOCKS", "INR", "₹"),
    CRYPTO("CRYPTO", "USD", "$"),
    US_STOCKS("US STOCKS", "USD", "$")
}

data class PortfolioUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val selectedTab: PortfolioTab = PortfolioTab.ALL,
    val displayCurrencySymbol: String = "₹",
    val displayCurrencyCode: String = "INR",
    val holdings: List<Holding> = emptyList(),
    val allHoldings: List<Holding> = emptyList(),

    // Tab-specific metrics
    val totalCurrentValue: Double = 0.0,
    val totalInvestedValue: Double = 0.0,
    val totalPnl: Double = 0.0,
    val todayPnl: Double = 0.0,

    // Pure segregated currency totals for Indian Stocks (INR)
    val inrCurrentValue: Double = 0.0,
    val inrInvestedValue: Double = 0.0,
    val inrTotalPnl: Double = 0.0,
    val inrTodayPnl: Double = 0.0,
    val indianStocksCount: Int = 0,

    // Pure segregated currency totals for USD assets (Crypto & US Stocks)
    val usdCurrentValue: Double = 0.0,
    val usdInvestedValue: Double = 0.0,
    val usdTotalPnl: Double = 0.0,
    val usdTodayPnl: Double = 0.0,
    val cryptoCount: Int = 0,
    val usStocksCount: Int = 0,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Search & Add State
    val searchQuery: String = "",
    val isAddDialogVisible: Boolean = false,
    val prefillSymbol: String = "",
    val prefillCategory: String = "INDIAN_STOCK",

    // Safety Dialog States
    val holdingToLiquidate: String? = null,
    val isLiquidateAllDialogVisible: Boolean = false
) {
    val totalPnlPercent: Double
        get() = if (totalInvestedValue > 0.0) (totalPnl / totalInvestedValue) * 100.0 else 0.0

    val todayPnlPercent: Double
        get() = if (totalInvestedValue > 0.0) (todayPnl / totalInvestedValue) * 100.0 else 0.0

    val inrPnlPercent: Double
        get() = if (inrInvestedValue > 0.0) (inrTotalPnl / inrInvestedValue) * 100.0 else 0.0

    val inrTodayPnlPercent: Double
        get() = if (inrInvestedValue > 0.0) (inrTodayPnl / inrInvestedValue) * 100.0 else 0.0

    val usdPnlPercent: Double
        get() = if (usdInvestedValue > 0.0) (usdTotalPnl / usdInvestedValue) * 100.0 else 0.0

    val usdTodayPnlPercent: Double
        get() = if (usdInvestedValue > 0.0) (usdTodayPnl / usdInvestedValue) * 100.0 else 0.0
}
