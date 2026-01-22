package com.example.redxfnoscanner.domain

enum class OptionStrategy {
    // Bullish Strategies
    BULL_CALL_SPREAD,
    BULL_PUT_SPREAD,
    LONG_CALL,
    SHORT_PUT,

    // Bearish Strategies
    BEAR_PUT_SPREAD,
    BEAR_CALL_SPREAD,
    LONG_PUT,
    SHORT_CALL,

    // Neutral & Volatility Strategies
    LONG_STRADDLE,
    LONG_STRANGLE,
    SHORT_STRADDLE,
    SHORT_STRANGLE,
    IRON_CONDOR,
    IRON_BUTTERFLY
}

val strategyMap: Map<MarketRegime, List<OptionStrategy>> = mapOf(
    MarketRegime.TRENDING to listOf(
        OptionStrategy.BULL_CALL_SPREAD, 
        OptionStrategy.BULL_PUT_SPREAD,
        OptionStrategy.BEAR_PUT_SPREAD,
        OptionStrategy.BEAR_CALL_SPREAD,
        OptionStrategy.LONG_CALL,
        OptionStrategy.LONG_PUT
    ),
    MarketRegime.RANGE_BOUND to listOf(
        OptionStrategy.IRON_CONDOR,
        OptionStrategy.IRON_BUTTERFLY,
        OptionStrategy.SHORT_STRADDLE,
        OptionStrategy.SHORT_STRANGLE
    ),
    MarketRegime.VOLATILITY_DRIVEN to listOf(
        OptionStrategy.LONG_STRADDLE,
        OptionStrategy.LONG_STRANGLE
    )
)
