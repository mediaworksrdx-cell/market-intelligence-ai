package com.example.marketintelligence.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.TrainingModule
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
    val modules: List<TrainingModule> = emptyList(),
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("Mentor", "Welcome to REDX AI Mentor. How can I assist your market study today?", true)
    ),
    val currentInput: String = "",
    val suggestedQuestions: List<String> = listOf(
        "What is a Liquidity Sweep?",
        "Explain Order Blocks.",
        "How to identify Market Structure Break?"
    ),
    val simulationMode: Boolean = false,
    val selectedModule: TrainingModule? = null
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
                val activeMentor = engineRouter.activeMentorEngine.firstOrNull()
                val explanation = activeMentor?.explainSignal(signal)
                _uiState.update { it.copy(lastFnoSignal = signal, mentorExplanation = explanation) }
            }
            .launchIn(viewModelScope)

        // 2. Listen for Global Market Context Switch
        settingsRepository.selectedMarket
            .onEach { market ->
                val modules = when(market) {
                    MarketType.IN -> listOf(
                        TrainingModule("1", "Institutional Simulation Engine", "2h", false, listOf("RedX Theory", "Execution")),
                        TrainingModule("2", "NIFTY 50 Dynamics", "1h 30m", false, listOf("Index Correlation", "SMC")),
                        TrainingModule("3", "Behavioral Bias Detection", "1h", false, listOf("Retail Traps", "Volume Analysis")),
                        TrainingModule("4", "Risk Discipline Engine", "2h", true, listOf("Institutional Control"))
                    )
                    MarketType.US -> listOf(
                        TrainingModule("5", "Institutional Playbook Library", "3h", false, listOf("Wall St Tactics", "Dark Pools")),
                        TrainingModule("6", "Fed Policy Impact", "2h", false, listOf("Macro Context", "Interest Rates")),
                        TrainingModule("7", "Cross-Asset Context", "1h 30m", false, listOf("DXY Correlation", "Yields")),
                        TrainingModule("8", "Scenario Stress Testing", "4h", true, listOf("Black Swan Prep"))
                    )
                    MarketType.UAE -> listOf(
                        TrainingModule("9", "Market Regime Intelligence", "1h 30m", false, listOf("Volatility Index", "ADX")),
                        TrainingModule("10", "Oil & Real Estate Correlation", "2h", false, listOf("Sector Flow", "Macro UAE")),
                        TrainingModule("11", "Governance & Audit", "1h", false, listOf("Audit Layer", "Risk Protocol")),
                        TrainingModule("12", "Outcome Attribution", "2h", true, listOf("Performance Metrics"))
                    )
                }
                _uiState.update { 
                    it.copy(
                        selectedMarket = market,
                        modules = modules
                    ) 
                }
            }
            .launchIn(viewModelScope)
    }

    fun onInputChanged(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun selectModule(module: TrainingModule) {
        if (!module.locked) {
            _uiState.update { it.copy(selectedModule = module) }
        }
    }

    fun closeModule() {
        _uiState.update { it.copy(selectedModule = null) }
    }

    fun toggleSimulation() {
        _uiState.update { it.copy(simulationMode = !it.simulationMode) }
    }

    fun sendMessage(input: String? = null) {
        val message = input ?: _uiState.value.currentInput
        if (message.isBlank()) return

        val userMsg = ChatMessage("User", message, false)
        _uiState.update { it.copy(
            chatHistory = it.chatHistory + userMsg,
            currentInput = ""
        ) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Get response from active mentor engine
            val activeMentor = engineRouter.activeMentorEngine.firstOrNull()
            val response = activeMentor?.ask(message) ?: "Mentor engine is currently unavailable."
            
            _uiState.update { it.copy(
                chatHistory = it.chatHistory + ChatMessage("Mentor", response, true),
                isLoading = false
            ) }
        }
    }
}
