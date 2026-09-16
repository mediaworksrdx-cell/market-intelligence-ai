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
private const val CRYPTO_POLLING_INTERVAL_MS = 3000L // 3 seconds real-time
private const val LIVE_PRICE_POLLING_INTERVAL_MS = 1000L // 1 second

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val marketRepository: MarketRepository,
    private val intelligenceBus: com.marketintelligence.ai.domain.engine.LiveIntelligenceBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var pollingJob: Job? = null
    private var livePricesJob: Job? = null

    init {
        // Observe selected market and indices filtered by country
        combine(
            marketRepository.getIndices(),
            settingsRepository.selectedMarket
        ) { indices, market ->
            val filtered = indices.filter { it.market == market }
            val resolved = if (filtered.isNotEmpty()) filtered else when(market) {
                MarketType.US -> com.marketintelligence.ai.data.local.MockData.INDICES_US
                MarketType.UAE -> com.marketintelligence.ai.data.local.MockData.INDICES_UAE
                MarketType.IN -> com.marketintelligence.ai.data.local.MockData.INDICES_IN
            }
            resolved to market
        }.onEach { (filteredIndices, market) ->
            _uiState.update { it.copy(indices = filteredIndices, selectedMarket = market) }
            filteredIndices.forEach { index ->
                subscribeToInstrument(index.symbol)
            }
        }.launchIn(viewModelScope)

        // Observe watchlist filtered by country
        combine(
            marketRepository.getWatchlist(),
            settingsRepository.selectedMarket
        ) { watchlist, market ->
            val filtered = watchlist.filter { it.market == market }
            val resolved = if (filtered.isNotEmpty()) filtered else when(market) {
                MarketType.US -> com.marketintelligence.ai.data.local.MockData.WATCHLIST_US
                MarketType.UAE -> com.marketintelligence.ai.data.local.MockData.WATCHLIST_UAE
                MarketType.IN -> com.marketintelligence.ai.data.local.MockData.WATCHLIST_INITIAL
            }
            resolved
        }.onEach { filteredStocks ->
            _uiState.update { it.copy(watchlist = filteredStocks) }
            filteredStocks.forEach { stock ->
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
        prices.forEach { p ->
            intelligenceBus.updateLivePrice(p.symbol, p.ltp, p.change, p.changePercent)
        }
        _uiState.update { currentState ->
            val updatedIndices = currentState.indices.map {
                val price = prices.find { p -> matchSymbol(it.symbol, p.symbol) }
                if (price != null) {
                    val resolvedChangePercent = if (kotlin.math.abs(price.changePercent) < 0.0001 && kotlin.math.abs(price.change) > 0.0) {
                        val prevClose = price.ltp - price.change
                        if (prevClose > 0.0) (price.change / prevClose) * 100.0 else price.changePercent
                    } else {
                        price.changePercent
                    }
                    val resolvedChange = if (kotlin.math.abs(price.change) < 0.0001 && kotlin.math.abs(resolvedChangePercent) > 0.0) {
                        (price.ltp * resolvedChangePercent) / 100.0
                    } else {
                        price.change
                    }
                    it.copy(price = price.ltp, change = resolvedChange, changePercent = resolvedChangePercent)
                } else {
                    it
                }
            }
            val updatedWatchlist = currentState.watchlist.map {
                val price = prices.find { p -> matchSymbol(it.symbol, p.symbol) }
                if (price != null) {
                    val resolvedChangePercent = if (kotlin.math.abs(price.changePercent) < 0.0001 && kotlin.math.abs(price.change) > 0.0) {
                        val prevClose = price.ltp - price.change
                        if (prevClose > 0.0) (price.change / prevClose) * 100.0 else price.changePercent
                    } else {
                        price.changePercent
                    }
                    val resolvedChange = if (kotlin.math.abs(price.change) < 0.0001 && kotlin.math.abs(resolvedChangePercent) > 0.0) {
                        (price.ltp * resolvedChangePercent) / 100.0
                    } else {
                        price.change
                    }
                    it.copy(price = price.ltp, change = resolvedChange, changePercent = resolvedChangePercent)
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
        cryptoPrices.forEach { c ->
            intelligenceBus.updateLivePrice(c.symbol, c.price, 0.0, c.changePercent)
        }
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
            ?: uiState.value.cryptos.find { matchCryptoSymbol(it.symbol, symbol) }?.let {
                val chg = it.price * (it.changePercent / 100.0)
                StockData(
                    symbol = it.symbol,
                    name = it.name,
                    price = it.price,
                    openPrice = it.price - chg,
                    change = chg,
                    changePercent = it.changePercent,
                    volume = "",
                    market = com.marketintelligence.ai.domain.model.MarketType.US,
                    instrumentToken = 0L
                )
            }
    }

    private fun matchSymbol(s1: String, s2: String): Boolean {
        val n1 = s1.replace(" ", "").removeSuffix(".NS").removeSuffix(".BO").trim()
        val n2 = s2.replace(" ", "").removeSuffix(".NS").removeSuffix(".BO").trim()
        return n1.equals(n2, ignoreCase = true)
    }

    private fun matchCryptoSymbol(s1: String, s2: String): Boolean {
        val n1 = s1.removeSuffix("USDT").removeSuffix("-USD").removeSuffix("USD").trim().lowercase()
        val n2 = s2.removeSuffix("USDT").removeSuffix("-USD").removeSuffix("USD").trim().lowercase()
        return n1 == n2 || n1.equals(n2, ignoreCase = true)
    }
}
