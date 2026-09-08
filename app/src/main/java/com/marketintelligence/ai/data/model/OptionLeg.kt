package com.marketintelligence.ai.data.model

data class OptionLeg(
    val optionSymbol: String,
    val type: OptionType, // CE or PE
    val strike: Double,
    val expiry: String,
    val action: TradeAction, // BUY or SELL
    val quantity: Int,
    val premium: Double // Added for risk calculation
)

enum class OptionType { CE, PE }
enum class TradeAction { BUY, SELL }
