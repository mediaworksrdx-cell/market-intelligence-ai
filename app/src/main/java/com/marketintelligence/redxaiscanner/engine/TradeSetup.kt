package com.marketintelligence.redxaiscanner.engine

import java.math.BigDecimal

// Update ConfluenceSignal
data class ConfluenceSignal(
    val symbol: String,
    val timeframe: String,
    val higherTimeframeBias: MarketBias,
    val confidenceScore: Double,
    val smcSignal: Any,
    val patternSignal: PatternResult? = null,
    val fvgSignal: FVGResult? = null,
    val versions: Map<String, String>,
    val integrityHash: String,
    val explanation: SignalExplanation,
    val liquidityZones: List<LiquidityZone> = emptyList(), // Added
    val regime: MarketRegime = MarketRegime.UNDEFINED // Added
)

// Update TradeSetup to be the final container
data class TradeSetup(
    val entryPrice: BigDecimal,
    val stopLossPrice: BigDecimal,
    val takeProfit1: BigDecimal,
    val takeProfit2: BigDecimal,
    val riskToRewardRatio: String,
    val underlyingSignal: ScoredSignal,
    val versions: Map<String, String>,
    val explanation: SignalExplanation // The final, user-facing explanation
)
