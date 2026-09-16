package com.marketintelligence.ai.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.domain.model.SearchResult
import com.marketintelligence.ai.domain.repository.InstrumentRepository
import com.marketintelligence.ai.domain.repository.MarketRepository
import com.marketintelligence.ai.domain.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class InstrumentSearchViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _rawResults = MutableStateFlow<List<SearchResult>>(emptyList())

    // Track all instruments currently added in Hub (indices, stocks, crypto)
    val addedSymbols: StateFlow<Set<String>> = combine(
        marketRepository.getIndices(),
        marketRepository.getWatchlist(),
        marketRepository.getCryptos()
    ) { indices, stocks, cryptos ->
        val set = mutableSetOf<String>()
        indices.forEach { set.add(it.symbol.uppercase()) }
        stocks.forEach { set.add(it.symbol.uppercase()) }
        cryptos.forEach { set.add(it.symbol.uppercase()) }
        set
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val syncStatus = instrumentRepository.getSyncStatus()

    // Curated suggestions when search query is empty
    private val defaultSuggestions = listOf(
        // Core Indian Indices
        SearchResult("NIFTY 50", "NIFTY 50 Index", "INDEX"),
        SearchResult("BANKNIFTY", "Nifty Bank Index", "INDEX"),
        SearchResult("FINNIFTY", "Nifty Financial Services", "INDEX"),
        SearchResult("SENSEX", "BSE SENSEX", "INDEX"),
        SearchResult("MIDCPNIFTY", "NIFTY Midcap 50", "INDEX"),
        // Core US Indices
        SearchResult("SPX", "S&P 500 Index", "INDEX"),
        SearchResult("NDX", "NASDAQ 100 Index", "INDEX"),
        SearchResult("DJI", "Dow Jones Industrial", "INDEX"),
        // Core UAE Indices
        SearchResult("DFMGI", "DFM General Index", "INDEX"),
        SearchResult("ADX", "Abu Dhabi Securities Exchange", "INDEX"),

        // Indian Equities
        SearchResult("RELIANCE.NS", "Reliance Industries Ltd", "STOCK"),
        SearchResult("TCS.NS", "Tata Consultancy Services", "STOCK"),
        SearchResult("HDFCBANK.NS", "HDFC Bank Ltd", "STOCK"),
        SearchResult("INFY.NS", "Infosys Ltd", "STOCK"),
        SearchResult("ICICIBANK.NS", "ICICI Bank Ltd", "STOCK"),
        SearchResult("BHARTIARTL.NS", "Bharti Airtel Ltd", "STOCK"),
        SearchResult("TATAMOTORS.NS", "Tata Motors Ltd", "STOCK"),
        SearchResult("ITC.NS", "ITC Limited", "STOCK"),
        SearchResult("SBIN.NS", "State Bank of India", "STOCK"),
        SearchResult("LT.NS", "Larsen & Toubro Ltd", "STOCK"),

        // US Equities
        SearchResult("AAPL", "Apple Inc.", "STOCK"),
        SearchResult("NVDA", "NVIDIA Corporation", "STOCK"),
        SearchResult("MSFT", "Microsoft Corporation", "STOCK"),
        SearchResult("GOOGL", "Alphabet Inc.", "STOCK"),
        SearchResult("AMZN", "Amazon.com Inc.", "STOCK"),
        SearchResult("TSLA", "Tesla Inc.", "STOCK"),
        SearchResult("META", "Meta Platforms Inc.", "STOCK"),

        // UAE Equities
        SearchResult("EMAAR", "Emaar Properties PJSC", "STOCK"),
        SearchResult("DEWA", "Dubai Electricity & Water", "STOCK"),
        SearchResult("FAB", "First Abu Dhabi Bank", "STOCK"),
        SearchResult("ALDAR", "Aldar Properties", "STOCK"),
        SearchResult("EMIRATESNBD", "Emirates NBD Bank", "STOCK"),

        // Crypto Intelligence
        SearchResult("BTC", "Bitcoin", "CRYPTO"),
        SearchResult("ETH", "Ethereum", "CRYPTO"),
        SearchResult("SOL", "Solana", "CRYPTO"),
        SearchResult("DOGE", "Dogecoin", "CRYPTO"),
        SearchResult("BNB", "Binance Coin", "CRYPTO"),
        SearchResult("SHIB", "Shiba Inu", "CRYPTO"),
        SearchResult("ADA", "Cardano", "CRYPTO"),
        SearchResult("XRP", "XRP", "CRYPTO"),
        SearchResult("AVAX", "Avalanche", "CRYPTO")
    )

    // Filtered search results based on query & category
    val searchResults: StateFlow<List<SearchResult>> = combine(
        _rawResults,
        _searchQuery,
        _selectedCategory
    ) { raw, query, category ->
        val sourceList = if (query.isBlank()) defaultSuggestions else raw
        if (category == "ALL") {
            sourceList
        } else {
            val filterType = when (category) {
                "INDICES" -> "INDEX"
                "STOCKS" -> "STOCK"
                "CRYPTO" -> "CRYPTO"
                else -> category
            }
            sourceList.filter { it.type.equals(filterType, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultSuggestions)

    init {
        viewModelScope.launch {
            instrumentRepository.syncInstrumentsIfNeeded()
        }

        _searchQuery
            .debounce(250)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    _rawResults.value = marketRepository.search(query)
                } else {
                    _rawResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun addInstrument(instrument: SearchResult, onAdded: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            marketRepository.addToWatchlist(instrument)
            withContext(Dispatchers.Main) {
                onAdded()
            }
        }
    }

    fun onInstrumentSelected(instrument: SearchResult) {
        viewModelScope.launch(Dispatchers.IO) {
            marketRepository.addToWatchlist(instrument)
        }
    }

    fun removeInstrument(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            marketRepository.removeFromWatchlist(symbol)
        }
    }
}
