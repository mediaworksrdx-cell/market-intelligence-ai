package com.example.marketintelligence.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.TrainingModule
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.engine.MentorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EngineInitStatus {
    IDLE,
    INITIALIZING,
    READY,
    FAILED
}

enum class AcademyEngineType {
    INSTITUTIONAL_SIMULATION,
    NIFTY_50_DYNAMICS,
    BEHAVIORAL_BIAS,
    RISK_DISCIPLINE,
    GENERAL
}

fun TrainingModule.toEngineType(): AcademyEngineType = when {
    id == "1" || title.contains("Simulation", ignoreCase = true) -> AcademyEngineType.INSTITUTIONAL_SIMULATION
    id == "2" || title.contains("Nifty", ignoreCase = true) -> AcademyEngineType.NIFTY_50_DYNAMICS
    id == "3" || title.contains("Bias", ignoreCase = true) -> AcademyEngineType.BEHAVIORAL_BIAS
    id == "4" || title.contains("Risk", ignoreCase = true) -> AcademyEngineType.RISK_DISCIPLINE
    else -> AcademyEngineType.GENERAL
}

data class NiftyConstituentItem(
    val name: String,
    val sector: String,
    val weight: Double,
    val currentChangePct: Double,
    val isSimulatedBullish: Boolean = true
)

fun defaultNiftyConstituents(): List<NiftyConstituentItem> = listOf(
    NiftyConstituentItem("HDFC Bank", "BANKING", 13.5, 1.20, true),
    NiftyConstituentItem("Reliance Ind", "OIL & GAS", 9.8, 0.65, true),
    NiftyConstituentItem("ICICI Bank", "BANKING", 8.2, 1.45, true),
    NiftyConstituentItem("Infosys", "IT", 5.9, -0.40, false),
    NiftyConstituentItem("TCS", "IT", 4.1, 0.15, true),
    NiftyConstituentItem("Larsen & Toubro", "INFRA", 3.8, 0.90, true),
    NiftyConstituentItem("Kotak Bank", "BANKING", 3.4, 0.50, true),
    NiftyConstituentItem("Axis Bank", "BANKING", 3.2, 1.10, true)
)

data class AcademyUiState(
    val selectedMarket: MarketType = MarketType.IN,
    val lastFnoSignal: AiTradeSignal? = null,
    val mentorExplanation: String? = null,
    val isLoading: Boolean = false,
    val modules: List<TrainingModule> = emptyList(),
    val chatHistory: List<ChatMessage> = listOf(
        ChatMessage("Aarka AI", "Welcome to Aarka AI Cognitive Mentor. Ask any question regarding market structure, institutional liquidity, technical studies, or company fundamentals.", true)
    ),
    val currentInput: String = "",
    val suggestedQuestions: List<String> = emptyList(),
    val simulationMode: Boolean = false,
    val selectedModule: TrainingModule? = null,
    val isModuleInitialized: Boolean = false,
    val simulationPnl: Double = 12450.0,
    val simulationStep: Int = 1,
    val simulationMessage: String = "Simulating real-time market replay for NIFTY 50. Gamma hedge active.",
    val selectedSignalAnalysis: AiTradeSignal? = null,
    val selectedScannerSignal: com.example.marketintelligence.domain.engine.ScannerSignalInfo? = null,
    val isSignalDetailOpen: Boolean = false,
    val activeScannerSignals: List<com.example.marketintelligence.domain.engine.ScannerSignalInfo> = emptyList(),

    // --- Interactive Engine Lifecycle & Diagnostics ---
    val engineInitStatus: EngineInitStatus = EngineInitStatus.IDLE,
    val engineInitProgress: Float = 0f,
    val engineInitStepMessage: String = "",
    val engineInitError: String? = null,
    val initializedModuleId: String? = null,
    val initializedEngineType: AcademyEngineType? = null,

    // 1. Institutional Simulation Engine State
    val simScenario: String = "NIFTY 50 Expiry Day Gamma Squeeze Replay",
    val simSpot: Double = 23824.50,
    val simHistory: List<String> = listOf("Scenario mounted: Spot 23,824.50. Position: Bull Call Spread 23800/23950 CE."),
    val simGreeks: com.example.marketintelligence.domain.model.Greeks = com.example.marketintelligence.domain.model.Greeks(0.28, 0.014, -380.0, 240.0),

    // 2. NIFTY 50 Dynamics Engine State
    val niftySpot: Double = 23820.50,
    val niftyBreadth: String = "34 ADV / 16 DEC",
    val niftyFiiLongPct: Double = 64.2,
    val niftyConstituents: List<NiftyConstituentItem> = defaultNiftyConstituents(),
    val niftySectorFilter: String = "ALL",
    val niftyProjectedMove: Double = 0.0,
    val niftyDrillAnswer: Int? = null,

    // 3. Behavioral Bias Detection Engine State
    val biasDisciplineScore: Int = 88,
    val biasEmotionShield: Boolean = true,
    val biasCooldownSeconds: Int = 0,
    val neutralizedBiases: Set<String> = emptySet(),
    val biasDrillAnswer: Int? = null,

    // 4. Risk Discipline Engine State
    val riskCapital: Double = 500000.0,
    val riskPercent: Double = 1.0,
    val riskEntry: Double = 23800.0,
    val riskStopLoss: Double = 23720.0,
    val riskMaxDrawdownPct: Double = 3.0,
    val riskCurrentDrawdown: Double = 0.0,
    val riskCircuitBreakerHalted: Boolean = false,
    val riskConsecutiveLossCount: Int = 0
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
    private val engineRouter: EngineRouter,
    private val aarkaAiEngine: com.example.marketintelligence.data.engine.AarkaAiMentorEngineImpl? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 1. Seed initial signal immediately for zero-delay live training
        val defaultSignal = AiTradeSignal(
            underlyingSymbol = "NIFTY 50",
            strategyName = "Bull Call Spread",
            marketRegime = com.example.marketintelligence.data.model.MarketRegime.TRENDING_BULLISH,
            optionLegs = listOf(
                com.example.marketintelligence.data.model.OptionLeg("NIFTY 23600 CE", com.example.marketintelligence.data.model.OptionType.CE, 23600.0, "CURRENT", com.example.marketintelligence.data.model.TradeAction.BUY, 50, 142.0),
                com.example.marketintelligence.data.model.OptionLeg("NIFTY 23750 CE", com.example.marketintelligence.data.model.OptionType.CE, 23750.0, "CURRENT", com.example.marketintelligence.data.model.TradeAction.SELL, 50, 68.0)
            ),
            entryPrice = 74.0,
            target = 145.0,
            stopLoss = 38.0,
            confidenceScore = 86.5,
            riskParameters = com.example.marketintelligence.data.model.RiskParameters(
                positionSize = 1,
                capitalRequired = 3700.0,
                marginRequired = 18500.0,
                maxLoss = 3700.0
            )
        )
        viewModelScope.launch {
            val mentor = aarkaAiEngine ?: engineRouter.activeMentorEngine.firstOrNull()
            val explanation = mentor?.explainSignal(defaultSignal) ?: "Aarka AI Institutional Framework active."
            _uiState.update { 
                if (it.lastFnoSignal == null) {
                    it.copy(lastFnoSignal = defaultSignal, mentorExplanation = explanation)
                } else it
            }
        }

        // 2. Listen for Live Signals from F&O Engine
        intelligenceBus.fnoSignalFlow
            .onEach { signal ->
                val mentor = aarkaAiEngine ?: engineRouter.activeMentorEngine.firstOrNull()
                val explanation = mentor?.explainSignal(signal) ?: "Institutional strategy analysis active."
                _uiState.update { it.copy(lastFnoSignal = signal, mentorExplanation = explanation) }
            }
            .launchIn(viewModelScope)

        // 3. Listen for Confluence Scanner Signals
        intelligenceBus.scannerSignals
            .onEach { signals ->
                _uiState.update { it.copy(activeScannerSignals = signals) }
            }
            .launchIn(viewModelScope)

        // 2. Listen for Global Market Context Switch
        settingsRepository.selectedMarket
            .onEach { market ->
                val modules = when(market) {
                    MarketType.IN -> listOf(
                        TrainingModule("1", "Institutional Simulation Engine", "2h", false, listOf("Market Intelligence Theory", "Execution", "Historical Replay", "Order Flow Drills")),
                        TrainingModule("2", "NIFTY 50 Dynamics", "1h 30m", false, listOf("Index Correlation", "SMC & Liquidity Pools", "Heavyweight Weightings", "FII Futures Flow")),
                        TrainingModule("3", "Behavioral Bias Detection", "1h", false, listOf("Retail Traps", "Loss Aversion", "Volume Analysis", "Discipline Scoring")),
                        TrainingModule("4", "Risk Discipline Engine", "2h", false, listOf("1% Capital Rule", "Position Sizing", "Drawdown Kill-Switch", "MAE / R:R Matrix"))
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
        val alreadyInitializedForThis = _uiState.value.initializedModuleId == module.id && 
                                        _uiState.value.engineInitStatus == EngineInitStatus.READY
        _uiState.update { 
            it.copy(
                selectedModule = module,
                isModuleInitialized = alreadyInitializedForThis,
                engineInitStatus = if (alreadyInitializedForThis) EngineInitStatus.READY else EngineInitStatus.IDLE,
                engineInitProgress = if (alreadyInitializedForThis) 1.0f else 0.0f,
                engineInitStepMessage = if (alreadyInitializedForThis) "Engine Online & Synchronized" else "",
                engineInitError = null
            ) 
        }
    }

    fun initializeModule(module: TrainingModule, forceFail: Boolean = false) {
        if (_uiState.value.engineInitStatus == EngineInitStatus.INITIALIZING) return

        val engineType = module.toEngineType()
        _uiState.update { 
            it.copy(
                selectedModule = module,
                isModuleInitialized = false,
                engineInitStatus = EngineInitStatus.INITIALIZING,
                engineInitProgress = 0.15f,
                engineInitStepMessage = "Bootstrapping ${module.title} runtime...",
                engineInitError = null
            ) 
        }

        viewModelScope.launch {
            val stages = when (engineType) {
                AcademyEngineType.INSTITUTIONAL_SIMULATION -> listOf(
                    0.35f to "Connecting to Institutional Replay Cache...",
                    0.65f to "Mounting NIFTY 50 Order Book Feeds & Delta Neutrality...",
                    0.85f to "Calibrating Gamma Hedging & Greeks Calculator...",
                    1.0f to "Institutional Simulation Engine Online & Ready."
                )
                AcademyEngineType.NIFTY_50_DYNAMICS -> listOf(
                    0.35f to "Querying NIFTY 50 Constituent Weights & Sectoral Beta...",
                    0.70f to "Mapping Smart Money (SMC) Liquidity Pools & Fair Value Gaps...",
                    0.90f to "Synchronizing FII Futures Flow & PCR Wall...",
                    1.0f to "NIFTY 50 Dynamics Engine Online & Ready."
                )
                AcademyEngineType.BEHAVIORAL_BIAS -> listOf(
                    0.35f to "Scanning Trade Execution Logs for Cognitive Distortions...",
                    0.70f to "Calibrating Cognitive Biases Matrix & Emotion Shield...",
                    0.90f to "Evaluating Revenge Trading Cooldown Thresholds...",
                    1.0f to "Behavioral Bias Detection Engine Online (Score: 88/100)."
                )
                AcademyEngineType.RISK_DISCIPLINE -> listOf(
                    0.35f to "Binding Mathematical 1% Capital Allocation Mandate...",
                    0.70f to "Calibrating Dynamic Position Sizing Matrix & Lot Steps...",
                    0.90f to "Arming Daily Drawdown Circuit Breaker (Halt at 3.0%)...",
                    1.0f to "Risk Discipline Engine Online & Armed."
                )
                AcademyEngineType.GENERAL -> listOf(
                    0.50f to "Mounting Institutional Learning Subsystem...",
                    1.0f to "${module.title} Online & Ready."
                )
            }

            for ((progress, message) in stages) {
                kotlinx.coroutines.delay(60L)
                _uiState.update { 
                    it.copy(
                        engineInitProgress = progress,
                        engineInitStepMessage = message
                    ) 
                }
            }

            if (forceFail) {
                _uiState.update { 
                    it.copy(
                        engineInitStatus = EngineInitStatus.FAILED,
                        engineInitError = "Failed to initialize ${module.title}. Telemetry socket timed out. Tap retry.",
                        isModuleInitialized = false
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        engineInitStatus = EngineInitStatus.READY,
                        isModuleInitialized = true,
                        initializedModuleId = module.id,
                        initializedEngineType = engineType,
                        engineInitProgress = 1.0f,
                        engineInitError = null,
                        simulationStep = 1,
                        simulationPnl = 12450.0,
                        simulationMessage = "Initialized: ${module.title}. Institutional environment is live."
                    ) 
                }
            }
        }
    }

    fun retryInitializeModule(module: TrainingModule) {
        initializeModule(module, forceFail = false)
    }

    fun failModuleInitializationForTesting(module: TrainingModule, reason: String? = null) {
        _uiState.update {
            it.copy(
                selectedModule = module,
                engineInitStatus = EngineInitStatus.FAILED,
                engineInitError = reason ?: "Failed to initialize ${module.title}.",
                isModuleInitialized = false
            )
        }
    }

    fun closeModule() {
        _uiState.update { it.copy(selectedModule = null) }
    }

    // --- 1. Simulation Engine Interactive Controls ---
    fun executeSimAction(action: String) {
        _uiState.update { current ->
            when (action) {
                "HEDGE" -> {
                    val newPnl = current.simulationPnl + 1850.0
                    val newGreeks = current.simGreeks.copy(
                        delta = (current.simGreeks.delta * 0.36).coerceAtLeast(0.05),
                        gamma = current.simGreeks.gamma * 0.5
                    )
                    current.copy(
                        simulationPnl = newPnl,
                        simulationStep = (current.simulationStep + 1).coerceAtMost(4),
                        simulationMessage = "Protective Put Leg deployed. Gamma risk reduced by 64%. P&L: +₹${newPnl.toInt()}.",
                        simGreeks = newGreeks,
                        simHistory = current.simHistory + "Step ${current.simulationStep}: Hedge Deployed (+₹1,850)"
                    )
                }
                "ROLL" -> {
                    val newPnl = current.simulationPnl + 3200.0
                    current.copy(
                        simulationPnl = newPnl,
                        simulationStep = (current.simulationStep + 1).coerceAtMost(4),
                        simulationMessage = "Strikes rolled to 23,900 CE. Profit locked at ₹${newPnl.toInt()}.",
                        simHistory = current.simHistory + "Step ${current.simulationStep}: Strikes Rolled to 23900 CE (+₹3,200)"
                    )
                }
                "SQUARE" -> current.copy(
                    simulationStep = 4,
                    simulationMessage = "Position squared off. Discipline Score: 96/100 • Return on Capital: +21.4% • P&L: +₹${current.simulationPnl.toInt()}.",
                    simHistory = current.simHistory + "Final: Position Squared Off (+₹${current.simulationPnl.toInt()})"
                )
                "RESET" -> current.copy(
                    simulationStep = 1,
                    simulationPnl = 12450.0,
                    simulationMessage = "Simulation scenario reset to tick 1.",
                    simGreeks = com.example.marketintelligence.domain.model.Greeks(0.28, 0.014, -380.0, 240.0),
                    simHistory = listOf("Scenario reset: Spot 23,824.50. Position: Bull Call Spread 23800/23950 CE.")
                )
                else -> {
                    val spotDrift = (Math.random() * 20 - 8)
                    val newSpot = current.simSpot + spotDrift
                    val pnlDelta = spotDrift * 35.0
                    val newPnl = current.simulationPnl + pnlDelta
                    current.copy(
                        simSpot = newSpot,
                        simulationPnl = newPnl,
                        simulationStep = (current.simulationStep + 1).coerceAtMost(4),
                        simulationMessage = "Advanced tick: Spot ${String.format(java.util.Locale.US, "%.2f", newSpot)}. Volatility expansion detected.",
                        simHistory = current.simHistory + "Tick advanced: Spot -> ${String.format(java.util.Locale.US, "%.2f", newSpot)}"
                    )
                }
            }
        }
    }

    // --- 2. NIFTY 50 Dynamics Engine Interactive Controls ---
    fun setNiftySectorFilter(sector: String) {
        _uiState.update { it.copy(niftySectorFilter = sector) }
    }

    fun toggleNiftyConstituentBias(name: String) {
        _uiState.update { current ->
            val updated = current.niftyConstituents.map { item ->
                if (item.name.equals(name, ignoreCase = true)) {
                    item.copy(isSimulatedBullish = !item.isSimulatedBullish)
                } else item
            }
            val netWeightDelta = updated.sumOf { (if (it.isSimulatedBullish) 1.0 else -1.0) * it.weight }
            val projectedPoints = (netWeightDelta - 15.0) * 2.8
            current.copy(
                niftyConstituents = updated,
                niftyProjectedMove = projectedPoints
            )
        }
    }

    fun submitNiftyDrillAnswer(answerIndex: Int) {
        _uiState.update { it.copy(niftyDrillAnswer = answerIndex) }
    }

    // --- 3. Behavioral Bias Detection Engine Interactive Controls ---
    fun toggleEmotionShield() {
        _uiState.update { it.copy(biasEmotionShield = !it.biasEmotionShield) }
    }

    fun neutralizeBias(biasKey: String) {
        _uiState.update { current ->
            val newNeutralized = current.neutralizedBiases + biasKey
            val newScore = (current.biasDisciplineScore + 3).coerceAtMost(100)
            current.copy(
                neutralizedBiases = newNeutralized,
                biasDisciplineScore = newScore
            )
        }
    }

    fun activateRevengeCooldown() {
        _uiState.update { it.copy(biasCooldownSeconds = 1200) }
    }

    fun submitBiasDrillAnswer(answerIndex: Int) {
        _uiState.update { it.copy(biasDrillAnswer = answerIndex) }
    }

    // --- 4. Risk Discipline Engine Interactive Controls ---
    fun updateRiskParameters(capital: Double?, riskPct: Double?, entry: Double?, sl: Double?) {
        _uiState.update { current ->
            current.copy(
                riskCapital = capital ?: current.riskCapital,
                riskPercent = riskPct ?: current.riskPercent,
                riskEntry = entry ?: current.riskEntry,
                riskStopLoss = sl ?: current.riskStopLoss
            )
        }
    }

    fun simulateDrawdownLoss() {
        _uiState.update { current ->
            val newLossCount = current.riskConsecutiveLossCount + 1
            val newDrawdown = (current.riskCurrentDrawdown + 1.1).coerceAtMost(10.0)
            val halted = newDrawdown >= current.riskMaxDrawdownPct
            current.copy(
                riskConsecutiveLossCount = newLossCount,
                riskCurrentDrawdown = newDrawdown,
                riskCircuitBreakerHalted = halted
            )
        }
    }

    fun resetDrawdownCircuitBreaker() {
        _uiState.update { current ->
            current.copy(
                riskCurrentDrawdown = 0.0,
                riskConsecutiveLossCount = 0,
                riskCircuitBreakerHalted = false
            )
        }
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
            
            // Aarka AI answers dynamically without template fallback
            val mentor = aarkaAiEngine ?: engineRouter.activeMentorEngine.firstOrNull()
            val response = mentor?.ask(message) ?: "Aarka AI is currently analyzing real-time institutional flow. Please retry."
            
            _uiState.update { it.copy(
                chatHistory = it.chatHistory + ChatMessage("Aarka AI", response, true),
                isLoading = false
            ) }
        }
    }

    fun openSignalAnalysis(signal: AiTradeSignal) {
        _uiState.update {
            it.copy(
                selectedSignalAnalysis = signal,
                selectedScannerSignal = null,
                isSignalDetailOpen = true
            )
        }
    }

    fun openScannerSignalAnalysis(signal: com.example.marketintelligence.domain.engine.ScannerSignalInfo) {
        _uiState.update {
            it.copy(
                selectedScannerSignal = signal,
                selectedSignalAnalysis = null,
                isSignalDetailOpen = true
            )
        }
    }

    fun closeSignalAnalysis() {
        _uiState.update {
            it.copy(
                selectedSignalAnalysis = null,
                selectedScannerSignal = null,
                isSignalDetailOpen = false
            )
        }
    }
}
