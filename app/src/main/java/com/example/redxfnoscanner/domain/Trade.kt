package com.example.redxfnoscanner.domain

import com.example.redxfnoscanner.data.Option

data class Leg(val option: Option, val quantity: Int)

data class Position(val legs: List<Leg>)

data class RiskMetrics(
    val maxLoss: Double,
    val maxProfit: Double,
    val breakevenPoints: List<Double>,
    val marginRequirement: Double
)

data class TradeSignal(
    val strategy: OptionStrategy,
    val position: Position,
    val riskMetrics: RiskMetrics,
    val confidenceScore: Double,
    val entryPrice: Double,
    val target: Double,
    val stopLoss: Double
)
