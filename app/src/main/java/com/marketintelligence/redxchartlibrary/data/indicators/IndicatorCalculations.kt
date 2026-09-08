package com.marketintelligence.redxchartlibrary.data.indicators

import com.marketintelligence.redxchartlibrary.model.Candle

object IndicatorCalculations {

    fun calculateSMA(candles: List<Candle>, period: Int): IndicatorOutput {
        val smaValues = mutableMapOf<Long, List<Float?>>()
        for (i in period - 1 until candles.size) {
            val avg = candles.subList(i - period + 1, i + 1).map { it.close }.average().toFloat()
            smaValues[candles[i].openTime] = listOf(avg)
        }
        return IndicatorOutput(smaValues)
    }

    fun calculateEMA(candles: List<Candle>, period: Int): IndicatorOutput {
        val emaValues = mutableMapOf<Long, List<Float?>>()
        if (candles.isEmpty()) return IndicatorOutput(emaValues)

        val multiplier = 2f / (period + 1)
        var ema = candles.first().close.toFloat()

        emaValues[candles.first().openTime] = listOf(ema)

        for (i in 1 until candles.size) {
            ema = (candles[i].close.toFloat() - ema) * multiplier + ema
            emaValues[candles[i].openTime] = listOf(ema)
        }
        return IndicatorOutput(emaValues)
    }

    fun calculateBollingerBands(candles: List<Candle>, period: Int, multiplier: Double): IndicatorOutput {
        val bbValues = mutableMapOf<Long, List<Float?>>()
        if (candles.size < period) return IndicatorOutput(bbValues)

        for (i in period - 1 until candles.size) {
            val sublist = candles.subList(i - period + 1, i + 1)
            val sma = sublist.map { it.close }.average().toFloat()
            val stdDev = kotlin.math.sqrt(sublist.map { (it.close - sma) * (it.close - sma) }.average().toFloat()).toFloat()
            val upperBand = sma + stdDev * multiplier.toFloat()
            val lowerBand = sma - stdDev * multiplier.toFloat()
            bbValues[candles[i].openTime] = listOf(sma, upperBand, lowerBand)
        }

        return IndicatorOutput(bbValues)
    }

    fun calculateRSI(candles: List<Candle>, period: Int = 14): IndicatorOutput {
        if (candles.size < period) return IndicatorOutput(emptyMap())

        val rsiValues = mutableMapOf<Long, List<Float?>>()
        var avgGain = 0f
        var avgLoss = 0f

        // Initial Averages
        for (i in 1..period) {
            val change = (candles[i].close - candles[i - 1].close).toFloat()
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period
        avgLoss /= period

        var rs = if (avgLoss != 0f) avgGain / avgLoss else Float.POSITIVE_INFINITY
        var rsi = 100f - (100f / (1 + rs))
        rsiValues[candles[period].openTime] = listOf(rsi)

        // Subsequent RSI values
        for (i in period + 1 until candles.size) {
            val change = (candles[i].close - candles[i - 1].close).toFloat()
            val gain = if (change > 0) change else 0f
            val loss = if (change < 0) -change else 0f

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss != 0f) avgGain / avgLoss else Float.POSITIVE_INFINITY
            rsi = 100f - (100f / (1 + rs))
            rsiValues[candles[i].openTime] = listOf(rsi)
        }
        return IndicatorOutput(rsiValues)
    }

    fun calculateVolumeMA(candles: List<Candle>, period: Int = 20): IndicatorOutput {
        val volumeMaValues = mutableMapOf<Long, List<Float?>>()
        for (i in period - 1 until candles.size) {
            val avg = candles.subList(i - period + 1, i + 1).map { it.volume }.average().toFloat()
            volumeMaValues[candles[i].openTime] = listOf(avg)
        }
        return IndicatorOutput(volumeMaValues)
    }

    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): IndicatorOutput {
        if (candles.size < slowPeriod) return IndicatorOutput(emptyMap())

        val closePrices = candles.map { it.close.toFloat() }

        fun ema(prices: List<Float>, period: Int): List<Float> {
            val emaList = mutableListOf<Float>()
            var ema = prices.first()
            emaList.add(ema)
            val multiplier = 2f / (period + 1)
            for (i in 1 until prices.size) {
                ema = (prices[i] - ema) * multiplier + ema
                emaList.add(ema)
            }
            return emaList
        }

        val emaFast = ema(closePrices, fastPeriod)
        val emaSlow = ema(closePrices, slowPeriod)

        val macdLine = emaFast.zip(emaSlow.drop(slowPeriod - fastPeriod)) { fast, slow -> fast - slow }
        val signalLine = ema(macdLine, signalPeriod)
        val histogram = macdLine.drop(signalPeriod - 1).zip(signalLine) { macd, signal -> macd - signal }

        val macdData = mutableMapOf<Long, List<Float?>>()
        val offset = slowPeriod + signalPeriod - 2

        for (i in histogram.indices) {
            val candleIndex = offset + i
            if (candleIndex < candles.size) {
                macdData[candles[candleIndex].openTime] = listOf(
                    macdLine[signalPeriod - 1 + i],
                    signalLine[i],
                    histogram[i]
                )
            }
        }

        return IndicatorOutput(macdData)
    }
}
