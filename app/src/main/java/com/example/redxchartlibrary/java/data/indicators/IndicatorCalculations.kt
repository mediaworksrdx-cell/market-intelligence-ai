package com.example.redxchartlibrary.data.indicators

import com.example.redxchartlibrary.model.Candle

object IndicatorCalculations {

    fun calculateSMA(candles: List<Candle>, period: Int): IndicatorResult.Line {
        val smaValues = mutableListOf<LineData>()
        for (i in period - 1 until candles.size) {
            val avg = candles.subList(i - period + 1, i + 1).map { it.close }.average().toFloat()
            smaValues.add(LineData(candles[i].timestamp, avg))
        }
        return IndicatorResult.Line(smaValues)
    }

    fun calculateEMA(candles: List<Candle>, period: Int): IndicatorResult.Line {
        val emaValues = mutableListOf<LineData>()
        if (candles.isEmpty()) return IndicatorResult.Line(emaValues)

        val multiplier = 2f / (period + 1)
        var ema = candles.first().close

        emaValues.add(LineData(candles.first().timestamp, ema))

        for (i in 1 until candles.size) {
            ema = (candles[i].close - ema) * multiplier + ema
            emaValues.add(LineData(candles[i].timestamp, ema))
        }
        return IndicatorResult.Line(emaValues)
    }

    fun calculateRSI(candles: List<Candle>, period: Int = 14): IndicatorResult.Line {
        if (candles.size < period) return IndicatorResult.Line(emptyList())

        val rsiValues = mutableListOf<LineData>()
        var avgGain = 0f
        var avgLoss = 0f

        // Initial Averages
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period
        avgLoss /= period

        var rs = if (avgLoss != 0f) avgGain / avgLoss else Float.POSITIVE_INFINITY
        var rsi = 100f - (100f / (1 + rs))
        rsiValues.add(LineData(candles[period].timestamp, rsi))

        // Subsequent RSI values
        for (i in period + 1 until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0f
            val loss = if (change < 0) -change else 0f

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss != 0f) avgGain / avgLoss else Float.POSITIVE_INFINITY
            rsi = 100f - (100f / (1 + rs))
            rsiValues.add(LineData(candles[i].timestamp, rsi))
        }
        return IndicatorResult.Line(rsiValues)
    }
    
    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): IndicatorResult.MACD {
        if (candles.size < slowPeriod) return IndicatorResult.MACD(emptyList())

        val closePrices = candles.map { it.close }

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
        
        val macdData = mutableListOf<MacdData>()
        val offset = slowPeriod + signalPeriod - 2

        for (i in histogram.indices) {
            val candleIndex = offset + i
            if (candleIndex < candles.size) {
                macdData.add(
                    MacdData(
                        timestamp = candles[candleIndex].timestamp,
                        macdLine = macdLine[signalPeriod - 1 + i],
                        signalLine = signalLine[i],
                        histogram = histogram[i]
                    )
                )
            }
        }

        return IndicatorResult.MACD(macdData)
    }
}
