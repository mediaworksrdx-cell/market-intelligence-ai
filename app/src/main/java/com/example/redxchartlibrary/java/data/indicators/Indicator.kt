package com.example.redxchartlibrary.data.indicators

// Represents a single data point for a line-based indicator (e.g., SMA, EMA, RSI)
data class LineData(val timestamp: Long, val value: Float)

// Represents a single data point for MACD, which has three lines
data class MacdData(
    val timestamp: Long,
    val macdLine: Float,
    val signalLine: Float,
    val histogram: Float
)

// A generic container for any type of indicator result
sealed class IndicatorResult {
    data class Line(val data: List<LineData>) : IndicatorResult()
    data class MACD(val data: List<MacdData>) : IndicatorResult()
}