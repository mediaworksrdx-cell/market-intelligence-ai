package com.example.marketintelligence.ui.main

import androidx.lifecycle.ViewModel
import com.example.marketintelligence.domain.model.MarketType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GlobalSettingsUiState(
    val selectedMarket: MarketType = MarketType.IN
)

@HiltViewModel
class GlobalSettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GlobalSettingsUiState())
    val uiState = _uiState.asStateFlow()
}
