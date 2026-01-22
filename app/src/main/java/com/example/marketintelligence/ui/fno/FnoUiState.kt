package com.example.marketintelligence.ui.fno

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.model.MarketRegime
import com.example.marketintelligence.domain.model.*

data class FnoUiState(
    val searchQuery: String = "",
    val selectedAsset: String = "NIFTY 50",
    val spotPrice: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val marketRegime: MarketRegime = MarketRegime.UNDEFINED,
    
    // 1. Summary Data
    val summary: FnoSummary = FnoSummary(),
    
    // 2. Options Study Data
    val optionChain: OptionChain? = null,
    val optionsStudyStats: OptionsStudyStats = OptionsStudyStats(),
    
    // 3. Strategies
    val availableStrategies: List<OptionStrategy> = emptyList(),
    val selectedStrategy: OptionStrategy? = null,
    
    // AI Signal
    val generatedSignal: AiTradeSignal? = null,
    val mentorExplanation: String? = null
)

data class FnoSummary(
    val futuresPrices: List<FuturePrice> = emptyList(), // Near, Next, Far
    val iv: Double = 0.0,
    val pcr: Double = 0.0,
    val maxPain: Double = 0.0,
    val highestCallOIStrike: Double = 0.0,
    val highestPutOIStrike: Double = 0.0,
    val nearMonthStrike: Double = 0.0
)

data class FuturePrice(val expiry: String, val price: Double)

data class OptionsStudyStats(
    val maxPain: Double = 0.0,
    val iv: Double = 0.0,
    val pcr: Double = 0.0
)
