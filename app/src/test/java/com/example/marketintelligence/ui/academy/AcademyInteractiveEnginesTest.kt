package com.example.marketintelligence.ui.academy

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.*
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.tradeengine.models.Candle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for Academy Library Interactive Engines:
 *
 * 1. Institutional Simulation Engine (Module 1)
 * 2. NIFTY 50 Dynamics Engine (Module 2)
 * 3. Behavioral Bias Detection Engine (Module 3)
 * 4. Risk Discipline Engine (Module 4)
 *
 * Verifies:
 * - Module identification & engine type mapping.
 * - Initialization lifecycle: IDLE -> INITIALIZING -> READY with stages & progress.
 * - Failure handling & recovery retry flow (ensuring failed button is not normally functional).
 * - Interactive controls & mathematical models for each of the 4 engines.
 * - Strict module state isolation (preventing cross-contamination between modules).
 * - UI state readiness and button condition mapping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AcademyInteractiveEnginesTest {

    private val testDispatcher = StandardTestDispatcher()

    // Test Training Modules
    private val module1Sim = TrainingModule(
        id = "1",
        title = "Institutional Simulation Engine",
        duration = "2h",
        locked = false,
        topics = listOf("Market Intelligence Theory", "Execution", "Historical Replay", "Order Flow Drills")
    )

    private val module2Nifty = TrainingModule(
        id = "2",
        title = "NIFTY 50 Dynamics",
        duration = "1h 30m",
        locked = false,
        topics = listOf("Index Correlation", "SMC & Liquidity Pools", "Heavyweight Weightings", "FII Futures Flow")
    )

    private val module3Bias = TrainingModule(
        id = "3",
        title = "Behavioral Bias Detection",
        duration = "1h",
        locked = false,
        topics = listOf("Retail Traps", "Loss Aversion", "Volume Analysis", "Discipline Scoring")
    )

    private val module4Risk = TrainingModule(
        id = "4",
        title = "Risk Discipline Engine",
        duration = "2h",
        locked = false,
        topics = listOf("1% Capital Rule", "Position Sizing", "Drawdown Kill-Switch", "MAE / R:R Matrix")
    )

    private val moduleGeneral = TrainingModule(
        id = "5",
        title = "Institutional Playbook Library",
        duration = "3h",
        locked = false,
        topics = listOf("Wall St Tactics", "Dark Pools")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. MODULE IDENTIFICATION & ENGINE TYPE MAPPING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModuleEngineTypeMapping() {
        assertEquals(AcademyEngineType.INSTITUTIONAL_SIMULATION, module1Sim.toEngineType())
        assertEquals(AcademyEngineType.NIFTY_50_DYNAMICS, module2Nifty.toEngineType())
        assertEquals(AcademyEngineType.BEHAVIORAL_BIAS, module3Bias.toEngineType())
        assertEquals(AcademyEngineType.RISK_DISCIPLINE, module4Risk.toEngineType())
        assertEquals(AcademyEngineType.GENERAL, moduleGeneral.toEngineType())

        // Also test title-based fallback
        val customSim = TrainingModule("101", "Advanced Market Simulation", "1h", false, emptyList())
        val customNifty = TrainingModule("102", "Nifty Heavyweights Matrix", "1h", false, emptyList())
        val customBias = TrainingModule("103", "Cognitive Bias Workshop", "1h", false, emptyList())
        val customRisk = TrainingModule("104", "Risk Allocation Framework", "1h", false, emptyList())

        assertEquals(AcademyEngineType.INSTITUTIONAL_SIMULATION, customSim.toEngineType())
        assertEquals(AcademyEngineType.NIFTY_50_DYNAMICS, customNifty.toEngineType())
        assertEquals(AcademyEngineType.BEHAVIORAL_BIAS, customBias.toEngineType())
        assertEquals(AcademyEngineType.RISK_DISCIPLINE, customRisk.toEngineType())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MODULE 1: INSTITUTIONAL SIMULATION ENGINE INTERACTIVE VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModule1_SimulationEngineLifecycleAndActions() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // 1. Select Module 1
        viewModel.selectModule(module1Sim)
        var state = viewModel.uiState.value
        assertEquals(module1Sim, state.selectedModule)
        assertEquals(EngineInitStatus.IDLE, state.engineInitStatus)
        assertFalse(state.isModuleInitialized)
        assertNull(state.initializedModuleId)

        // 2. Initialize Module 1
        viewModel.initializeModule(module1Sim)
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.INITIALIZING, state.engineInitStatus)
        assertTrue(state.engineInitProgress >= 0.15f)
        assertFalse(state.isModuleInitialized)

        // Advance coroutine delay stages
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertTrue(state.isModuleInitialized)
        assertEquals("1", state.initializedModuleId)
        assertEquals(AcademyEngineType.INSTITUTIONAL_SIMULATION, state.initializedEngineType)
        assertEquals(1.0f, state.engineInitProgress, 0.001f)
        assertNull(state.engineInitError)

        // Check initial simulation state
        assertEquals(12450.0, state.simulationPnl, 0.001)
        assertEquals(1, state.simulationStep)
        assertEquals(0.28, state.simGreeks.delta, 0.001)
        assertEquals(0.014, state.simGreeks.gamma, 0.001)

        // 3. Test Interactive HEDGE action
        viewModel.executeSimAction("HEDGE")
        state = viewModel.uiState.value
        assertEquals(12450.0 + 1850.0, state.simulationPnl, 0.001)
        assertEquals(2, state.simulationStep)
        assertTrue(state.simGreeks.gamma < 0.014) // Gamma cut by 50%
        assertTrue(state.simulationMessage.contains("Protective Put Leg deployed"))
        assertTrue(state.simHistory.last().contains("Hedge Deployed"))

        // 4. Test Interactive ROLL action
        val pnlBeforeRoll = state.simulationPnl
        viewModel.executeSimAction("ROLL")
        state = viewModel.uiState.value
        assertEquals(pnlBeforeRoll + 3200.0, state.simulationPnl, 0.001)
        assertEquals(3, state.simulationStep)
        assertTrue(state.simulationMessage.contains("Strikes rolled"))
        assertTrue(state.simHistory.last().contains("Strikes Rolled to 23900 CE"))

        // 5. Test Interactive SQUARE action
        viewModel.executeSimAction("SQUARE")
        state = viewModel.uiState.value
        assertEquals(4, state.simulationStep)
        assertTrue(state.simulationMessage.contains("Position squared off"))
        assertTrue(state.simulationMessage.contains("Discipline Score: 96/100"))

        // 6. Test RESET action
        viewModel.executeSimAction("RESET")
        state = viewModel.uiState.value
        assertEquals(1, state.simulationStep)
        assertEquals(12450.0, state.simulationPnl, 0.001)
        assertEquals(0.28, state.simGreeks.delta, 0.001)
        assertEquals(0.014, state.simGreeks.gamma, 0.001)
        assertEquals(1, state.simHistory.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. MODULE 2: NIFTY 50 DYNAMICS ENGINE INTERACTIVE VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModule2_NiftyDynamicsLifecycleAndCalculations() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // 1. Select & Initialize Module 2
        viewModel.selectModule(module2Nifty)
        viewModel.initializeModule(module2Nifty)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertEquals("2", state.initializedModuleId)
        assertEquals(AcademyEngineType.NIFTY_50_DYNAMICS, state.initializedEngineType)

        // 2. Verify constituent matrix catalog
        val constituents = state.niftyConstituents
        assertTrue(constituents.size >= 8)
        val hdfc = constituents.find { it.name == "HDFC Bank" }
        assertNotNull(hdfc)
        assertEquals(13.5, hdfc!!.weight, 0.001)
        assertEquals("BANKING", hdfc.sector)

        val reliance = constituents.find { it.name == "Reliance Ind" }
        assertNotNull(reliance)
        assertEquals(9.8, reliance!!.weight, 0.001)

        // 3. Test Sector Filter
        viewModel.setNiftySectorFilter("BANKING")
        state = viewModel.uiState.value
        assertEquals("BANKING", state.niftySectorFilter)

        viewModel.setNiftySectorFilter("ALL")
        state = viewModel.uiState.value
        assertEquals("ALL", state.niftySectorFilter)

        // 4. Test Dynamic Constituent Bias Toggle & Point Impact Delta
        val initialProjected = state.niftyProjectedMove
        // Toggle HDFC Bank from Bullish to Bearish
        viewModel.toggleNiftyConstituentBias("HDFC Bank")
        state = viewModel.uiState.value
        val updatedHdfc = state.niftyConstituents.find { it.name == "HDFC Bank" }
        assertNotNull(updatedHdfc)
        assertFalse(updatedHdfc!!.isSimulatedBullish)
        // With HDFC flipping from +13.5 to -13.5, net delta drops by 27.0 points of weight
        // Projected move must shift downwards
        assertTrue("Projected move should decrease when HDFC Bank turns bearish", state.niftyProjectedMove < initialProjected)

        // Toggle Reliance Ind
        val moveAfterHdfc = state.niftyProjectedMove
        viewModel.toggleNiftyConstituentBias("Reliance Ind")
        state = viewModel.uiState.value
        val updatedReliance = state.niftyConstituents.find { it.name == "Reliance Ind" }
        assertNotNull(updatedReliance)
        assertFalse(updatedReliance!!.isSimulatedBullish)
        assertTrue("Projected move should further decrease when Reliance turns bearish", state.niftyProjectedMove < moveAfterHdfc)

        // 5. Test Interactive Drill Submission
        viewModel.submitNiftyDrillAnswer(2)
        state = viewModel.uiState.value
        assertEquals(2, state.niftyDrillAnswer)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. MODULE 3: BEHAVIORAL BIAS DETECTION ENGINE INTERACTIVE VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModule3_BehavioralBiasLifecycleAndScoring() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // 1. Select & Initialize Module 3
        viewModel.selectModule(module3Bias)
        viewModel.initializeModule(module3Bias)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertEquals("3", state.initializedModuleId)
        assertEquals(AcademyEngineType.BEHAVIORAL_BIAS, state.initializedEngineType)
        assertEquals(88, state.biasDisciplineScore)
        assertTrue(state.biasEmotionShield)
        assertTrue(state.neutralizedBiases.isEmpty())

        // 2. Test Emotion Shield Toggle
        viewModel.toggleEmotionShield()
        state = viewModel.uiState.value
        assertFalse(state.biasEmotionShield)

        viewModel.toggleEmotionShield()
        state = viewModel.uiState.value
        assertTrue(state.biasEmotionShield)

        // 3. Test Bias Neutralization & Score Progression
        viewModel.neutralizeBias("LOSS_AVERSION")
        state = viewModel.uiState.value
        assertTrue(state.neutralizedBiases.contains("LOSS_AVERSION"))
        assertEquals(91, state.biasDisciplineScore) // 88 + 3 = 91

        viewModel.neutralizeBias("FOMO_CHASE")
        state = viewModel.uiState.value
        assertTrue(state.neutralizedBiases.contains("FOMO_CHASE"))
        assertEquals(94, state.biasDisciplineScore) // 91 + 3 = 94

        viewModel.neutralizeBias("REVENGE_TRADING")
        state = viewModel.uiState.value
        assertEquals(97, state.biasDisciplineScore) // 94 + 3 = 97

        viewModel.neutralizeBias("DISPOSITION_EFFECT")
        state = viewModel.uiState.value
        assertEquals(100, state.biasDisciplineScore) // 97 + 3 = 100

        // Score capped at 100
        viewModel.neutralizeBias("OVERCONFIDENCE")
        state = viewModel.uiState.value
        assertEquals(100, state.biasDisciplineScore)

        // 4. Test Revenge Cooldown Trigger
        viewModel.activateRevengeCooldown()
        state = viewModel.uiState.value
        assertEquals(1200, state.biasCooldownSeconds)

        // 5. Test Drill Submission
        viewModel.submitBiasDrillAnswer(1)
        state = viewModel.uiState.value
        assertEquals(1, state.biasDrillAnswer)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. MODULE 4: RISK DISCIPLINE ENGINE INTERACTIVE VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModule4_RiskDisciplineLifecycleAndCircuitBreaker() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // 1. Select & Initialize Module 4
        viewModel.selectModule(module4Risk)
        viewModel.initializeModule(module4Risk)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertEquals("4", state.initializedModuleId)
        assertEquals(AcademyEngineType.RISK_DISCIPLINE, state.initializedEngineType)

        // 2. Verify Position Sizing Model
        // Default: Capital 500,000, Risk 1.0%, Entry 23800, SL 23720
        assertEquals(500000.0, state.riskCapital, 0.001)
        assertEquals(1.0, state.riskPercent, 0.001)
        assertEquals(23800.0, state.riskEntry, 0.001)
        assertEquals(23720.0, state.riskStopLoss, 0.001)

        val maxRiskAmount = state.riskCapital * (state.riskPercent / 100.0)
        val stopLossDistance = state.riskEntry - state.riskStopLoss
        val allowedUnits = maxRiskAmount / stopLossDistance
        val allowedLots = (allowedUnits / 25.0).toInt()

        assertEquals(5000.0, maxRiskAmount, 0.001)
        assertEquals(80.0, stopLossDistance, 0.001)
        assertEquals(62.5, allowedUnits, 0.001)
        assertEquals(2, allowedLots) // 2 lots = 50 units (50 * 80 = 4,000 <= 5,000 max loss)

        // Update risk parameters to 1,000,000 capital, 2% risk, 24000 entry, 23900 SL
        viewModel.updateRiskParameters(capital = 1000000.0, riskPct = 2.0, entry = 24000.0, sl = 23900.0)
        state = viewModel.uiState.value
        assertEquals(1000000.0, state.riskCapital, 0.001)
        assertEquals(2.0, state.riskPercent, 0.001)
        assertEquals(24000.0, state.riskEntry, 0.001)
        assertEquals(23900.0, state.riskStopLoss, 0.001)

        val newMaxRisk = state.riskCapital * (state.riskPercent / 100.0) // 20,000
        val newSlDist = state.riskEntry - state.riskStopLoss // 100
        val newAllowedLots = ((newMaxRisk / newSlDist) / 25.0).toInt() // 200 units / 25 = 8 lots
        assertEquals(20000.0, newMaxRisk, 0.001)
        assertEquals(8, newAllowedLots)

        // 3. Test Drawdown Circuit Breaker Simulation
        assertEquals(0.0, state.riskCurrentDrawdown, 0.001)
        assertFalse(state.riskCircuitBreakerHalted)

        // Simulate 1st loss: +1.1%
        viewModel.simulateDrawdownLoss()
        state = viewModel.uiState.value
        assertEquals(1.1, state.riskCurrentDrawdown, 0.01)
        assertEquals(1, state.riskConsecutiveLossCount)
        assertFalse(state.riskCircuitBreakerHalted)

        // Simulate 2nd loss: +1.1% -> 2.2%
        viewModel.simulateDrawdownLoss()
        state = viewModel.uiState.value
        assertEquals(2.2, state.riskCurrentDrawdown, 0.01)
        assertEquals(2, state.riskConsecutiveLossCount)
        assertFalse(state.riskCircuitBreakerHalted)

        // Simulate 3rd loss: +1.1% -> 3.3% >= 3.0% limit -> TRIPS CIRCUIT BREAKER
        viewModel.simulateDrawdownLoss()
        state = viewModel.uiState.value
        assertEquals(3.3, state.riskCurrentDrawdown, 0.01)
        assertEquals(3, state.riskConsecutiveLossCount)
        assertTrue("Circuit breaker must be armed and HALTED when drawdown reaches >= 3.0%", state.riskCircuitBreakerHalted)

        // Subsequent loss retains halted state
        viewModel.simulateDrawdownLoss()
        state = viewModel.uiState.value
        assertTrue(state.riskCircuitBreakerHalted)

        // 4. Test Circuit Breaker Reset
        viewModel.resetDrawdownCircuitBreaker()
        state = viewModel.uiState.value
        assertEquals(0.0, state.riskCurrentDrawdown, 0.001)
        assertEquals(0, state.riskConsecutiveLossCount)
        assertFalse("Circuit breaker should be reset", state.riskCircuitBreakerHalted)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. ERROR HANDLING, FAILED STATE & RETRY INITIALIZATION FLOW
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testModuleInitializationFailureAndRetryRecovery() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // 1. Select Module 1
        viewModel.selectModule(module1Sim)

        // 2. Trigger initialization with failure condition (forceFail = true)
        viewModel.initializeModule(module1Sim, forceFail = true)
        var state = viewModel.uiState.value
        assertEquals(EngineInitStatus.INITIALIZING, state.engineInitStatus)

        advanceUntilIdle()

        // 3. Verify Failed State
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.FAILED, state.engineInitStatus)
        assertFalse("Module must not be marked initialized when failed", state.isModuleInitialized)
        assertNotNull("Error message must be present", state.engineInitError)
        assertTrue(state.engineInitError!!.contains("Failed to initialize"))
        assertTrue(state.engineInitError!!.contains("Tap retry"))

        // 4. Verify UI Readiness Predicates during failure:
        // isEngineReady = status == READY && initializedModuleId == module.id
        val isEngineReady = state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module1Sim.id
        val isEngineFailed = state.engineInitStatus == EngineInitStatus.FAILED && state.selectedModule?.id == module1Sim.id
        assertFalse(isEngineReady)
        assertTrue(isEngineFailed)

        // 5. Retry Initialization
        viewModel.retryInitializeModule(module1Sim)
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.INITIALIZING, state.engineInitStatus)
        assertNull(state.engineInitError)

        advanceUntilIdle()

        // 6. Verify Full Recovery
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertTrue(state.isModuleInitialized)
        assertEquals("1", state.initializedModuleId)
        assertNull(state.engineInitError)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. STRICT MODULE ISOLATION (PREVENTS CROSS-CONTAMINATION)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testStrictModuleIsolationBetweenEngines() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // Step 1: Initialize Module 1 (Institutional Simulation)
        viewModel.selectModule(module1Sim)
        viewModel.initializeModule(module1Sim)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertEquals("1", state.initializedModuleId)
        assertEquals(AcademyEngineType.INSTITUTIONAL_SIMULATION, state.initializedEngineType)

        // Step 2: User closes Module 1 and clicks Module 2 (NIFTY 50 Dynamics)
        viewModel.closeModule()
        assertEquals(null, viewModel.uiState.value.selectedModule)

        viewModel.selectModule(module2Nifty)
        state = viewModel.uiState.value
        assertEquals(module2Nifty, state.selectedModule)

        // CRITICAL CHECK: Module 2 must NOT be marked initialized or ready!
        assertFalse("Module 2 must NOT inherit Module 1 initialization", state.isModuleInitialized)
        assertEquals("Module 2 must be in IDLE state on initial select", EngineInitStatus.IDLE, state.engineInitStatus)
        assertEquals(0.0f, state.engineInitProgress, 0.001f)

        // Check view predicate for Module 2
        val isMod2Ready = state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module2Nifty.id
        assertFalse("Module 2 must not be treated as ready before its own initialization", isMod2Ready)

        // Step 3: Initialize Module 2
        viewModel.initializeModule(module2Nifty)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.READY, state.engineInitStatus)
        assertEquals("2", state.initializedModuleId)
        assertEquals(AcademyEngineType.NIFTY_50_DYNAMICS, state.initializedEngineType)

        val isMod2NowReady = state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module2Nifty.id
        val isMod1NowReady = state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module1Sim.id
        assertTrue(isMod2NowReady)
        assertFalse(isMod1NowReady) // Module 1 is no longer the active initialized module

        // Step 4: Switch to Module 3 (Behavioral Bias)
        viewModel.selectModule(module3Bias)
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.IDLE, state.engineInitStatus)
        assertFalse(state.isModuleInitialized)
        assertFalse(state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module3Bias.id)

        // Step 5: Switch to Module 4 (Risk Discipline)
        viewModel.selectModule(module4Risk)
        state = viewModel.uiState.value
        assertEquals(EngineInitStatus.IDLE, state.engineInitStatus)
        assertFalse(state.isModuleInitialized)
        assertFalse(state.engineInitStatus == EngineInitStatus.READY && state.initializedModuleId == module4Risk.id)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. REDUNDANT INITIALIZATION GUARD
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testInitializationGuardPreventsConcurrentExecution() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        viewModel.selectModule(module1Sim)
        viewModel.initializeModule(module1Sim)

        assertEquals(EngineInitStatus.INITIALIZING, viewModel.uiState.value.engineInitStatus)

        // Calling initialize again while INITIALIZING should be safely ignored
        viewModel.initializeModule(module1Sim)
        assertEquals(EngineInitStatus.INITIALIZING, viewModel.uiState.value.engineInitStatus)

        advanceUntilIdle()
        assertEquals(EngineInitStatus.READY, viewModel.uiState.value.engineInitStatus)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST FACTORY HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun createTestViewModel(): AcademyViewModel {
        val bus = LiveIntelligenceBus()
        val settingsRepo = FakeSettingsRepository()
        val dummyMentor = object : MentorEngine {
            override val engineName: String = "Aarka AI"
            override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath = error("unused")
            override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview = error("unused")
            override suspend fun explainSignal(signal: AiTradeSignal): String = "Explaining ${signal.underlyingSymbol}"
            override suspend fun ask(query: String): String = "Mentor answer for: $query"
        }
        val dummyScanner = object : ScannerEngine {
            override val engineName: String = "Proprietary Scanner"
            override suspend fun scanSymbol(symbol: String, timeframe: String): AIAnalysisResult = error("unused")
            override fun monitorSymbols(symbols: List<String>): Flow<AIAnalysisResult> = emptyFlow()
        }
        val dummyChart = object : ChartEngine {
            override val engineName: String = "Lightweight Charts"
            @androidx.compose.runtime.Composable
            override fun Render(symbol: String, timeframe: String, candles: List<Candle>) {}
        }
        val router = EngineRouter(
            engines = setOf(dummyMentor, dummyScanner, dummyChart),
            settingsRepository = settingsRepo,
            logService = EngineLogService()
        )

        return AcademyViewModel(
            intelligenceBus = bus,
            settingsRepository = settingsRepo,
            engineRouter = router
        )
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val selectedScannerEngine: Flow<String> = flowOf("Proprietary Scanner")
        override val selectedMentorEngine: Flow<String> = flowOf("Aarka AI")
        override val selectedChartEngine: Flow<String> = flowOf("Lightweight Charts")
        override val selectedMarket: Flow<MarketType> = flowOf(MarketType.IN)
        override val isDarkMode: Flow<Boolean> = flowOf(true)
        override val riskProfile: Flow<String> = flowOf("CONSERVATIVE")
        override val isNotificationsEnabled: Flow<Boolean> = flowOf(true)

        override suspend fun setScannerEngine(engineName: String) {}
        override suspend fun setMentorEngine(engineName: String) {}
        override suspend fun setChartEngine(engineName: String) {}
        override suspend fun setMarket(market: MarketType) {}
        override suspend fun setDarkMode(enabled: Boolean) {}
        override suspend fun setRiskProfile(profile: String) {}
        override suspend fun setNotificationsEnabled(enabled: Boolean) {}
    }
}
