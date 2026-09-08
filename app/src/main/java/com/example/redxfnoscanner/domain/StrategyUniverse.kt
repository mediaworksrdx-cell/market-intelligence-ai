package com.example.redxfnoscanner.domain

enum class OptionStrategy {
    // Bullish Strategies
    BULL_CALL_SPREAD,
    BULL_PUT_SPREAD,
    LONG_CALL,
    SHORT_PUT,
    COVERED_CALL,
    CALL_RATIO_BACKSPREAD,
    BULL_CALENDAR_SPREAD,
    CALL_DIAGONAL_SPREAD,
    LONG_CALL_BUTTERFLY,

    // Bearish Strategies
    BEAR_PUT_SPREAD,
    BEAR_CALL_SPREAD,
    LONG_PUT,
    SHORT_CALL,
    PROTECTIVE_PUT,
    PUT_RATIO_BACKSPREAD,
    BEAR_CALENDAR_SPREAD,
    PUT_DIAGONAL_SPREAD,
    LONG_PUT_BUTTERFLY,

    // Neutral & Income Strategies
    SHORT_STRADDLE,
    SHORT_STRANGLE,
    IRON_CONDOR,
    IRON_BUTTERFLY,
    JADE_LIZARD,
    REVERSE_JADE_LIZARD,
    BROKEN_WING_BUTTERFLY,
    CALENDAR_SPREAD,

    // Volatility & Breakout Strategies
    LONG_STRADDLE,
    LONG_STRANGLE,
    REVERSE_IRON_CONDOR,
    REVERSE_IRON_BUTTERFLY
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
    MarketRegime.TRENDING_UP to listOf(
        OptionStrategy.BULL_CALL_SPREAD, 
        OptionStrategy.BULL_PUT_SPREAD,
        OptionStrategy.LONG_CALL,
        OptionStrategy.SHORT_PUT,
        OptionStrategy.COVERED_CALL,
        OptionStrategy.CALL_RATIO_BACKSPREAD,
        OptionStrategy.BULL_CALENDAR_SPREAD,
        OptionStrategy.CALL_DIAGONAL_SPREAD,
        OptionStrategy.LONG_CALL_BUTTERFLY
    ),
    MarketRegime.TRENDING_DOWN to listOf(
        OptionStrategy.BEAR_PUT_SPREAD,
        OptionStrategy.BEAR_CALL_SPREAD,
        OptionStrategy.LONG_PUT,
        OptionStrategy.SHORT_CALL,
        OptionStrategy.PROTECTIVE_PUT,
        OptionStrategy.PUT_RATIO_BACKSPREAD,
        OptionStrategy.BEAR_CALENDAR_SPREAD,
        OptionStrategy.PUT_DIAGONAL_SPREAD,
        OptionStrategy.LONG_PUT_BUTTERFLY
    ),
    MarketRegime.RANGE_BOUND to listOf(
        OptionStrategy.IRON_CONDOR,
        OptionStrategy.IRON_BUTTERFLY,
        OptionStrategy.SHORT_STRADDLE,
        OptionStrategy.SHORT_STRANGLE,
        OptionStrategy.JADE_LIZARD,
        OptionStrategy.REVERSE_JADE_LIZARD,
        OptionStrategy.BROKEN_WING_BUTTERFLY,
        OptionStrategy.CALENDAR_SPREAD
    ),
    MarketRegime.VOLATILITY_DRIVEN to listOf(
        OptionStrategy.LONG_STRADDLE,
        OptionStrategy.LONG_STRANGLE,
        OptionStrategy.REVERSE_IRON_CONDOR,
        OptionStrategy.REVERSE_IRON_BUTTERFLY,
        OptionStrategy.CALL_RATIO_BACKSPREAD,
        OptionStrategy.PUT_RATIO_BACKSPREAD
    ),
    MarketRegime.VOLATILITY_EXPANSION to listOf(
        OptionStrategy.LONG_STRADDLE,
        OptionStrategy.LONG_STRANGLE,
        OptionStrategy.REVERSE_IRON_CONDOR,
        OptionStrategy.REVERSE_IRON_BUTTERFLY,
        OptionStrategy.CALL_RATIO_BACKSPREAD,
        OptionStrategy.PUT_RATIO_BACKSPREAD
    ),
    MarketRegime.EVENT_RISK to listOf(
        OptionStrategy.LONG_STRADDLE,
        OptionStrategy.LONG_STRANGLE,
        OptionStrategy.REVERSE_IRON_CONDOR
    )
)
