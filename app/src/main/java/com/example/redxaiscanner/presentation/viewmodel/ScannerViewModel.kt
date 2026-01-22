package com.example.redxaiscanner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.redxaiscanner.domain.usecase.GenerateTradeSetupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// @HiltViewModel
class ScannerViewModel /* @Inject constructor */(
    // In a real app, this would be injected by Hilt
    private val generateTradeSetupUseCase: GenerateTradeSetupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Starts a scan for a predefined list of symbols.
     * It updates the UI state to show a loading indicator, then populates the state
     * with the final results from the entire analysis pipeline.
     */
    fun startScan(symbols: List<String>) {
        viewModelScope.launch {
            generateTradeSetupUseCase.getTradeSetups(symbols)
                .onStart {
                    // Set loading state at the beginning of the flow
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { e ->
                    // Handle any errors from the pipeline
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    e.printStackTrace()
                }
                .collect { setups ->
                    // Update state with the final, actionable results
                    _uiState.update {
                        it.copy(isLoading = false, tradeSetups = setups)
                    }
                }
        }
    }
}