package com.example.marketintelligence.domain.chart

import com.example.tradeengine.models.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure computation class for technical indicator calculations.
 * All methods are stateless and produce results from raw candle data.
 * No Android or UI dependencies — fully unit-testable.
 */
object IndicatorCalculator {

    // ── Simple Moving Average ──
    // ── Simple Moving Average ──
    fun calculateSMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val p = period.coerceAtLeast(1)
        if (candles.size < p) {
            // Progressive cumulative moving average over available history so line renders gracefully
            return List(candles.size) { i ->
                var sum = 0.0
                for (j in 0..i) sum += candles[j].close
                sum / (i + 1)
            }
        }
        return List(candles.size) { i ->
            if (i < p - 1) null
            else {
                var sum = 0.0
                for (j in (i - p + 1)..i) sum += candles[j].close
                sum / p
            }
        }
    }

    // ── Exponential Moving Average ──
    fun calculateEMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val p = period.coerceAtLeast(1)
        val multiplier = 2.0 / (p + 1)
        val result = MutableList<Double?>(candles.size) { null }

        val effectiveSeed = minOf(p, candles.size)
        var sum = 0.0
        for (i in 0 until effectiveSeed) sum += candles[i].close
        result[effectiveSeed - 1] = sum / effectiveSeed

        for (i in effectiveSeed until candles.size) {
            val prevEma = result[i - 1] ?: candles[i - 1].close
            result[i] = (candles[i].close - prevEma) * multiplier + prevEma
        }
        return result
    }

    // ── Relative Strength Index ──
    fun calculateRSI(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.size < 2) return List(candles.size) { null }
        val p = period.coerceIn(1, maxOf(1, candles.size - 1))
        val result = MutableList<Double?>(candles.size) { null }

        // Calculate initial average gains and losses
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..p) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= p
        avgLoss /= p

        result[p] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))

        // Smoothed RSI
        for (i in (p + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            avgGain = (avgGain * (p - 1) + gain) / p
            avgLoss = (avgLoss * (p - 1) + loss) / p
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }
        return result
    }

    // ── MACD ──
    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MACDResult {
        val fastEma = calculateEMA(candles, fastPeriod)
        val slowEma = calculateEMA(candles, slowPeriod)
        val size = candles.size

        val macdLine = MutableList<Double?>(size) { null }
        for (i in 0 until size) {
            val fast = fastEma[i]
            val slow = slowEma[i]
            macdLine[i] = if (fast != null && slow != null) fast - slow else null
        }

        // Calculate signal line (EMA of MACD line)
        val macdValues = macdLine.filterNotNull()
        val signalLine = MutableList<Double?>(size) { null }
        val histogram = MutableList<Double?>(size) { null }

        if (macdValues.size >= signalPeriod) {
            val macdStartIndex = macdLine.indexOfFirst { it != null }
            val multiplier = 2.0 / (signalPeriod + 1)

            // Seed signal with SMA of first signalPeriod MACD values
            var signalSum = 0.0
            for (i in 0 until signalPeriod) signalSum += macdValues[i]
            val seedIndex = macdStartIndex + signalPeriod - 1
            if (seedIndex < size) {
                signalLine[seedIndex] = signalSum / signalPeriod
                histogram[seedIndex] = macdLine[seedIndex]!! - signalLine[seedIndex]!!

                for (i in (seedIndex + 1) until size) {
                    val macdVal = macdLine[i]
                    val prevSignal = signalLine[i - 1]
                    if (macdVal != null && prevSignal != null) {
                        signalLine[i] = (macdVal - prevSignal) * multiplier + prevSignal
                        histogram[i] = macdVal - signalLine[i]!!
                    }
                }
            }
        }
        return MACDResult(macdLine, signalLine, histogram)
    }

    // ── Bollinger Bands ──
    fun calculateBollingerBands(
        candles: List<Candle>,
        period: Int = 20,
        deviation: Double = 2.0
    ): BollingerResult {
        if (candles.isEmpty()) return BollingerResult(emptyList(), emptyList(), emptyList())
        val p = period.coerceIn(1, maxOf(1, candles.size))
        val sma = calculateSMA(candles, p)
        val upper = MutableList<Double?>(candles.size) { null }
        val lower = MutableList<Double?>(candles.size) { null }

        for (i in candles.indices) {
            val mid = sma[i] ?: continue
            val window = minOf(p, i + 1)
            var sumSqDiff = 0.0
            for (j in (i - window + 1)..i) {
                val diff = candles[j].close - mid
                sumSqDiff += diff * diff
            }
            val stdDev = sqrt(sumSqDiff / window)
            upper[i] = mid + deviation * stdDev
            lower[i] = mid - deviation * stdDev
        }
        return BollingerResult(upper, sma, lower)
    }

    // ── VWAP (Volume Weighted Average Price) ──
    fun calculateVWAP(candles: List<Candle>): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        var cumulativeTPV = 0.0  // Typical Price × Volume
        var cumulativeVolume = 0.0
        var lastDayKey = -1L

        for (i in candles.indices) {
            val candle = candles[i]
            val rawTime = candle.openTime
            val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
            // Detect daily session boundary (IST UTC+5:30 offset = 19,800,000 ms)
            val dayKey = if (timeMs > 0L) (timeMs + 19_800_000L) / 86_400_000L else 0L

            if (lastDayKey != -1L && dayKey != lastDayKey) {
                // Session reset: start fresh each trading day as standard in financial charting
                cumulativeTPV = 0.0
                cumulativeVolume = 0.0
            }
            lastDayKey = dayKey

            val typicalPrice = (candle.high + candle.low + candle.close) / 3.0
            val volume = if (candle.volume > 0.0) candle.volume else 1.0
            cumulativeTPV += typicalPrice * volume
            cumulativeVolume += volume
            result[i] = if (cumulativeVolume > 0.0) cumulativeTPV / cumulativeVolume else typicalPrice
        }
        return result
    }

    // ── Supertrend ──
    fun calculateSupertrend(
        candles: List<Candle>,
        period: Int = 10,
        multiplier: Double = 3.0
    ): SupertrendResult {
        val p = period.coerceIn(1, maxOf(1, candles.size))
        val atr = calculateATR(candles, p)
        val values = MutableList<Double?>(candles.size) { null }
        val directions = MutableList<Boolean?>(candles.size) { null }

        if (candles.isEmpty()) return SupertrendResult(values, directions)
        val startIdx = (p - 1).coerceIn(0, candles.size - 1)

        var prevUpperBand = 0.0
        var prevLowerBand = 0.0
        var prevSupertrend = 0.0
        var _prevDirection = true

        for (i in startIdx until candles.size) {
            val atrVal = atr[i] ?: continue
            val hl2 = (candles[i].high + candles[i].low) / 2.0
            var upperBand = hl2 + multiplier * atrVal
            var lowerBand = hl2 - multiplier * atrVal

            // Band continuity
            if (i > startIdx) {
                if (lowerBand > prevLowerBand || candles[i - 1].close < prevLowerBand) {
                    // keep lowerBand
                } else {
                    lowerBand = prevLowerBand
                }
                if (upperBand < prevUpperBand || candles[i - 1].close > prevUpperBand) {
                    // keep upperBand
                } else {
                    upperBand = prevUpperBand
                }

                // Direction determination
                val direction = if (prevSupertrend == prevUpperBand) {
                    candles[i].close > upperBand
                } else {
                    candles[i].close >= lowerBand
                }

                val supertrend = if (direction) lowerBand else upperBand
                values[i] = supertrend
                directions[i] = direction
                prevSupertrend = supertrend
                _prevDirection = direction
            } else {
                // First value
                val direction = candles[i].close > hl2
                val supertrend = if (direction) lowerBand else upperBand
                values[i] = supertrend
                directions[i] = direction
                prevSupertrend = supertrend
                _prevDirection = direction
            }
            prevUpperBand = upperBand
            prevLowerBand = lowerBand
        }
        return SupertrendResult(values, directions)
    }

    // ── Average True Range ──
    fun calculateATR(candles: List<Candle>, period: Int = 14): List<Double?> {
        if (candles.size < 2) return List(candles.size) { null }
        val p = period.coerceIn(1, maxOf(1, candles.size - 1))
        val result = MutableList<Double?>(candles.size) { null }

        // Calculate True Range values
        val trValues = MutableList(candles.size) { 0.0 }
        trValues[0] = candles[0].high - candles[0].low
        for (i in 1 until candles.size) {
            val highLow = candles[i].high - candles[i].low
            val highPrevClose = abs(candles[i].high - candles[i - 1].close)
            val lowPrevClose = abs(candles[i].low - candles[i - 1].close)
            trValues[i] = max(highLow, max(highPrevClose, lowPrevClose))
        }

        // First ATR is simple average
        var sum = 0.0
        for (i in 0 until p) sum += trValues[i]
        result[p - 1] = sum / p

        // Smoothed ATR
        for (i in p until candles.size) {
            val prevAtr = result[i - 1] ?: (sum / p)
            result[i] = (prevAtr * (p - 1) + trValues[i]) / p
        }
        return result
    }

    // ── Stochastic Oscillator ──
    fun calculateStochastic(
        candles: List<Candle>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): StochasticResult {
        val size = candles.size
        val kLine = MutableList<Double?>(size) { null }
        val dLine = MutableList<Double?>(size) { null }
        if (size == 0) return StochasticResult(kLine, dLine)

        val effectiveK = kPeriod.coerceIn(1, size)
        val effectiveD = dPeriod.coerceIn(1, size)

        // Calculate %K
        for (i in (effectiveK - 1) until size) {
            var highestHigh = Double.MIN_VALUE
            var lowestLow = Double.MAX_VALUE
            for (j in (i - effectiveK + 1)..i) {
                highestHigh = max(highestHigh, candles[j].high)
                lowestLow = min(lowestLow, candles[j].low)
            }
            val range = highestHigh - lowestLow
            kLine[i] = if (range > 0) ((candles[i].close - lowestLow) / range) * 100.0 else 50.0
        }

        // Calculate %D (SMA of %K)
        val kValues = kLine.filterNotNull()
        if (kValues.size >= effectiveD) {
            val kStartIndex = kLine.indexOfFirst { it != null }
            for (i in (kStartIndex + effectiveD - 1) until size) {
                var sum = 0.0
                var count = 0
                for (j in (i - effectiveD + 1)..i) {
                    kLine[j]?.let { sum += it; count++ }
                }
                if (count == effectiveD) dLine[i] = sum / effectiveD
            }
        }
        return StochasticResult(kLine, dLine)
    }

    // ── Ichimoku Cloud ──
    fun calculateIchimoku(
        candles: List<Candle>,
        tenkanPeriod: Int = 9,
        kijunPeriod: Int = 26,
        senkouBPeriod: Int = 52
    ): IchimokuResult {
        val size = candles.size
        val tenkanSen = MutableList<Double?>(size) { null }
        val kijunSen = MutableList<Double?>(size) { null }
        val senkouSpanA = MutableList<Double?>(size + kijunPeriod) { null }
        val senkouSpanB = MutableList<Double?>(size + kijunPeriod) { null }
        val chikouSpan = MutableList<Double?>(size) { null }

        // Helper: calculate midpoint of highest high and lowest low over period
        fun midpoint(endIndex: Int, period: Int): Double? {
            if (endIndex < period - 1) return null
            var highest = Double.MIN_VALUE
            var lowest = Double.MAX_VALUE
            for (j in (endIndex - period + 1)..endIndex) {
                highest = max(highest, candles[j].high)
                lowest = min(lowest, candles[j].low)
            }
            return (highest + lowest) / 2.0
        }

        for (i in candles.indices) {
            tenkanSen[i] = midpoint(i, tenkanPeriod)
            kijunSen[i] = midpoint(i, kijunPeriod)

            // Senkou Span A: (Tenkan + Kijun) / 2, plotted kijunPeriod ahead
            val tenkan = tenkanSen[i]
            val kijun = kijunSen[i]
            if (tenkan != null && kijun != null) {
                val futureIndex = i + kijunPeriod
                senkouSpanA[futureIndex] = (tenkan + kijun) / 2.0
            }

            // Senkou Span B: midpoint of 52-period, plotted kijunPeriod ahead
            val spanB = midpoint(i, senkouBPeriod)
            if (spanB != null) {
                val futureIndex = i + kijunPeriod
                senkouSpanB[futureIndex] = spanB
            }

            // Chikou Span: close price plotted kijunPeriod behind
            val pastIndex = i - kijunPeriod
            if (pastIndex >= 0) {
                chikouSpan[pastIndex] = candles[i].close
            }
        }
        return IchimokuResult(tenkanSen, kijunSen, senkouSpanA, senkouSpanB, chikouSpan)
    }

    // ── Cumulative Volume Delta (CVD) ──
    fun calculateCVD(candles: List<Candle>): CvdResult {
        if (candles.isEmpty()) return CvdResult(emptyList(), emptyList())
        val deltaBars = ArrayList<Double?>(candles.size)
        val cvdLine = ArrayList<Double?>(candles.size)
        var cumulative = 0.0

        for (c in candles) {
            val range = c.high - c.low
            val delta = if (range > 0.0) {
                val buyerWeight = (c.close - c.low) / range
                val sellerWeight = (c.high - c.close) / range
                c.volume * (buyerWeight - sellerWeight)
            } else {
                0.0
            }
            cumulative += delta
            deltaBars.add(delta)
            cvdLine.add(cumulative)
        }
        return CvdResult(cvdLine, deltaBars)
    }

    // ── Volume Profile (VPVR) ──
    fun calculateVolumeProfile(
        candles: List<Candle>,
        priceMin: Double,
        priceMax: Double,
        bucketCount: Int = 40
    ): VolumeProfileData {
        if (candles.isEmpty() || priceMin >= priceMax || bucketCount <= 0) {
            return VolumeProfileData()
        }

        val step = (priceMax - priceMin) / bucketCount
        val buckets = Array(bucketCount) { i ->
            val pLow = priceMin + i * step
            val pHigh = pLow + step
            VolumeProfileBucket(priceLow = pLow, priceHigh = pHigh, buyVolume = 0.0, sellVolume = 0.0, totalVolume = 0.0)
        }

        for (c in candles) {
            if (c.high < priceMin || c.low > priceMax) continue
            val range = max(c.high - c.low, 1e-9)
            val buyFrac = ((c.close - c.low) / range).coerceIn(0.0, 1.0)
            val buyVol = c.volume * buyFrac
            val sellVol = c.volume * (1.0 - buyFrac)

            for (i in 0 until bucketCount) {
                val b = buckets[i]
                val overlapLow = max(b.priceLow, c.low)
                val overlapHigh = min(b.priceHigh, c.high)
                if (overlapHigh > overlapLow) {
                    val portion = (overlapHigh - overlapLow) / range
                    val bBuy = buyVol * portion
                    val bSell = sellVol * portion
                    buckets[i] = b.copy(
                        buyVolume = b.buyVolume + bBuy,
                        sellVolume = b.sellVolume + bSell,
                        totalVolume = b.totalVolume + bBuy + bSell
                    )
                }
            }
        }

        val bucketList = buckets.toList()
        var maxVol = 0.0
        var pocIdx = 0
        var totalVol = 0.0

        for (i in bucketList.indices) {
            val vol = bucketList[i].totalVolume
            totalVol += vol
            if (vol > maxVol) {
                maxVol = vol
                pocIdx = i
            }
        }

        val pocPrice = (bucketList[pocIdx].priceLow + bucketList[pocIdx].priceHigh) / 2.0

        // 70% Value Area calculation
        val targetVaVol = totalVol * 0.70
        var currentVaVol = maxVol
        var vaLowIdx = pocIdx
        var vaHighIdx = pocIdx

        while (currentVaVol < targetVaVol && (vaLowIdx > 0 || vaHighIdx < bucketCount - 1)) {
            val nextLowVol = if (vaLowIdx > 0) bucketList[vaLowIdx - 1].totalVolume else -1.0
            val nextHighVol = if (vaHighIdx < bucketCount - 1) bucketList[vaHighIdx + 1].totalVolume else -1.0

            if (nextHighVol >= nextLowVol && nextHighVol >= 0.0) {
                vaHighIdx++
                currentVaVol += nextHighVol
            } else if (nextLowVol >= 0.0) {
                vaLowIdx--
                currentVaVol += nextLowVol
            } else {
                break
            }
        }

        val vahPrice = bucketList[vaHighIdx].priceHigh
        val valPrice = bucketList[vaLowIdx].priceLow

        return VolumeProfileData(
            buckets = bucketList,
            pocPrice = pocPrice,
            vahPrice = vahPrice,
            valPrice = valPrice,
            maxBucketVolume = maxVol
        )
    }

    // ── Smart Money Concepts (SMC: FVG & Liquidity Sweeps) ──
    fun detectSMC(candles: List<Candle>): SmcAnalysis {
        if (candles.size < 5) return SmcAnalysis()

        val fvgs = mutableListOf<SmcFvg>()
        val sweeps = mutableListOf<SmcLiquiditySweep>()

        // 1. Fair Value Gaps (3-candle pattern)
        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val _c2 = candles[i - 1]
            val c3 = candles[i]

            // Bullish FVG: c3 low > c1 high
            if (c3.low > c1.high) {
                val fvg = SmcFvg(
                    startIndex = i - 1,
                    endIndex = candles.size - 1,
                    topPrice = c3.low,
                    bottomPrice = c1.high,
                    isBullish = true,
                    isMitigated = false
                )
                // Check future mitigation
                for (k in (i + 1) until candles.size) {
                    if (candles[k].low <= fvg.bottomPrice) {
                        fvg.isMitigated = true
                        fvg.endIndex = k
                        break
                    }
                }
                fvgs.add(fvg)
            }
            // Bearish FVG: c3 high < c1 low
            else if (c3.high < c1.low) {
                val fvg = SmcFvg(
                    startIndex = i - 1,
                    endIndex = candles.size - 1,
                    topPrice = c1.low,
                    bottomPrice = c3.high,
                    isBullish = false,
                    isMitigated = false
                )
                // Check future mitigation
                for (k in (i + 1) until candles.size) {
                    if (candles[k].high >= fvg.topPrice) {
                        fvg.isMitigated = true
                        fvg.endIndex = k
                        break
                    }
                }
                fvgs.add(fvg)
            }
        }

        // 2. Liquidity Sweeps (Pivot high/low sweep & rejection)
        val pivotPeriod = 5
        for (i in (pivotPeriod * 2) until candles.size) {
            val candidateHigh = candles[i - pivotPeriod].high
            val candidateLow = candles[i - pivotPeriod].low

            var isPivotHigh = true
            var isPivotLow = true
            for (p in (i - pivotPeriod * 2) until i) {
                if (p != i - pivotPeriod) {
                    if (candles[p].high > candidateHigh) isPivotHigh = false
                    if (candles[p].low < candidateLow) isPivotLow = false
                }
            }

            val cur = candles[i]
            if (isPivotHigh && cur.high > candidateHigh && cur.close < candidateHigh) {
                sweeps.add(SmcLiquiditySweep(candleIndex = i, price = cur.high, isHighSweep = true, label = "BSL Sweep ($$$)"))
            }
            if (isPivotLow && cur.low < candidateLow && cur.close > candidateLow) {
                sweeps.add(SmcLiquiditySweep(candleIndex = i, price = cur.low, isHighSweep = false, label = "SSL Sweep ($$$)"))
            }
        }

        return SmcAnalysis(fvgs = fvgs, sweeps = sweeps)
    }

    // ── Parabolic SAR ──
    fun calculateParabolicSAR(
        candles: List<Candle>,
        step: Double = 0.02,
        maxAf: Double = 0.20
    ): List<Double?> {
        if (candles.size < 2) return List(candles.size) { null }
        val sar = MutableList<Double?>(candles.size) { null }

        var isUptrend = candles[1].close > candles[0].close
        var ep = if (isUptrend) max(candles[0].high, candles[1].high) else min(candles[0].low, candles[1].low)
        var af = step
        sar[1] = if (isUptrend) min(candles[0].low, candles[1].low) else max(candles[0].high, candles[1].high)

        for (i in 2 until candles.size) {
            val prevSar = sar[i - 1]!!
            var currentSar = prevSar + af * (ep - prevSar)

            if (isUptrend) {
                currentSar = min(currentSar, min(candles[i - 1].low, candles[i - 2].low))
                if (candles[i].low < currentSar) {
                    isUptrend = false
                    sar[i] = ep
                    ep = candles[i].low
                    af = step
                } else {
                    sar[i] = currentSar
                    if (candles[i].high > ep) {
                        ep = candles[i].high
                        af = min(af + step, maxAf)
                    }
                }
            } else {
                currentSar = max(currentSar, max(candles[i - 1].high, candles[i - 2].high))
                if (candles[i].high > currentSar) {
                    isUptrend = true
                    sar[i] = ep
                    ep = candles[i].high
                    af = step
                } else {
                    sar[i] = currentSar
                    if (candles[i].low < ep) {
                        ep = candles[i].low
                        af = min(af + step, maxAf)
                    }
                }
            }
        }
        return sar
    }

    // ── ADX (Average Directional Index) ──
    fun calculateADX(candles: List<Candle>, period: Int = 14): AdxResult {
        val size = candles.size
        val plusDI = MutableList<Double?>(size) { null }
        val minusDI = MutableList<Double?>(size) { null }
        val adx = MutableList<Double?>(size) { null }
        if (size < 2) return AdxResult(plusDI, minusDI, adx)
        val p = period.coerceIn(2, size - 1)

        val tr = MutableList(size) { 0.0 }
        val plusDM = MutableList(size) { 0.0 }
        val minusDM = MutableList(size) { 0.0 }

        for (i in 1 until size) {
            val highDiff = candles[i].high - candles[i - 1].high
            val lowDiff = candles[i - 1].low - candles[i].low

            tr[i] = max(
                candles[i].high - candles[i].low,
                max(abs(candles[i].high - candles[i - 1].close), abs(candles[i].low - candles[i - 1].close))
            )

            plusDM[i] = if (highDiff > lowDiff && highDiff > 0) highDiff else 0.0
            minusDM[i] = if (lowDiff > highDiff && lowDiff > 0) lowDiff else 0.0
        }

        var trSum = 0.0
        var plusDMSum = 0.0
        var minusDMSum = 0.0

        for (i in 1..p) {
            trSum += tr[i]
            plusDMSum += plusDM[i]
            minusDMSum += minusDM[i]
        }

        val dx = MutableList<Double?>(size) { null }

        for (i in p until size) {
            if (i > p) {
                trSum = trSum - (trSum / p) + tr[i]
                plusDMSum = plusDMSum - (plusDMSum / p) + plusDM[i]
                minusDMSum = minusDMSum - (minusDMSum / p) + minusDM[i]
            }

            val pdi = if (trSum == 0.0) 0.0 else 100 * plusDMSum / trSum
            val mdi = if (trSum == 0.0) 0.0 else 100 * minusDMSum / trSum
            plusDI[i] = pdi
            minusDI[i] = mdi

            val dxVal = if (pdi + mdi == 0.0) 0.0 else 100 * abs(pdi - mdi) / (pdi + mdi)
            dx[i] = dxVal
        }

        var dxSum = 0.0
        var dxCount = 0
        for (i in p until size) {
            dxSum += dx[i]!!
            dxCount++
            if (dxCount == p) {
                adx[i] = dxSum / p
            } else if (dxCount > p) {
                adx[i] = ((adx[i - 1]!! * (p - 1)) + dx[i]!!) / p
            }
        }

        return AdxResult(plusDI, minusDI, adx)
    }

    // ── OBV (On-Balance Volume) ──
    fun calculateOBV(candles: List<Candle>): List<Double?> {
        val size = candles.size
        if (size == 0) return emptyList()
        val obv = MutableList<Double?>(size) { null }
        var currentObv = 0.0
        obv[0] = currentObv

        for (i in 1 until size) {
            if (candles[i].close > candles[i - 1].close) {
                currentObv += candles[i].volume
            } else if (candles[i].close < candles[i - 1].close) {
                currentObv -= candles[i].volume
            }
            obv[i] = currentObv
        }
        return obv
    }

    // ── CCI (Commodity Channel Index) ──
    fun calculateCCI(candles: List<Candle>, period: Int = 20): List<Double?> {
        val size = candles.size
        val cci = MutableList<Double?>(size) { null }
        if (size == 0) return cci
        val p = period.coerceIn(1, size)

        val tp = candles.map { (it.high + it.low + it.close) / 3.0 }

        for (i in (p - 1) until size) {
            var sum = 0.0
            for (j in (i - p + 1)..i) {
                sum += tp[j]
            }
            val sma = sum / p

            var meanDevSum = 0.0
            for (j in (i - p + 1)..i) {
                meanDevSum += abs(tp[j] - sma)
            }
            val meanDev = meanDevSum / p

            cci[i] = if (meanDev == 0.0) 0.0 else (tp[i] - sma) / (0.015 * meanDev)
        }
        return cci
    }

    // ── Williams %R ──
    fun calculateWilliamsR(candles: List<Candle>, period: Int = 14): List<Double?> {
        val size = candles.size
        val r = MutableList<Double?>(size) { null }
        if (size == 0) return r
        val p = period.coerceIn(1, size)

        for (i in (p - 1) until size) {
            var highestHigh = Double.MIN_VALUE
            var lowestLow = Double.MAX_VALUE

            for (j in (i - p + 1)..i) {
                highestHigh = max(highestHigh, candles[j].high)
                lowestLow = min(lowestLow, candles[j].low)
            }

            val denom = highestHigh - lowestLow
            r[i] = if (denom == 0.0) 0.0 else (highestHigh - candles[i].close) / denom * -100.0
        }
        return r
    }

    // ── MFI (Money Flow Index) ──
    fun calculateMFI(candles: List<Candle>, period: Int = 14): List<Double?> {
        val size = candles.size
        val mfi = MutableList<Double?>(size) { null }
        if (size < 2) return mfi
        val p = period.coerceIn(1, size - 1)

        val tp = candles.map { (it.high + it.low + it.close) / 3.0 }
        val rmf = DoubleArray(size) { i -> tp[i] * candles[i].volume }

        for (i in p until size) {
            var posFlow = 0.0
            var negFlow = 0.0

            for (j in (i - p + 1)..i) {
                if (tp[j] > tp[j - 1]) {
                    posFlow += rmf[j]
                } else if (tp[j] < tp[j - 1]) {
                    negFlow += rmf[j]
                }
            }

            mfi[i] = if (negFlow == 0.0) {
                100.0
            } else {
                val mfr = posFlow / negFlow
                100.0 - (100.0 / (1.0 + mfr))
            }
        }
        return mfi
    }
}
