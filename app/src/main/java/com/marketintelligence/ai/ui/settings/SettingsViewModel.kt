package com.marketintelligence.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val selectedScannerEngine: String = "Standard (Gemini)",
    val selectedMentorEngine: String = "Standard (Gemini)",
    val selectedChartEngine: String = "Proprietary Engine",
    val selectedMarket: MarketType = MarketType.IN,
    val isDarkMode: Boolean = true,
    val riskProfile: String = "CONSERVATIVE",
    val isNotificationsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        combine(
            settingsRepository.selectedScannerEngine,
            settingsRepository.selectedMentorEngine,
            settingsRepository.selectedChartEngine,
            settingsRepository.selectedMarket,
            settingsRepository.isDarkMode,
            settingsRepository.riskProfile,
            settingsRepository.isNotificationsEnabled
        ) { args ->
            SettingsUiState(
                selectedScannerEngine = args[0] as String,
                selectedMentorEngine = args[1] as String,
                selectedChartEngine = args[2] as String,
                selectedMarket = args[3] as MarketType,
                isDarkMode = args[4] as Boolean,
                riskProfile = args[5] as String,
                isNotificationsEnabled = args[6] as Boolean
            )
        }.onEach { _uiState.value = it }
        .launchIn(viewModelScope)
    }

    fun setScannerEngine(name: String) = viewModelScope.launch { settingsRepository.setScannerEngine(name) }
    fun setMentorEngine(name: String) = viewModelScope.launch { settingsRepository.setMentorEngine(name) }
    fun setChartEngine(name: String) = viewModelScope.launch { settingsRepository.setChartEngine(name) }
    fun setMarket(market: MarketType) = viewModelScope.launch { settingsRepository.setMarket(market) }
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    fun setRiskProfile(profile: String) = viewModelScope.launch { settingsRepository.setRiskProfile(profile) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
}
