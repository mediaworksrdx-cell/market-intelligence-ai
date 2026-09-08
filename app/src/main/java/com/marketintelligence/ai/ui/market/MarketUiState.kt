package com.marketintelligence.ai.ui.market

import com.marketintelligence.ai.domain.model.*

data class MarketUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val indices: List<IndexData> = emptyList(),
    val watchlist: List<StockData> = emptyList(),
    val cryptos: List<CryptoData> = emptyList(),
    val macroData: List<MacroData> = emptyList(),
    val searchQuery: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val advancingCount: Int = 0,
    val decliningCount: Int = 0
)
