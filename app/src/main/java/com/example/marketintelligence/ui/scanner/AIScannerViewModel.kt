package com.example.marketintelligence.ui.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.usecase.ScanStockUseCase
import com.example.redxaiscanner.background.AnalysisWorker
import com.example.redxaiscanner.engine.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AIScannerUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val autoScanAssets: List<String> = listOf("NIFTY", "BANKNIFTY", "BTC", "ETH"),
    val manualScanResults: List<TradeSetup> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTimeframe: String = "1H"
)

@HiltViewModel
class AIScannerViewModel @Inject constructor(
    private val scanStockUseCase: ScanStockUseCase,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIScannerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        setupPeriodicScan()
        
        settingsRepository.selectedMarket.onEach { market ->
            val defaultAutoScanList = when(market) {
                MarketType.IN -> listOf("NIFTY", "BANKNIFTY", "RELIANCE", "HDFCBANK")
                MarketType.US -> listOf("SPX", "NDX", "AAPL", "NVDA")
                MarketType.UAE -> listOf("DFMGI", "ADI", "EMAAR")
            }
            _uiState.update { it.copy(selectedMarket = market, autoScanAssets = defaultAutoScanList) }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTimeframeSelected(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun addAutoScanAsset(symbol: String) {
        if (_uiState.value.autoScanAssets.size < 10 && symbol.isNotBlank()) {
            _uiState.update { it.copy(autoScanAssets = it.autoScanAssets + symbol.uppercase()) }
        }
    }

    fun removeAutoScanAsset(symbol: String) {
        _uiState.update { it.copy(autoScanAssets = it.autoScanAssets - symbol) }
    }

    fun forceAutoScan() {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putStringArray(AnalysisWorker.KEY_SYMBOLS, _uiState.value.autoScanAssets.toTypedArray())
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(inputData)
            .build()
        
        workManager.enqueue(workRequest)
    }

    private fun setupPeriodicScan() {
        val workManager = WorkManager.getInstance(context)
        val inputData = Data.Builder()
            .putStringArray(AnalysisWorker.KEY_SYMBOLS, _uiState.value.autoScanAssets.toTypedArray())
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<AnalysisWorker>(2, TimeUnit.HOURS)
            .setInputData(inputData)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            AnalysisWorker.UNIQUE_PERIODIC_SCAN,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun startManualScan() {
        val symbol = _uiState.value.searchQuery.uppercase()
        if (symbol.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            // Simulated Advanced Study Generation
            kotlinx.coroutines.delay(1500) // Simulation delay
            
            val mockSetup = TradeSetup(
                entryPrice = BigDecimal("22500.00"),
                stopLossPrice = BigDecimal("22420.00"),
                takeProfit1 = BigDecimal("22650.00"),
                takeProfit2 = BigDecimal("22800.00"),
                riskToRewardRatio = "1:3.0",
                underlyingSignal = ScoredSignal(
                    confidenceScore = 89,
                    scoreBreakdown = mapOf("MTF" to 10, "SMC" to 20, "Volume" to 15),
                    underlyingSignal = ConfluenceSignal(
                        symbol = symbol,
                        timeframe = _uiState.value.selectedTimeframe,
                        higherTimeframeBias = MarketBias.BULLISH,
                        confidenceScore = 0.89,
                        smcSignal = "Order Block",
                        explanation = SignalExplanation("Study", emptyList()),
                        versions = emptyMap(),
                        integrityHash = "mock_hash"
                    )
                ),
                versions = emptyMap(),
                explanation = SignalExplanation("Institutional Setup", emptyList())
            )

            _uiState.update { it.copy(
                isLoading = false, 
                manualScanResults = listOf(mockSetup) + it.manualScanResults.take(4)
            ) }
        }
    }
}
