package com.example.marketintelligence.ui.portfolio

import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.Holding

data class PortfolioUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val holdings: List<Holding> = emptyList(),
    val totalCurrentValue: Double = 0.0,
    val totalInvestedValue: Double = 0.0,
    val totalPnl: Double = 0.0,
    val todayPnl: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // Search & Add State
    val searchQuery: String = "",
    val isAddDialogVisible: Boolean = false,
    val prefillSymbol: String = "",
    
    // Safety Dialog States
    val holdingToLiquidate: String? = null,
    val isLiquidateAllDialogVisible: Boolean = false
) {
    val totalPnlPercent: Double
        get() = if (totalInvestedValue > 0.0) (totalPnl / totalInvestedValue) * 100.0 else 0.0

    val todayPnlPercent: Double
        get() = if (totalInvestedValue > 0.0) (todayPnl / totalInvestedValue) * 100.0 else 0.0
}
