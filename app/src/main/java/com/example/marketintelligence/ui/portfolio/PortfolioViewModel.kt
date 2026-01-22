package com.example.marketintelligence.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.source.local.TransactionEntity
import com.example.marketintelligence.domain.model.StockQuote
import com.example.marketintelligence.domain.repository.PortfolioRepository
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.usecase.CalculatePortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val settingsRepository: SettingsRepository,
    private val calculatePortfolioUseCase: CalculatePortfolioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState = _uiState.asStateFlow()

    init {
        combine(
            portfolioRepository.getAllTransactions(),
            settingsRepository.selectedMarket
        ) { transactions, selectedMarket ->
            _uiState.update { it.copy(isLoading = true, selectedMarket = selectedMarket) }
            
            // In production, fetch real quotes. Here we use mock logic or assume price = avgPrice for simplicity in demo
            val quotes = transactions.map { it.symbol }.distinct().associateWith { 
                StockQuote(it, 100.0, 5.0, 5.0) // Mock Quote
            }
            
            val allHoldings = calculatePortfolioUseCase(transactions, quotes)
            val filteredHoldings = allHoldings // In a real app, filter by market. For now, show all added.
            
            _uiState.update {
                it.copy(
                    holdings = filteredHoldings,
                    totalCurrentValue = filteredHoldings.sumOf { h -> h.currentValue },
                    totalInvestedValue = filteredHoldings.sumOf { h -> h.investedValue },
                    totalPnl = filteredHoldings.sumOf { h -> h.totalPnl },
                    todayPnl = filteredHoldings.sumOf { h -> h.todayPnl },
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showAddDialog() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            _uiState.update { it.copy(isAddDialogVisible = true) }
        }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(isAddDialogVisible = false) }
    }

    fun confirmAddAsset(quantity: String, price: String) {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val avgPrice = price.toDoubleOrNull() ?: 0.0
        val symbol = _uiState.value.searchQuery.uppercase()

        if (qty > 0 && avgPrice > 0) {
            viewModelScope.launch {
                portfolioRepository.addTransaction(
                    TransactionEntity(
                        symbol = symbol,
                        type = "BUY",
                        quantity = qty,
                        price = avgPrice,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(isAddDialogVisible = false, searchQuery = "") }
            }
        }
    }

    fun liquidateAll() {
        viewModelScope.launch {
            portfolioRepository.deleteAllTransactions()
        }
    }
    
    fun liquidateAsset(symbol: String) {
        viewModelScope.launch {
            portfolioRepository.deleteAsset(symbol)
        }
    }
}
