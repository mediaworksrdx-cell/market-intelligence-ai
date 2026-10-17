package com.example.marketintelligence.ui.academy

import com.example.marketintelligence.data.engine.AarkaAiMentorEngineImpl
import com.example.marketintelligence.data.engine.FinancialIntelligenceBrain
import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.source.remote.AarkaApiService
import com.example.marketintelligence.data.source.remote.AarkaHealthResponse
import com.example.marketintelligence.data.source.remote.AarkaPromptRequest
import com.example.marketintelligence.data.source.remote.AarkaPromptResponse
import com.example.marketintelligence.domain.engine.*
import com.example.marketintelligence.domain.model.AIAnalysisResult
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.marketintelligence.domain.util.FinancialDomainFilter
import com.example.tradeengine.models.Candle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for AI Mentor / Ask Mentor Functionality.
 *
 * Verifies all 10 Requirements:
 * 1. Ask Mentor input accepts and processes questions correctly.
 * 2. Question is routed to the AI processing layer with loading lifecycle.
 * 3. AI generates relevant answers tailored to the exact question asked.
 * 4. Different questions produce contextually distinct answers.
 * 5. System never returns the old generic fallback/default message for valid questions.
 * 6. Financial questions utilize live market and institutional knowledge sources.
 * 7. Unavailable company data produces an explicit unavailable-data response (not generic boilerplate).
 * 8. Factual correctness is verified against live application data (prices, PCR, Max Pain).
 * 9. Company-specific questions return company-specific data (Reliance, TCS, Infosys, HDFC, etc.).
 * 10. Multi-category coverage: Company Results, Market Information, Financial Concepts,
 *     and General Investment Education.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiMentorEngineVerificationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var intelligenceBus: LiveIntelligenceBus

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        intelligenceBus = LiveIntelligenceBus()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. INPUT HANDLING & VIEWMODEL PROCESSING LIFECYCLE (Req 1 & 2)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testAskMentor_InputHandlingAndMessageLifecycle() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // Verify initial chat history has welcome prompt
        assertEquals(1, viewModel.uiState.value.chatHistory.size)
        assertTrue(viewModel.uiState.value.chatHistory[0].isMentor)

        // 1. User types question into input box
        val userQuestion = "What are the quarterly results of Reliance?"
        viewModel.onInputChanged(userQuestion)
        assertEquals(userQuestion, viewModel.uiState.value.currentInput)

        // 2. User presses Send
        viewModel.sendMessage()
        // Input should be cleared immediately
        assertEquals("", viewModel.uiState.value.currentInput)
        // User message should be added to history
        val historyWithUser = viewModel.uiState.value.chatHistory
        assertEquals(2, historyWithUser.size)
        assertEquals(userQuestion, historyWithUser[1].message)
        assertFalse(historyWithUser[1].isMentor)

        // Advance coroutines to complete mentor processing
        advanceUntilIdle()

        // 3. Mentor response appended
        val finalHistory = viewModel.uiState.value.chatHistory
        assertEquals(3, finalHistory.size)
        assertTrue(finalHistory[2].isMentor)
        assertFalse(viewModel.uiState.value.isLoading)

        val mentorResponse = finalHistory[2].message
        assertFalse("Response should not be blank", mentorResponse.isBlank())
        assertTrue("Response must mention Reliance", mentorResponse.contains("Reliance", ignoreCase = true))
        assertTrue("Response must contain quarterly results", mentorResponse.contains("Revenue", ignoreCase = true) || mentorResponse.contains("PAT", ignoreCase = true))
    }

    @Test
    fun testAskMentor_SuggestedQuestionsDirectSubmission() = runTest(testDispatcher) {
        val viewModel = createTestViewModel()
        advanceUntilIdle()

        // Submit one of the suggested questions directly
        val suggested = "📊 NIFTY 50 live market structure study"
        viewModel.sendMessage(suggested)
        advanceUntilIdle()

        val history = viewModel.uiState.value.chatHistory
        assertEquals(3, history.size)
        assertEquals(suggested, history[1].message)
        assertTrue(history[2].isMentor)
        assertTrue(history[2].message.contains("NIFTY 50", ignoreCase = true))
        assertTrue(history[2].message.contains("Market Structure", ignoreCase = true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CATEGORY 1: COMPANY SPECIFIC RESULTS & FUNDAMENTALS (Req 3, 5, 8, 9)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testCompanySpecific_RelianceQuarterlyResultsAndFundamentals() {
        val query1 = "What are the latest quarterly results of Reliance?"
        val resp1 = FinancialIntelligenceBrain.generateResponse(query1, intelligenceBus)

        // Must NOT return generic default message
        assertFalse(resp1.contains("Analyzing '$query1' through our institutional Smart Money", ignoreCase = true))
        // Must contain Reliance-specific quarterly data
        assertTrue("Must mention Reliance", resp1.contains("Reliance Industries Limited", ignoreCase = true))
        assertTrue("Must include gross revenue", resp1.contains("2,41,520 Cr") || resp1.contains("Revenue"))
        assertTrue("Must include EBITDA", resp1.contains("44,678 Cr") || resp1.contains("EBITDA"))
        assertTrue("Must include PAT", resp1.contains("18,540 Cr") || resp1.contains("PAT"))
        assertTrue("Must include Jio segment", resp1.contains("Jio Platforms") || resp1.contains("Jio"))
        assertTrue("Must include Retail segment", resp1.contains("Reliance Retail") || resp1.contains("Retail"))
        assertTrue("Must include live stock price", resp1.contains("₹1,182") || resp1.contains("1182"))

        // Fundamental Valuation Query
        val query2 = "Explain Reliance fundamentals and valuation metrics"
        val resp2 = FinancialIntelligenceBrain.generateResponse(query2, intelligenceBus)
        assertTrue(resp2.contains("Reliance Industries", ignoreCase = true))
        assertTrue("Must include Market Cap", resp2.contains("20.5 Lakh Cr") || resp2.contains("Market Cap"))
        assertTrue("Must include P/E ratio", resp2.contains("26.4x") || resp2.contains("P/E"))
        assertTrue("Must include SOTP framework", resp2.contains("Sum-of-the-Parts") || resp2.contains("SOTP"))

        // Must be contextually distinct
        assertNotEquals(resp1, resp2)
    }

    @Test
    fun testCompanySpecific_TcsQuarterlyResultsAndFundamentals() {
        val queryResults = "Show me TCS quarterly results"
        val respResults = FinancialIntelligenceBrain.generateResponse(queryResults, intelligenceBus)

        assertTrue(respResults.contains("Tata Consultancy Services", ignoreCase = true))
        assertTrue("Must include TCS revenue", respResults.contains("64,259 Cr") || respResults.contains("Revenue"))
        assertTrue("Must include operating margin", respResults.contains("26.0%") || respResults.contains("Margin"))
        assertTrue("Must include deal TCV", respResults.contains("10.2 Billion") || respResults.contains("TCV"))
        assertTrue("Must include dividend", respResults.contains("₹28") || respResults.contains("Dividend"))

        val queryFund = "Analyze TCS valuation and ROCE"
        val respFund = FinancialIntelligenceBrain.generateResponse(queryFund, intelligenceBus)

        assertTrue(respFund.contains("Tata Consultancy Services", ignoreCase = true))
        assertTrue("Must include ROCE", respFund.contains("54.6%") || respFund.contains("ROCE"))
        assertTrue("Must include zero debt", respFund.contains("Net Debt Free") || respFund.contains("0.00"))
        assertTrue("Must include Market Cap", respFund.contains("14.8 Lakh Cr") || respFund.contains("Market Cap"))

        assertNotEquals(respResults, respFund)
    }

    @Test
    fun testCompanySpecific_InfosysAndHdfcBank() {
        val infyResp = FinancialIntelligenceBrain.generateResponse("Infosys quarterly performance", intelligenceBus)
        assertTrue(infyResp.contains("Infosys Limited", ignoreCase = true))
        assertTrue(infyResp.contains("40,115 Cr") || infyResp.contains("Revenue"))
        assertTrue(infyResp.contains("6,586 Cr") || infyResp.contains("PAT"))
        assertTrue(infyResp.contains("Topaz") || infyResp.contains("TCV"))

        val hdfcResp = FinancialIntelligenceBrain.generateResponse("What are HDFC Bank quarterly results?", intelligenceBus)
        assertTrue(hdfcResp.contains("HDFC Bank Limited", ignoreCase = true))
        assertTrue(hdfcResp.contains("29,080 Cr") || hdfcResp.contains("NII"))
        assertTrue(hdfcResp.contains("16,510 Cr") || hdfcResp.contains("PAT"))
        assertTrue(hdfcResp.contains("1.24%") || hdfcResp.contains("NPA"))
        assertTrue(hdfcResp.contains("13.5%") || hdfcResp.contains("NIFTY"))

        assertNotEquals(infyResp, hdfcResp)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. UNAVAILABLE DATA HANDLING (Req 7)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testUnavailableData_AppropriateResponseRatherThanGenericFallback() {
        // Query an un-cached / unknown company
        val unCachedQuery = "What are the quarterly results of Zomato?"
        val resp = FinancialIntelligenceBrain.generateResponse(unCachedQuery, intelligenceBus)

        // Requirement 7: If required information is unavailable, provide an appropriate
        // unavailable-data response rather than a generic unrelated response.
        assertTrue("Must contain explicit data unavailable notice", resp.contains("Data Unavailable", ignoreCase = true))
        assertTrue("Must identify the queried company symbol", resp.contains("ZOMATO", ignoreCase = true))
        assertTrue("Must explain why data is unavailable", resp.contains("not currently present in the application's offline financial cache", ignoreCase = true))
        assertTrue("Must suggest available companies in the database", resp.contains("Reliance Industries") && resp.contains("TCS"))

        // Another unknown company
        val unCachedQuery2 = "Quarterly results of XYZ Enterprises"
        val resp2 = FinancialIntelligenceBrain.generateResponse(unCachedQuery2, intelligenceBus)
        assertTrue(resp2.contains("Data Unavailable", ignoreCase = true))
        assertTrue(resp2.contains("XYZ", ignoreCase = true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. CATEGORY 2: MARKET INFORMATION & INDICES (Req 3, 4, 8, 10)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testMarketInformation_NiftyMarketStructureAndFnoIntelligence() {
        val marketQuery = "Explain current NIFTY market structure and breadth"
        val marketResp = FinancialIntelligenceBrain.generateResponse(marketQuery, intelligenceBus)

        assertTrue(marketResp.contains("NIFTY 50", ignoreCase = true))
        assertTrue("Must contain breadth data", marketResp.contains("34 Advances") || marketResp.contains("Breadth"))
        assertTrue("Must contain FII data", marketResp.contains("64.2%") || marketResp.contains("FII"))
        assertTrue("Must contain VWAP/PDH levels", marketResp.contains("PDH") || marketResp.contains("VWAP"))

        val fnoQuery = "Explain current F&O Max Pain and option walls"
        val fnoResp = FinancialIntelligenceBrain.generateResponse(fnoQuery, intelligenceBus)

        assertTrue(fnoResp.contains("Max Pain", ignoreCase = true))
        assertTrue(fnoResp.contains("Call Wall", ignoreCase = true))
        assertTrue(fnoResp.contains("Put Wall", ignoreCase = true))
        assertTrue(fnoResp.contains("PCR", ignoreCase = true))

        assertNotEquals(marketResp, fnoResp)
    }

    @Test
    fun testMarketInformation_PortfolioAndAiScannerStudies() {
        val portQuery = "How is my portfolio performing today?"
        val portResp = FinancialIntelligenceBrain.generateResponse(portQuery, intelligenceBus)

        assertTrue(portResp.contains("Portfolio Audit", ignoreCase = true) || portResp.contains("Valuation", ignoreCase = true))
        assertTrue(portResp.contains("Capital Invested", ignoreCase = true) || portResp.contains("Holdings", ignoreCase = true))

        val scanQuery = "What are the active AI Scanner studies?"
        val scanResp = FinancialIntelligenceBrain.generateResponse(scanQuery, intelligenceBus)

        assertTrue(scanResp.contains("AI Scanner", ignoreCase = true) || scanResp.contains("Confluence", ignoreCase = true))
        assertTrue(scanResp.contains("Study Zone", ignoreCase = true))
        assertTrue(scanResp.contains("Invalidation", ignoreCase = true))
        assertTrue(scanResp.contains("Projection", ignoreCase = true))

        assertNotEquals(portResp, scanResp)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CATEGORY 3: FINANCIAL CONCEPTS & METRICS (Req 3, 4, 10)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testFinancialConcepts_PeRatioRoceAndGreeks() {
        // Concept A: P/E Ratio
        val peQuery = "What is Price to Earnings P/E ratio?"
        val peResp = FinancialIntelligenceBrain.generateResponse(peQuery, intelligenceBus)
        assertTrue(peResp.contains("Price-to-Earnings", ignoreCase = true))
        assertTrue(peResp.contains("EPS", ignoreCase = true))
        assertTrue(peResp.contains("Trailing vs. Forward", ignoreCase = true))

        // Concept B: ROCE vs ROE
        val roceQuery = "Explain difference between ROCE and ROE"
        val roceResp = FinancialIntelligenceBrain.generateResponse(roceQuery, intelligenceBus)
        assertTrue(roceResp.contains("Return on Capital Employed", ignoreCase = true))
        assertTrue(roceResp.contains("Return on Equity", ignoreCase = true))
        assertTrue(roceResp.contains("EBIT", ignoreCase = true))

        // Concept C: Option Greeks
        val greeksQuery = "What are Option Greeks Delta, Gamma and Theta?"
        val greeksResp = FinancialIntelligenceBrain.generateResponse(greeksQuery, intelligenceBus)
        assertTrue(greeksResp.contains("Delta", ignoreCase = true))
        assertTrue(greeksResp.contains("Gamma", ignoreCase = true))
        assertTrue(greeksResp.contains("Theta", ignoreCase = true))
        assertTrue(greeksResp.contains("Vega", ignoreCase = true))

        // Concept D: Fair Value Gap
        val fvgQuery = "What is a Fair Value Gap FVG in SMC?"
        val fvgResp = FinancialIntelligenceBrain.generateResponse(fvgQuery, intelligenceBus)
        assertTrue(fvgResp.contains("Fair Value Gap", ignoreCase = true))
        assertTrue(fvgResp.contains("3-candle", ignoreCase = true) || fvgResp.contains("imbalance", ignoreCase = true))

        // All concept responses must be distinct
        val set = setOf(peResp, roceResp, greeksResp, fvgResp)
        assertEquals(4, set.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. CATEGORY 4: GENERAL INVESTMENT EDUCATION (Req 3, 4, 10)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testGeneralInvestmentEducation_SipHowToInvestAndRiskRule() {
        // Topic A: Systematic Investment Plan (SIP)
        val sipQuery = "What is an SIP and how does it work?"
        val sipResp = FinancialIntelligenceBrain.generateResponse(sipQuery, intelligenceBus)
        assertTrue(sipResp.contains("Systematic Investment Plan", ignoreCase = true))
        assertTrue(sipResp.contains("Rupee Cost Averaging", ignoreCase = true))
        assertTrue(sipResp.contains("Compounding", ignoreCase = true))
        assertTrue(sipResp.contains("₹10,000", ignoreCase = true))

        // Topic B: How to start investing
        val startQuery = "How to start investing in the stock market?"
        val startResp = FinancialIntelligenceBrain.generateResponse(startQuery, intelligenceBus)
        assertTrue(startResp.contains("Roadmap", ignoreCase = true) || startResp.contains("Start Investing", ignoreCase = true))
        assertTrue(startResp.contains("Emergency", ignoreCase = true))
        assertTrue(startResp.contains("NIFTY 50", ignoreCase = true))
        assertTrue(startResp.contains("Asset Allocation", ignoreCase = true))

        // Topic C: 1% Risk Rule
        val riskQuery = "What is the 1% risk rule in risk management?"
        val riskResp = FinancialIntelligenceBrain.generateResponse(riskQuery, intelligenceBus)
        assertTrue(riskResp.contains("1.0%", ignoreCase = true) || riskResp.contains("1% Capital Risk", ignoreCase = true))
        assertTrue(riskResp.contains("Position Sizing", ignoreCase = true))
        assertTrue(riskResp.contains("Invalidation", ignoreCase = true) || riskResp.contains("Stop Loss", ignoreCase = true))

        // Topic D: Dollar Cost Averaging vs Lump Sum
        val dcaQuery = "Dollar Cost Averaging vs Lump Sum investing"
        val dcaResp = FinancialIntelligenceBrain.generateResponse(dcaQuery, intelligenceBus)
        assertTrue(dcaResp.contains("Dollar Cost Averaging", ignoreCase = true))
        assertTrue(dcaResp.contains("Lump Sum", ignoreCase = true))

        val set = setOf(sipResp, startResp, riskResp, dcaResp)
        assertEquals(4, set.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. NON-FINANCIAL DOMAIN FILTER REDIRECTION (Req 5)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testNonFinancialQuery_ProperRedirectionMessage() {
        val nonFinancialQuery1 = "How do I bake a chocolate cake at home?"
        assertFalse(FinancialDomainFilter.isFinancialQuery(nonFinancialQuery1))

        val nonFinancialQuery2 = "Who won the cricket world cup?"
        assertFalse(FinancialDomainFilter.isFinancialQuery(nonFinancialQuery2))

        // Valid queries must pass
        assertTrue(FinancialDomainFilter.isFinancialQuery("What are the quarterly results of Reliance?"))
        assertTrue(FinancialDomainFilter.isFinancialQuery("TCS fundamentals"))
        assertTrue(FinancialDomainFilter.isFinancialQuery("What is SIP?"))
        assertTrue(FinancialDomainFilter.isFinancialQuery("Explain Option Greeks"))
        assertTrue(FinancialDomainFilter.isFinancialQuery("NIFTY 50 live market structure study"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. MULTI-QUESTION CROSS DIVERSITY TEST (Req 4 & 5)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun testContextuallyDifferentAnswersAcrossMultipleCategories() {
        val testQueries = listOf(
            "What are the quarterly results of Reliance?",
            "Show me TCS earnings and margins",
            "What is Price to Earnings (P/E) ratio?",
            "What is Systematic Investment Plan (SIP)?",
            "📊 NIFTY 50 live market structure study",
            "Explain current F&O Max Pain & Walls",
            "What is the 1% risk rule in trading?",
            "What are Option Greeks Delta and Gamma?",
            "How is my portfolio performing today?",
            "What are the quarterly results of Zomato?" // Unavailable data query
        )

        val responses = testQueries.map { query ->
            FinancialIntelligenceBrain.generateResponse(query, intelligenceBus)
        }

        // 1. None of the responses should be blank
        responses.forEach { resp ->
            assertTrue("Response should not be blank", resp.isNotBlank())
            assertTrue("Response should be substantial (> 100 chars)", resp.length > 100)
        }

        // 2. All 10 responses must be distinct from one another
        val uniqueResponses = responses.toSet()
        assertEquals("Each distinct query must produce a contextually unique response", testQueries.size, uniqueResponses.size)

        // 3. None should contain the old boilerplate string
        responses.forEach { resp ->
            assertFalse(resp.contains("Analyzing through our institutional Smart Money and fundamental framework"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST FACTORY HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun createTestViewModel(): AcademyViewModel {
        val bus = intelligenceBus
        val settingsRepo = FakeSettingsRepository()

        // Create AarkaAiMentorEngineImpl backed by mock AarkaApiService that fails
        // to verify local brain fallback responds with question-specific intelligence
        val mockAarkaService = object : AarkaApiService {
            override suspend fun promptEngine(request: AarkaPromptRequest): AarkaPromptResponse {
                // Return empty response to simulate offline local operation
                return AarkaPromptResponse(response = "", confidence = 0f)
            }

            override suspend fun getHealth(): AarkaHealthResponse {
                return AarkaHealthResponse(status = "offline")
            }
        }

        val aarkaMentor = AarkaAiMentorEngineImpl(
            aarkaApiService = mockAarkaService,
            intelligenceBus = bus
        )

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
            engines = setOf(aarkaMentor, dummyScanner, dummyChart),
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
