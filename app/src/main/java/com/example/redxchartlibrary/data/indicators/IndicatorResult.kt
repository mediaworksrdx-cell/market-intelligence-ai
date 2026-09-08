package com.example.redxchartlibrary.data.indicators

sealed class IndicatorResult {
    data class Line(val values: List<LineData>) : IndicatorResult()
    data class Macd(val values: List<MacdData>) : IndicatorResult()
}
