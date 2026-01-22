package com.example.marketintelligence.ui.fno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.BuildupEngine
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.usecase.FnoWorkflowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class FnoViewModel @Inject constructor(
    private val fnoWorkflowUseCase: FnoWorkflowUseCase,
    private val engineRouter: EngineRouter,
    private val intelligenceBus: LiveIntelligenceBus,
    private val buildupEngine: BuildupEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FnoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        settingsRepository.selectedMarket.onEach { market ->
            val defaultAsset = when(market) {
                MarketType.IN -> "NIFTY 50"
                MarketType.US -> "SPX"
                MarketType.UAE -> "DFMGI"
            }
            selectAsset(defaultAsset)
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addAsset() {
        val asset = _uiState.value.searchQuery.uppercase()
        if (asset.isNotBlank()) {
            selectAsset(asset)
            _uiState.update { it.copy(searchQuery = "") }
        }
    }

    fun selectStrategy(strategy: OptionStrategy) {
        _uiState.update { it.copy(selectedStrategy = strategy) }
    }

    fun selectAsset(asset: String) {
        _uiState.update { it.copy(isLoading = true, selectedAsset = asset, errorMessage = null) }
        
        viewModelScope.launch {
            val spotPrice = if (asset.contains("NIFTY")) 22500.0 else 1000.0
            
            // Mock Summary Data
            val summary = FnoSummary(
                futuresPrices = listOf(
                    FuturePrice("Near", spotPrice + 50),
                    FuturePrice("Next", spotPrice + 120),
                    FuturePrice("Far", spotPrice + 180)
                ),
                iv = 14.5,
                pcr = 0.95,
                maxPain = spotPrice - (spotPrice % 50),
                highestCallOIStrike = spotPrice + 500,
                highestPutOIStrike = spotPrice - 500,
                nearMonthStrike = spotPrice - (spotPrice % 100)
            )

            // Mock Option Chain
            val strikes = List(10) { i ->
                val strike = spotPrice - 250 + (i * 50)
                OptionChainData(
                    strike = strike,
                    callOI = 100000.0 * Random.nextDouble(),
                    callLTP = 100.0 * Random.nextDouble(),
                    callChange = 10.0,
                    callIV = 15.0,
                    callGreeks = Greeks(0.5, 0.01, -10.0, 5.0),
                    putOI = 80000.0 * Random.nextDouble(),
                    putLTP = 80.0 * Random.nextDouble(),
                    putChange = -5.0,
                    putIV = 16.0,
                    putGreeks = Greeks(-0.4, 0.01, -8.0, 4.0)
                )
            }
            val optionChain = OptionChain(spotPrice, strikes)

            val strategies = generateMockStrategies(spotPrice)

            _uiState.update { 
                it.copy(
                    spotPrice = spotPrice,
                    summary = summary,
                    optionChain = optionChain,
                    optionsStudyStats = OptionsStudyStats(summary.maxPain, summary.iv, summary.pcr),
                    availableStrategies = strategies,
                    selectedStrategy = strategies.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    private fun generateMockStrategies(spotPrice: Double): List<OptionStrategy> {
        return listOf(
            OptionStrategy("1", "Bull Call Spread", "Moderately Bullish Strategy", 5000.0, 2000.0, spotPrice + 100, 65.0, 25.0, listOf("Buy ATM CE", "Sell OTM CE"), generatePayoff(spotPrice)),
            OptionStrategy("2", "Iron Condor", "Neutral Range Strategy", 3000.0, 7000.0, spotPrice, 75.0, 15.0, listOf("Sell OTM CE", "Sell OTM PE", "Buy further OTM CE", "Buy further OTM PE"), generatePayoff(spotPrice)),
            OptionStrategy("3", "Bear Put Spread", "Moderately Bearish Strategy", 4500.0, 2500.0, spotPrice - 100, 60.0, 20.0, listOf("Buy ATM PE", "Sell OTM PE"), generatePayoff(spotPrice)),
            OptionStrategy("4", "Short Strangle", "Low Volatility Strategy", 8000.0, 0.0, spotPrice, 80.0, 10.0, listOf("Sell OTM CE", "Sell OTM PE"), generatePayoff(spotPrice))
        )
    }

    private fun generatePayoff(spot: Double): List<PayoffPoint> {
        val points = mutableListOf<PayoffPoint>()
        for (i in -10..10) {
            points.add(PayoffPoint(spot + (i * 50), (i * 200).toDouble()))
        }
        return points
    }
}
