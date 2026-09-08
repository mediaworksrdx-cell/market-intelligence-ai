package com.marketintelligence.ai.domain.engine

import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class BollingerBandsResult(
    val middleSma: Double,
    val upperBand: Double,
    val lowerBand: Double
)

data class MacdResult(
    val macdLine: Double,
    val signalLine: Double,
    val histogram: Double
)

data class SuperTrendResult(
    val value: Double,
    val isBullish: Boolean
)

data class InstitutionalIndicatorOutputs(
    val ema9: Map<Long, Double> = emptyMap(),
    val ema20: Map<Long, Double> = emptyMap(),
    val ema50: Map<Long, Double> = emptyMap(),
    val ema200: Map<Long, Double> = emptyMap(),
    val bollingerBands: Map<Long, BollingerBandsResult> = emptyMap(),
    val vwap: Map<Long, Double> = emptyMap(),
    val rsi14: Map<Long, Double> = emptyMap(),
    val macd: Map<Long, MacdResult> = emptyMap(),
    val superTrend: Map<Long, SuperTrendResult> = emptyMap(),
    val volumeMa20: Map<Long, Double> = emptyMap(),
    val heikinAshiCandles: List<Candle> = emptyList()
)

@Singleton
class InstitutionalIndicatorEngine @Inject constructor() {

    fun computeAll(candles: List<Candle>): InstitutionalIndicatorOutputs {
        if (candles.isEmpty()) return InstitutionalIndicatorOutputs()

        return InstitutionalIndicatorOutputs(
            ema9 = computeEMA(candles, 9),
            ema20 = computeEMA(candles, 20),
            ema50 = computeEMA(candles, 50),
            ema200 = computeEMA(candles, 200),
            bollingerBands = computeBollingerBands(candles, 20, 2.0),
            vwap = computeVWAP(candles),
            rsi14 = computeWildersRSI(candles, 14),
            macd = computeMACD(candles, 12, 26, 9),
            superTrend = computeSuperTrend(candles, 10, 3.0),
            volumeMa20 = computeVolumeMA(candles, 20),
            heikinAshiCandles = computeHeikinAshi(candles)
        )
    }

    fun computeEMA(candles: List<Candle>, period: Int): Map<Long, Double> {
        if (candles.isEmpty() || period <= 0) return emptyMap()
        val result = HashMap<Long, Double>(candles.size)
        val k = 2.0 / (period + 1.0)
        
        var ema = if (candles.size >= period) {
            candles.take(period).sumOf { it.close } / period
        } else {
            candles.first().close
        }

        val startIndex = if (candles.size >= period) {
            result[candles[period - 1].openTime] = ema
            period
        } else {
            result[candles.first().openTime] = ema
            1
        }

        for (i in startIndex until candles.size) {
            val c = candles[i]
            ema = (c.close - ema) * k + ema
            result[c.openTime] = ema
        }
        return result
    }

    fun computeBollingerBands(candles: List<Candle>, period: Int = 20, numStdDev: Double = 2.0): Map<Long, BollingerBandsResult> {
        if (candles.size < period) return emptyMap()
        val result = HashMap<Long, BollingerBandsResult>(candles.size)

        for (i in period - 1 until candles.size) {
            var sum = 0.0
            for (j in (i - period + 1)..i) {
                sum += candles[j].close
            }
            val sma = sum / period

            var varianceSum = 0.0
            for (j in (i - period + 1)..i) {
                val diff = candles[j].close - sma
                varianceSum += diff * diff
            }
            val stdDev = sqrt(varianceSum / period)

            result[candles[i].openTime] = BollingerBandsResult(
                middleSma = sma,
                upperBand = sma + (stdDev * numStdDev),
                lowerBand = sma - (stdDev * numStdDev)
            )
        }
        return result
    }

    fun computeWildersRSI(candles: List<Candle>, period: Int = 14): Map<Long, Double> {
        if (candles.size <= period) return emptyMap()
        val result = HashMap<Long, Double>(candles.size)

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 1..period) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff >= 0.0) gainSum += diff else lossSum += -diff
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        val firstRsi = if (avgGain == 0.0 && avgLoss == 0.0) 50.0 else if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
        result[candles[period].openTime] = firstRsi

        for (i in (period + 1) until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            val currentGain = if (diff > 0.0) diff else 0.0
            val currentLoss = if (diff < 0.0) -diff else 0.0

            avgGain = (avgGain * (period - 1) + currentGain) / period
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period

            val rsi = if (avgGain == 0.0 && avgLoss == 0.0) 50.0 else if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))
            result[candles[i].openTime] = rsi
        }
        return result
    }

    fun computeMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Map<Long, MacdResult> {
        if (candles.size < slowPeriod) return emptyMap()

        val fastK = 2.0 / (fastPeriod + 1.0)
        val slowK = 2.0 / (slowPeriod + 1.0)
        val signalK = 2.0 / (signalPeriod + 1.0)

        var fastEma = candles.first().close
        var slowEma = candles.first().close

        val macdList = ArrayList<Pair<Long, Double>>(candles.size)

        for (i in candles.indices) {
            val c = candles[i]
            fastEma = (c.close - fastEma) * fastK + fastEma
            slowEma = (c.close - slowEma) * slowK + slowEma

            if (i >= slowPeriod - 1) {
                val macd = fastEma - slowEma
                macdList.add(c.openTime to macd)
            }
        }

        if (macdList.isEmpty()) return emptyMap()

        val result = HashMap<Long, MacdResult>(macdList.size)
        var signalEma = macdList.first().second

        for (i in macdList.indices) {
            val (openTime, macd) = macdList[i]
            signalEma = (macd - signalEma) * signalK + signalEma
            val hist = macd - signalEma
            result[openTime] = MacdResult(macdLine = macd, signalLine = signalEma, histogram = hist)
        }
        return result
    }

    fun computeVWAP(candles: List<Candle>): Map<Long, Double> {
        if (candles.isEmpty()) return emptyMap()
        val result = HashMap<Long, Double>(candles.size)

        var cumulativeTypicalVolume = 0.0
        var cumulativeVolume = 0.0

        for (c in candles) {
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            val vol = if (c.volume > 0.0) c.volume else 1.0
            cumulativeTypicalVolume += typicalPrice * vol
            cumulativeVolume += vol

            val vwap = if (cumulativeVolume > 0.0) cumulativeTypicalVolume / cumulativeVolume else c.close
            result[c.openTime] = vwap
        }
        return result
    }

    fun computeSuperTrend(candles: List<Candle>, period: Int = 10, multiplier: Double = 3.0): Map<Long, SuperTrendResult> {
        if (candles.size <= period) return emptyMap()
        val result = HashMap<Long, SuperTrendResult>(candles.size)

        val tr = DoubleArray(candles.size)
        tr[0] = candles[0].high - candles[0].low
        for (i in 1 until candles.size) {
            val hl = candles[i].high - candles[i].low
            val hpc = abs(candles[i].high - candles[i - 1].close)
            val lpc = abs(candles[i].low - candles[i - 1].close)
            tr[i] = max(hl, max(hpc, lpc))
        }

        var atr = 0.0
        for (i in 0 until period) atr += tr[i]
        atr /= period

        var prevFinalUpper = (candles[period].high + candles[period].low) / 2.0 + (multiplier * atr)
        var prevFinalLower = (candles[period].high + candles[period].low) / 2.0 - (multiplier * atr)
        var isBullish = candles[period].close > prevFinalUpper

        result[candles[period].openTime] = SuperTrendResult(
            value = if (isBullish) prevFinalLower else prevFinalUpper,
            isBullish = isBullish
        )

        for (i in period until candles.size) {
            val c = candles[i]
            atr = (atr * (period - 1) + tr[i]) / period

            val hl2 = (c.high + c.low) / 2.0
            val basicUpper = hl2 + (multiplier * atr)
            val basicLower = hl2 - (multiplier * atr)

            val finalUpper = if (basicUpper < prevFinalUpper || candles[i - 1].close > prevFinalUpper) basicUpper else prevFinalUpper
            val finalLower = if (basicLower > prevFinalLower || candles[i - 1].close < prevFinalLower) basicLower else prevFinalLower

            isBullish = if (isBullish) {
                c.close >= finalLower
            } else {
                c.close > finalUpper
            }

            val superTrendVal = if (isBullish) finalLower else finalUpper
            result[c.openTime] = SuperTrendResult(value = superTrendVal, isBullish = isBullish)

            prevFinalUpper = finalUpper
            prevFinalLower = finalLower
        }
        return result
    }

    fun computeVolumeMA(candles: List<Candle>, period: Int = 20): Map<Long, Double> {
        if (candles.size < period) return emptyMap()
        val result = HashMap<Long, Double>(candles.size)

        for (i in period - 1 until candles.size) {
            var sum = 0.0
            for (j in (i - period + 1)..i) {
                sum += candles[j].volume
            }
            result[candles[i].openTime] = sum / period
        }
        return result
    }

    fun computeHeikinAshi(candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()
        val haCandles = ArrayList<Candle>(candles.size)

        val first = candles.first()
        var prevHaOpen = (first.open + first.close) / 2.0
        var prevHaClose = (first.open + first.high + first.low + first.close) / 4.0

        haCandles.add(
            first.copy(
                open = prevHaOpen,
                close = prevHaClose,
                high = max(first.high, max(prevHaOpen, prevHaClose)),
                low = min(first.low, min(prevHaOpen, prevHaClose))
            )
        )

        for (i in 1 until candles.size) {
            val c = candles[i]
            val haClose = (c.open + c.high + c.low + c.close) / 4.0
            val haOpen = (prevHaOpen + prevHaClose) / 2.0
            val haHigh = max(c.high, max(haOpen, haClose))
            val haLow = min(c.low, min(haOpen, haClose))

            haCandles.add(
                c.copy(
                    open = haOpen,
                    close = haClose,
                    high = haHigh,
                    low = haLow
                )
            )
            prevHaOpen = haOpen
            prevHaClose = haClose
        }
        return haCandles
    }
}
