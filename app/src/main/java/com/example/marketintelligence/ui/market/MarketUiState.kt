package com.example.marketintelligence.ui.market

import com.example.marketintelligence.domain.model.IndexData
import com.example.marketintelligence.domain.model.StockData
import com.example.marketintelligence.domain.model.CryptoData
import com.example.marketintelligence.domain.model.MarketType

data class MarketUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val indices: List<IndexData> = emptyList(),
    val watchlist: List<StockData> = emptyList(),
    val cryptos: List<CryptoData> = emptyList(),
    val searchQuery: String = "",
    val isEditMode: Boolean = false, // New field for edit mode
    val isLoading: Boolean = false,
    val error: String? = null
)
