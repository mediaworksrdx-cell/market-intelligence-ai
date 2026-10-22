package com.example.marketintelligence.ui.fno

import com.example.marketintelligence.domain.model.*

data class FnoUiState(
    val searchQuery: String = "NIFTY",
    val selectedAsset: String = "NIFTY 50",
    val spotPrice: Double = 22500.0,
    val selectedMarket: MarketType = MarketType.IN,
    val isLoading: Boolean = false,
    
    // 1. Dashboard Data
    val summary: FnoSummary = FnoSummary(),
    val futuresBuildup: List<BuildupData> = emptyList(),
    val heatmapData: List<HeatmapItem> = emptyList(),
    
    // 2. Options Study
    val optionChain: OptionChain? = null,
    val selectedStrikeForDetail: OptionChainData? = null,
    val selectedOptionSide: String? = null, // "CALL", "PUT", or null for strike overview
    val strikeChartState: StrikeChartState = StrikeChartState(),
    val maxPainHistory: List<Double> = emptyList(),
    val ivHistory: List<Double> = emptyList(),
    val gexProfile: com.example.marketintelligence.domain.engine.GexProfile? = null,
    val institutionalGreeks: Map<Double, com.example.marketintelligence.domain.engine.InstitutionalGreeks> = emptyMap(),
    val participantData: List<ParticipantPositioning> = emptyList(),
    
    // 3. Strategy Builder
    val activeLegs: List<StrategyLeg> = emptyList(),
    val predefinedStrategies: List<OptionStrategy> = emptyList(),
    val selectedStrategy: OptionStrategy? = null,
    val selectedStrategyName: String = "",
    val customStrategyName: String = "",
    val isStrategyAnalyzed: Boolean = false,
    val editingStrategyId: String? = null,
    val selectedCategory: StrategyCategory = StrategyCategory.BULLISH,
    val trackedStrategies: List<OptionStrategy> = emptyList(), // Saved strategies
    val ivSimulation: Double = 0.0, // Shift in %
    val timeSimulation: Int = 0, // Days from today
    val strategyGreeks: Greeks = Greeks(0.0, 0.0, 0.0, 0.0),
    val totalStrategyPnl: Double = 0.0,
    val totalStrategyPnlPct: Double = 0.0,
    val netPremium: Double = 0.0,
    val currentStrategyValue: Double = 0.0,
    val selectedExpiry: String = "",
    val availableExpiries: List<String> = emptyList(),
    val lotSize: Int = 65,
    val numberOfLots: Int = 1,
    val capitalRequired: Double = 0.0,
    val strikeStep: Double = 50.0,
    val isLiveConnected: Boolean = true,
    val selectedWorkspaceTab: OpstraWorkspaceTab = OpstraWorkspaceTab.PAYOFF_CHART,
    val spotSimulationPct: Double = 0.0,
    val analysisResult: com.example.marketintelligence.domain.engine.StrategyAnalysisResult? = null,
    val showResetConfirmDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val pendingDeleteStrategyId: String? = null,
    val isRenameDialogOpen: Boolean = false,
    val strategyRenameInput: String = "",
    val chartPriceRangePct: Double = 0.15,
    val selectedCrosshairPrice: Double? = null,

    // 4. Buildup Scanner
    val buildupStocks: List<FnoBuildupStock> = emptyList(),
    val buildupFilter: BuildupType? = null,
    val buildupAssetTypeFilter: AssetTypeFilter = AssetTypeFilter.ALL,
    val buildupSectorFilter: String = "ALL",
    val buildupSortOrder: BuildupSortOrder = BuildupSortOrder.OI_GAINERS,
    val buildupSearchQuery: String = "",
    val longBuildupCount: Int = 0,
    val shortBuildupCount: Int = 0,
    val shortCoveringCount: Int = 0,
    val longUnwindingCount: Int = 0
)

enum class OpstraWorkspaceTab(val title: String) {
    PAYOFF_CHART("Payoff Chart"),
    FUTURES_CHART("Futures Chart"),
    GREEKS("Greeks"),
    PNL_ANALYSIS("P&L Analysis")
}

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

data class StrikeChartState(
    val assetSymbol: String = "",
    val strikePrice: Double = 0.0,
    val optionType: String = "CE", // "CE" or "PE"
    val expiry: String = "",
    val contractDisplayName: String = "",
    val ltp: Double = 0.0,
    val changePercent: Double = 0.0,
    val iv: Double = 0.0,
    val oi: Double = 0.0,
    val greeks: Greeks = Greeks(0.0, 0.0, 0.0, 0.0),
    val candles: List<com.example.tradeengine.models.Candle> = emptyList(),
    val selectedTimeframe: String = "5m",
    val isLoading: Boolean = false,
    val error: String? = null
)

