package com.marketintelligence.redxchartlibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.redxchartlibrary.data.MarketDataClient
import com.marketintelligence.redxchartlibrary.data.local.Drawing
import com.marketintelligence.redxchartlibrary.data.local.DrawingRepository
import com.marketintelligence.redxchartlibrary.model.Candle
import com.marketintelligence.redxchartlibrary.state.ChartState
import com.marketintelligence.redxchartlibrary.util.TimeFrame
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartUiState(
    val candles: List<Candle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val marketDataClient: MarketDataClient,
    private val drawingRepository: DrawingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState = _uiState.asStateFlow()
    
    val chartState = ChartState()

    fun loadChart(symbol: String, timeframe: TimeFrame) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Fetch historical candles
            marketDataClient.fetchHistoricalCandles(symbol, timeframe)

            // 2. Load historical drawings
            drawingRepository.getDrawingsForChart(symbol, timeframe.identifier).collect { drawings ->
                chartState.drawings.value = drawings
            }

            // 3. Get historical candles and combine with forming candle
            marketDataClient.getHistoricalCandles(symbol, timeframe.identifier)
                .combine(marketDataClient.formingCandleFlow) { historical, forming ->
                    val combined = historical.toMutableList()
                    val formingIndex = combined.indexOfFirst { it.openTime == forming.openTime }

                    if (formingIndex != -1) {
                        combined[formingIndex] = forming
                    } else {
                        combined.add(forming)
                    }
                    combined
                }
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { candles ->
                    _uiState.update { it.copy(candles = candles, isLoading = false) }
                }
        }

        // 4. Start the live stream for new ticks
        marketDataClient.startLiveStream(symbol, timeframe)
    }

    fun saveDrawing(drawing: Drawing) {
        viewModelScope.launch {
            drawingRepository.insertDrawing(drawing)
        }
    }

    fun deleteDrawing(drawing: Drawing) {
        viewModelScope.launch {
            drawingRepository.deleteDrawing(drawing.id)
        }
    }
    
    override fun onCleared() {
        marketDataClient.stopLiveStream()
        super.onCleared()
    }
}
