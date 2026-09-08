package com.marketintelligence.ai.ui.market

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.domain.repository.MarketRepository
import com.marketintelligence.ai.domain.repository.SettingsRepository
import com.marketintelligence.tradeengine.LivePrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MarketViewModel"
private const val CRYPTO_POLLING_INTERVAL_MS = 3000L // 3 seconds
private const val LIVE_PRICE_POLLING_INTERVAL_MS = 1000L // 1 second

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var pollingJob: Job? = null
    private var livePricesJob: Job? = null

    init {
        // Observe indices and subscribe for live prices
        marketRepository.getIndices().onEach { indices ->
            _uiState.update { it.copy(indices = indices) }
            indices.forEach { index ->
                subscribeToInstrument(index.symbol)
            }
        }.launchIn(viewModelScope)

        // Observe watchlist and subscribe for live prices
        marketRepository.getWatchlist().onEach { watchlist ->
            _uiState.update { it.copy(watchlist = watchlist) }
            watchlist.forEach { stock ->
                subscribeToInstrument(stock.symbol)
            }
        }.launchIn(viewModelScope)

        // Observe cryptos
        marketRepository.getCryptos().onEach { cryptos ->
            _uiState.update { it.copy(cryptos = cryptos) }
        }.launchIn(viewModelScope)

        // Fetch initial crypto prices
        viewModelScope.launch {
            startPollingCryptoPrices()
        }
    }

    fun startListeningForLivePrices() {
        if (livePricesJob?.isActive == true) return
        livePricesJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val prices = marketRepository.getLivePrices()
                    updateLivePrices(prices)
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting live prices: ${e.message}")
                }
                delay(LIVE_PRICE_POLLING_INTERVAL_MS)
            }
        }
    }

    fun stopListeningForLivePrices() {
        livePricesJob?.cancel()
        livePricesJob = null
    }

    fun startPollingCryptoPrices() {
        if (pollingJob?.isActive == true) return // Already running
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val cryptoPrices = marketRepository.getCryptoLivePrices()
                    updateCryptoPrices(cryptoPrices)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch crypto prices", e)
                }
                delay(CRYPTO_POLLING_INTERVAL_MS)
            }
        }
    }

    fun stopPollingCryptoPrices() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun subscribeToInstrument(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                marketRepository.subscribeToInstrument(symbol, "1m")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to subscribe to $symbol", e)
            }
        }
    }

    private fun updateLivePrices(prices: List<LivePrice>) {
        _uiState.update { currentState ->
            val updatedIndices = currentState.indices.map {
                val price = prices.find { p -> matchSymbol(it.symbol, p.symbol) }
                if (price != null) {
                    it.copy(price = price.ltp, change = price.change, changePercent = price.changePercent)
                } else {
                    it
                }
            }
            val updatedWatchlist = currentState.watchlist.map {
                val price = prices.find { p -> matchSymbol(it.symbol, p.symbol) }
                if (price != null) {
                    it.copy(price = price.ltp, change = price.change, changePercent = price.changePercent)
                } else {
                    it
                }
            }

            val advancing = (updatedIndices.count { it.change > 0 } + updatedWatchlist.count { it.change > 0 })
            val declining = (updatedIndices.count { it.change < 0 } + updatedWatchlist.count { it.change < 0 })

            currentState.copy(
                indices = updatedIndices,
                watchlist = updatedWatchlist,
                advancingCount = if (advancing == 0 && declining == 0 && prices.isNotEmpty()) prices.count { it.change > 0 } else advancing,
                decliningCount = if (advancing == 0 && declining == 0 && prices.isNotEmpty()) prices.count { it.change < 0 } else declining
            )
        }
    }

    private fun updateCryptoPrices(cryptoPrices: List<CryptoData>) {
        _uiState.update { currentState ->
            val updatedCryptos = currentState.cryptos.map { userCrypto ->
                val live = cryptoPrices.find { matchCryptoSymbol(it.symbol, userCrypto.symbol) }
                if (live != null && live.price > 0) {
                    userCrypto.copy(
                        price = live.price,
                        changePercent = live.changePercent,
                        marketCap = if (live.marketCap > 0) live.marketCap else userCrypto.marketCap
                    )
                } else {
                    userCrypto
                }
            }
            currentState.copy(cryptos = updatedCryptos)
        }
    }

    fun onMarketSelected(market: MarketType) {
        viewModelScope.launch { settingsRepository.setMarket(market) }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch(Dispatchers.IO) {
            _searchResults.value = if (query.isNotBlank()) marketRepository.search(query) else emptyList()
        }
    }

    fun addInstrument(symbol: String, name: String, type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            marketRepository.addToWatchlist(SearchResult(symbol = symbol.trim().uppercase(), name = name.trim(), type = type))
        }
    }

    fun removeIndex(symbol: String) {
        _uiState.update { state -> state.copy(indices = state.indices.filterNot { matchSymbol(it.symbol, symbol) }) }
        viewModelScope.launch(Dispatchers.IO) { marketRepository.removeFromWatchlist(symbol) }
    }

    fun removeStock(symbol: String) {
        _uiState.update { state -> state.copy(watchlist = state.watchlist.filterNot { matchSymbol(it.symbol, symbol) }) }
        viewModelScope.launch(Dispatchers.IO) { marketRepository.removeFromWatchlist(symbol) }
    }

    fun removeCrypto(symbol: String) {
        _uiState.update { state -> state.copy(cryptos = state.cryptos.filterNot { matchCryptoSymbol(it.symbol, symbol) }) }
        viewModelScope.launch(Dispatchers.IO) { marketRepository.removeFromWatchlist(symbol) }
    }

    fun getSelectedInstrumentData(symbol: String): StockData? {
        return uiState.value.watchlist.find { matchSymbol(it.symbol, symbol) }
            ?: uiState.value.indices.find { matchSymbol(it.symbol, symbol) }?.let {
                StockData(it.symbol, it.name, it.price, it.openPrice, it.change, it.changePercent, "", it.market, it.instrumentToken)
            }
    }

    private fun matchSymbol(s1: String, s2: String): Boolean {
        val n1 = s1.removeSuffix(".NS").removeSuffix(".BO").trim()
        val n2 = s2.removeSuffix(".NS").removeSuffix(".BO").trim()
        return n1.equals(n2, ignoreCase = true)
    }

    private fun matchCryptoSymbol(s1: String, s2: String): Boolean {
        val n1 = s1.removeSuffix("-USD").removeSuffix("USD").trim().lowercase()
        val n2 = s2.removeSuffix("-USD").removeSuffix("USD").trim().lowercase()
        return n1 == n2 || n1.equals(n2, ignoreCase = true)
    }
}
