package com.marketintelligence.redxchartlibrary.data.indicators

sealed class Indicator {
    abstract val name: String
    abstract val requiresSeparatePane: Boolean

    data class SMA(val period: Int) : Indicator() {
        override val name = "SMA($period)"
        override val requiresSeparatePane = false
    }
    data class EMA(val period: Int) : Indicator() {
        override val name = "EMA($period)"
        override val requiresSeparatePane = false
    }
    data class BollingerBands(val period: Int, val multiplier: Double) : Indicator() {
        override val name = "BB($period, $multiplier)"
        override val requiresSeparatePane = false
    }
    object RSI : Indicator() {
        override val name = "RSI"
        override val requiresSeparatePane = true
    }
    object MACD : Indicator() {
        override val name = "MACD"
        override val requiresSeparatePane = true
    }
    object Volume : Indicator() {
        override val name = "Volume"
        override val requiresSeparatePane = true
    }
}
