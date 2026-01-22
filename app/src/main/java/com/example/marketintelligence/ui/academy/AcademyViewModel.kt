package com.example.marketintelligence.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AcademyUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val lastFnoSignal: AiTradeSignal? = null,
    val mentorExplanation: String? = null,
    val isLoading: Boolean = false,
    val recommendedModules: List<String> = emptyList(),
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("Mentor", "Welcome to REDX AI Mentor. How can I assist your market study today?", true)
    ),
    val currentInput: String = ""
)

data class ChatMessage(
    val sender: String,
    val message: String,
    val isMentor: Boolean
)

@HiltViewModel
class AcademyViewModel @Inject constructor(
    private val intelligenceBus: LiveIntelligenceBus,
    private val settingsRepository: SettingsRepository,
    private val engineRouter: EngineRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 1. Listen for Live Signals
        intelligenceBus.fnoSignalFlow
            .onEach { signal ->
                val activeMentor = engineRouter.activeMentorEngine.first()
                val explanation = activeMentor.explainSignal(signal)
                _uiState.update { it.copy(lastFnoSignal = signal, mentorExplanation = explanation) }
            }
            .launchIn(viewModelScope)

        // 2. Listen for Global Market Context Switch
        settingsRepository.selectedMarket
            .onEach { market ->
                val modules = when(market) {
                    MarketType.IN -> listOf("Institutional Simulation Engine", "NIFTY 50 Dynamics", "Behavioral Bias Detection", "Risk Discipline Engine")
                    MarketType.US -> listOf("Institutional Playbook Library", "Fed Policy Impact", "Cross-Asset Context", "Scenario Stress Testing")
                    MarketType.UAE -> listOf("Market Regime Intelligence", "Oil & Real Estate Correlation", "Governance & Audit", "Outcome Attribution")
                }
                _uiState.update { 
                    it.copy(
                        selectedMarket = market,
                        recommendedModules = modules
                    ) 
                }
            }
            .launchIn(viewModelScope)
    }

    fun onInputChanged(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun sendMessage() {
        val message = _uiState.value.currentInput
        if (message.isBlank()) return

        val userMsg = ChatMessage("User", message, false)
        _uiState.update { it.copy(
            chatHistory = it.chatHistory + userMsg,
            currentInput = ""
        ) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Simulate AI Mentor Response
            kotlinx.coroutines.delay(1000)
            val activeMentor = engineRouter.activeMentorEngine.first()
            // In real app: activeMentor.ask(message)
            val response = "Based on institutional SMC theory, the pattern you mentioned suggests a liquidity sweep. Review the 'Liquidity Grab' module for deeper insight."
            
            _uiState.update { it.copy(
                chatHistory = it.chatHistory + ChatMessage("Mentor", response, true),
                isLoading = false
            ) }
        }
    }
}
