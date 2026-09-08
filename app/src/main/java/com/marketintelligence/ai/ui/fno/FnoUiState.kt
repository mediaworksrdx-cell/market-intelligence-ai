package com.marketintelligence.ai.ui.fno

import com.marketintelligence.ai.domain.model.*

data class FnoUiState(
    val searchQuery: String = "NIFTY",
    val selectedAsset: String = "NIFTY 50",
    val spotPrice: Double = 22500.0,
    val isLoading: Boolean = false,
    
    // 1. Dashboard Data
    val summary: FnoSummary = FnoSummary(),
    val futuresBuildup: List<BuildupData> = emptyList(),
    val heatmapData: List<HeatmapItem> = emptyList(),
    
    // 2. Options Study
    val optionChain: OptionChain? = null,
    val maxPainHistory: List<Double> = emptyList(),
    val ivHistory: List<Double> = emptyList(),
    val gexProfile: com.example.marketintelligence.domain.engine.GexProfile? = null,
    val institutionalGreeks: Map<Double, com.example.marketintelligence.domain.engine.InstitutionalGreeks> = emptyMap(),
    val participantData: List<ParticipantPositioning> = emptyList(),
    
    // 3. Strategy Builder
    val activeLegs: List<StrategyLeg> = emptyList(),
    val predefinedStrategies: List<OptionStrategy> = emptyList(),
    val selectedStrategy: OptionStrategy? = null,
    val selectedCategory: StrategyCategory = StrategyCategory.BULLISH,
    val trackedStrategies: List<OptionStrategy> = emptyList(), // Saved strategies
    val ivSimulation: Double = 0.0, // Shift in %
    val timeSimulation: Int = 0, // Days from today
    val strategyGreeks: Greeks = Greeks(0.0, 0.0, 0.0, 0.0)
)

enum class StrategyCategory {
    BULLISH, BEARISH, NEUTRAL, VOLATILITY
}

data class ParticipantPositioning(
    val participant: String,
    val netFutures: Int,
    val netCalls: Int,
    val netPuts: Int,
    val bias: String
)

data class FnoSummary(
    val changePercent: Double = 0.0,
    val iv: Double = 0.0,
    val pcr: Double = 0.0,
    val maxPain: Double = 0.0,
    val lotSize: Int = 0,
    val trend: String = "NEUTRAL",
    val contracts: List<FutureContract> = emptyList(),
    val highestCallOIStrike: Double = 0.0,
    val highestPutOIStrike: Double = 0.0
)

data class BuildupData(
    val symbol: String,
    val type: String, // "Long Buildup", "Short Covering", etc.
    val changePercent: Double,
    val oiChangePercent: Double
)

data class HeatmapItem(
    val sector: String,
    val changePercent: Double,
    val weight: Float
)
