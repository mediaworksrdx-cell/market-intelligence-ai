package com.marketintelligence.cryptotracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.cryptotracker.engine.*
import com.marketintelligence.cryptotracker.scanner.CryptoMarketScanner
import com.marketintelligence.cryptotracker.scanner.MarketOverview
import com.marketintelligence.cryptotracker.scanner.ScanResult
import com.marketintelligence.tradeengine.models.Candle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class CryptoScannerViewModel @Inject constructor(
    private val scanner: CryptoMarketScanner
) : ViewModel() {

    private val _scannerState = MutableStateFlow(CryptoScannerUiState())
    val scannerState: StateFlow<CryptoScannerUiState> = _scannerState.asStateFlow()

    private val _detailState = MutableStateFlow(CoinDetailUiState())
    val detailState: StateFlow<CoinDetailUiState> = _detailState.asStateFlow()

    private val trackedSymbols = CryptoMarketScanner.DEFAULT_SYMBOLS

    init {
        refresh()
    }

    /**
     * Refreshes the market overview by scanning the configured symbols.
     */
    fun refresh() {
        _scannerState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                // Simulate network delay for scan
                delay(600)
                
                val timeframe = _scannerState.value.selectedTimeframe
                val candleDataMap = mutableMapOf<String, Map<String, List<Candle>>>()
                
                withContext(Dispatchers.Default) {
                    for (symbol in trackedSymbols) {
                        val htfCandles = generateMockCandles(symbol, "4h", 200)
                        val mtfCandles = generateMockCandles(symbol, timeframe, 300)
                        val ltfCandles = generateMockCandles(symbol, "5m", 100)
                        
                        candleDataMap[symbol] = mapOf(
                            "4h" to htfCandles,
                            timeframe to mtfCandles,
                            "5m" to ltfCandles
                        )
                    }
                }
                
                val overview = scanner.scanAll(candleDataMap)
                
                _scannerState.update { 
                    it.copy(
                        isLoading = false,
                        overview = overview,
                        lastRefreshTime = System.currentTimeMillis()
                    ) 
                }
            } catch (e: Exception) {
                _scannerState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Scan error occurred") }
            }
        }
    }

    /**
     * Updates the current timeframe and refreshes the scan.
     */
    fun setTimeframe(tf: String) {
        if (_scannerState.value.selectedTimeframe == tf) return
        _scannerState.update { it.copy(selectedTimeframe = tf) }
        refresh()
    }

    /**
     * Sets the signal filter without requiring a rescan.
     */
    fun setFilter(filter: SignalFilter) {
        _scannerState.update { it.copy(selectedFilter = filter) }
    }

    /**
     * Selects a coin and updates the detail UI state.
     */
    fun selectCoin(symbol: String) {
        _detailState.update { it.copy(isLoading = true, symbol = symbol, error = null) }
        
        viewModelScope.launch {
            val overview = _scannerState.value.overview
            val result = overview?.scanResults?.find { it.symbol == symbol }
            
            if (result != null) {
                val mockMultiTfRsi = mapOf(
                    "5m"  to Random.nextDouble(32.0, 68.0),
                    "15m" to result.rsiValue,
                    "1H"  to Random.nextDouble(38.0, 62.0),
                    "4H"  to Random.nextDouble(42.0, 58.0)
                )

                val candles = generateMockCandles(symbol, "15m", 120)
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        symbol = symbol,
                        currentPrice = result.setup?.entry ?: result.lastBOS?.price ?: 100.0,
                        candles = candles,
                        setup = result.setup,
                        smcAnalysis = result.setup?.smcAnalysis,
                        fvgs = result.activeFVG?.let { fvg -> listOf(fvg) } ?: emptyList(),
                        rsiAnalysis = result.setup?.rsiAnalysis,
                        multiTfRSI = mockMultiTfRsi
                    )
                }
            } else {
                _detailState.update { 
                    it.copy(isLoading = false, error = "Details not found for $symbol") 
                }
            }
        }
    }

    /**
     * Helper to generate realistic OHLCV candle data for demonstration.
     */
    private fun generateMockCandles(symbol: String, timeframe: String, count: Int): List<Candle> {
        val basePrice = when (symbol) {
            "BTCUSDT" -> 95000.0
            "ETHUSDT" -> 3500.0
            "SOLUSDT" -> 180.0
            "BNBUSDT" -> 600.0
            "XRPUSDT" -> 1.05
            "ADAUSDT" -> 0.75
            "DOGEUSDT" -> 0.22
            "AVAXUSDT" -> 32.0
            "LINKUSDT" -> 18.5
            "SUIUSDT" -> 3.2
            else -> 100.0
        }
        
        val tfMillis = when (timeframe) {
            "1m" -> 60 * 1000L
            "5m" -> 5 * 60 * 1000L
            "15m" -> 15 * 60 * 1000L
            "1h", "1H" -> 60 * 60 * 1000L
            "4h", "4H" -> 4 * 60 * 60 * 1000L
            else -> 15 * 60 * 1000L
        }

        var currentClose = basePrice
        var time = System.currentTimeMillis() - (count * tfMillis)
        
        return (0 until count).map {
            val open = currentClose
            val change = (Random.nextDouble() - 0.48) * 0.008 
            val close = open * (1 + change)
            val high = maxOf(open, close) * (1 + Random.nextDouble() * 0.003)
            val low = minOf(open, close) * (1 - Random.nextDouble() * 0.003)
            val volume = 200.0 + Random.nextDouble() * 15000.0
            
            currentClose = close
            time += tfMillis
            
            Candle(
                symbol = symbol,
                timeframe = timeframe,
                openTime = time,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = volume,
                closeTime = time + tfMillis,
                isClosed = true
            )
        }
    }
}
