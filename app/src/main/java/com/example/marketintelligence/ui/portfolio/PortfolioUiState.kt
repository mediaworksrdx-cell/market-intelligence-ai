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
    val isAddDialogVisible: Boolean = false
)
