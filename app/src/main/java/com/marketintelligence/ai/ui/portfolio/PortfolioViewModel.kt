package com.marketintelligence.ai.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.data.source.local.TransactionEntity
import com.marketintelligence.ai.domain.model.StockQuote
import com.marketintelligence.ai.domain.repository.MarketRepository
import com.marketintelligence.ai.domain.repository.PortfolioRepository
import com.marketintelligence.ai.domain.repository.SettingsRepository
import com.marketintelligence.ai.domain.usecase.CalculatePortfolioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val settingsRepository: SettingsRepository,
    private val marketRepository: MarketRepository,
    private val calculatePortfolioUseCase: CalculatePortfolioUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState = _uiState.asStateFlow()

    private val _liveQuotes = MutableStateFlow<Map<String, StockQuote>>(emptyMap())
    private var quotePollingJob: Job? = null

    init {
        // Start background polling for real-time market prices
        startQuotePolling()

        // Reactive stream combining local transactions, user market, and live quotes
        combine(
            portfolioRepository.getAllTransactions(),
            settingsRepository.selectedMarket,
            _liveQuotes
        ) { transactions, selectedMarket, liveQuotes ->
            _uiState.update { it.copy(isLoading = true, selectedMarket = selectedMarket) }

            // Build dynamic quotes for every transaction symbol
            val quotes = transactions.groupBy { it.symbol.uppercase() }.mapValues { (sym, txList) ->
                val normSym = sym.removeSuffix(".NS").removeSuffix(".BO")
                val quote = liveQuotes[sym]
                    ?: liveQuotes[normSym]
                    ?: liveQuotes["$normSym.NS"]
                    ?: liveQuotes.entries.find { it.key.startsWith(normSym) }?.value

                if (quote != null) {
                    quote
                } else {
                    // Fallback to average purchase price so unrealized P&L is 0 instead of fake mock losses
                    val lastPrice = txList.maxByOrNull { it.timestamp }?.price ?: 0.0
                    StockQuote(symbol = sym, price = lastPrice, change = 0.0, changePercent = 0.0)
                }
            }

            val allHoldings = calculatePortfolioUseCase(transactions, quotes)

            _uiState.update {
                it.copy(
                    holdings = allHoldings,
                    totalCurrentValue = allHoldings.sumOf { h -> h.currentValue },
                    totalInvestedValue = allHoldings.sumOf { h -> h.investedValue },
                    totalPnl = allHoldings.sumOf { h -> h.totalPnl },
                    todayPnl = allHoldings.sumOf { h -> h.todayPnl },
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun startQuotePolling() {
        quotePollingJob?.cancel()
        quotePollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    refreshQuotes()
                } catch (_: Exception) {}
                delay(5000)
            }
        }
    }

    suspend fun refreshQuotes() {
        val quotesMap = mutableMapOf<String, StockQuote>()

        // 1. Fetch Indian & Global Equity live quotes
        try {
            val livePrices = marketRepository.getLivePrices()
            for (lp in livePrices) {
                val upper = lp.symbol.uppercase()
                val q = StockQuote(
                    symbol = lp.symbol,
                    price = lp.ltp,
                    change = lp.change,
                    changePercent = lp.changePercent
                )
                quotesMap[upper] = q
                val clean = upper.removeSuffix(".NS").removeSuffix(".BO")
                quotesMap[clean] = q
            }
        } catch (_: Exception) {}

        // 2. Fetch Crypto live quotes
        try {
            val cryptoPrices = marketRepository.getCryptoLivePrices()
            for (cp in cryptoPrices) {
                val upper = cp.symbol.uppercase()
                val change = (cp.price * cp.changePercent) / 100.0
                val q = StockQuote(
                    symbol = cp.symbol,
                    price = cp.price,
                    change = change,
                    changePercent = cp.changePercent
                )
                quotesMap[upper] = q
                val clean = upper.removeSuffix("-USD").removeSuffix("USDT")
                quotesMap[clean] = q
            }
        } catch (_: Exception) {}

        if (quotesMap.isNotEmpty()) {
            _liveQuotes.value = quotesMap
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showAddDialog(symbol: String = "") {
        val defaultSym = if (symbol.isNotBlank()) symbol else _uiState.value.searchQuery
        _uiState.update { it.copy(isAddDialogVisible = true, prefillSymbol = defaultSym) }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(isAddDialogVisible = false) }
    }

    fun confirmAddAsset(symbol: String, quantity: String, price: String) {
        val qty = quantity.toDoubleOrNull() ?: 0.0
        val avgPrice = price.toDoubleOrNull() ?: 0.0
        val cleanSymbol = symbol.trim().uppercase()

        if (cleanSymbol.isNotBlank() && qty > 0 && avgPrice > 0) {
            viewModelScope.launch {
                portfolioRepository.addTransaction(
                    TransactionEntity(
                        symbol = cleanSymbol,
                        type = "BUY",
                        quantity = qty,
                        price = avgPrice,
                        timestamp = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(isAddDialogVisible = false, searchQuery = "", prefillSymbol = "") }
                refreshQuotes()
            }
        }
    }

    fun requestLiquidateAsset(symbol: String) {
        _uiState.update { it.copy(holdingToLiquidate = symbol) }
    }

    fun dismissLiquidateAsset() {
        _uiState.update { it.copy(holdingToLiquidate = null) }
    }

    fun confirmLiquidateAsset() {
        val symbol = _uiState.value.holdingToLiquidate ?: return
        viewModelScope.launch {
            portfolioRepository.deleteAsset(symbol)
            _uiState.update { it.copy(holdingToLiquidate = null) }
        }
    }

    fun requestLiquidateAll() {
        _uiState.update { it.copy(isLiquidateAllDialogVisible = true) }
    }

    fun dismissLiquidateAll() {
        _uiState.update { it.copy(isLiquidateAllDialogVisible = false) }
    }

    fun confirmLiquidateAll() {
        viewModelScope.launch {
            portfolioRepository.deleteAllTransactions()
            _uiState.update { it.copy(isLiquidateAllDialogVisible = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        quotePollingJob?.cancel()
    }
}
