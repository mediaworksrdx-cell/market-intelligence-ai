package com.marketintelligence.redxchartlibrary.data.indicators

import com.marketintelligence.redxchartlibrary.model.Candle

object IndicatorFactory {
    fun calculate(indicator: Indicator, candles: List<Candle>): IndicatorOutput {
        return when (indicator) {
            is Indicator.SMA -> IndicatorCalculations.calculateSMA(candles, indicator.period)
            is Indicator.EMA -> IndicatorCalculations.calculateEMA(candles, indicator.period)
            is Indicator.RSI -> IndicatorCalculations.calculateRSI(candles)
            is Indicator.MACD -> IndicatorCalculations.calculateMACD(candles)
            is Indicator.BollingerBands -> IndicatorCalculations.calculateBollingerBands(candles, indicator.period, indicator.multiplier)
            is Indicator.Volume -> IndicatorCalculations.calculateVolumeMA(candles)
        }
    }
}
