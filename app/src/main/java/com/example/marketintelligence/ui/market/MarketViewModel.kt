package com.example.marketintelligence.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.local.MockData
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<String>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        // Connect to global market setting
        settingsRepository.selectedMarket.onEach { market ->
            _uiState.update { it.copy(selectedMarket = market) }
            loadDataForMarket(market)
        }.launchIn(viewModelScope)
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun onMarketSelected(market: MarketType) {
        viewModelScope.launch {
            settingsRepository.setMarket(market)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addAsset() {
        val symbol = _uiState.value.searchQuery.uppercase()
        if (symbol.isBlank()) return

        // In a real app, you'd check symbol type via API. 
        // Here we apply a heuristic: If it contains BTC, ETH, SOL, or '-' it's crypto for this demo.
        val isCrypto = symbol.contains("BTC") || symbol.contains("ETH") || symbol.contains("SOL") || symbol.contains("-")
        
        if (isCrypto) {
            val newCrypto = CryptoData(symbol, "New Crypto Asset", 0.0, 0.0, 0.0)
            _uiState.update { it.copy(cryptos = it.cryptos + newCrypto, searchQuery = "") }
        } else {
            val newStock = StockData(symbol, "New $symbol Asset", 0.0, 0.0, 0.0, "0", _uiState.value.selectedMarket)
            _uiState.update { it.copy(watchlist = it.watchlist + newStock, searchQuery = "") }
        }
    }

    fun removeIndex(symbol: String) {
        _uiState.update { it.copy(indices = it.indices.filter { idx -> idx.symbol != symbol }) }
    }

    fun removeStock(symbol: String) {
        _uiState.update { it.copy(watchlist = it.watchlist.filter { s -> s.symbol != symbol }) }
    }

    fun removeCrypto(symbol: String) {
        _uiState.update { it.copy(cryptos = it.cryptos.filter { c -> c.symbol != symbol }) }
    }

    private fun loadDataForMarket(market: MarketType) {
        _uiState.update { currentState ->
            currentState.copy(
                indices = when(market) {
                    MarketType.IN -> MockData.INDICES_IN
                    MarketType.US -> listOf(
                        IndexData("SPX", "S&P 500", 5000.0, 25.0, 0.5, MarketType.US),
                        IndexData("DJI", "Dow Jones", 38000.0, -150.0, -0.4, MarketType.US),
                        IndexData("IXIC", "Nasdaq", 16000.0, 120.0, 0.75, MarketType.US),
                        IndexData("RUT", "Russell 2000", 2000.0, 10.0, 0.5, MarketType.US)
                    )
                    MarketType.UAE -> listOf(
                        IndexData("DFMGI", "DFM General", 4200.0, 15.0, 0.35, MarketType.UAE),
                        IndexData("ADI", "ADX General", 9500.0, -20.0, -0.21, MarketType.UAE)
                    )
                },
                watchlist = MockData.WATCHLIST_INITIAL.filter { it.market == market }.take(12),
                cryptos = listOf(
                    CryptoData("BTC", "Bitcoin", 65000.0, 1200.0, 1.8),
                    CryptoData("ETH", "Ethereum", 3500.0, 50.0, 1.4),
                    CryptoData("SOL", "Solana", 145.0, -5.0, -3.3)
                )
            )
        }
    }
}
