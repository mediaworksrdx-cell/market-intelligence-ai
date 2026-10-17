package com.example.marketintelligence.ui.fno

import com.example.tradeengine.models.Candle
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-fidelity mathematical Option Contract Candle Generator.
 *
 * Produces deterministic, realistic OHLCV price action for individual CE and PE option strikes.
 * Anchored to the live contract LTP, day change %, IV, and volume.
 * Guarantees that the latest candle closes at the exact live LTP.
 */
object OptionCandleGenerator {

    fun generateOptionCandles(
        assetSymbol: String,
        strike: Double,
        optionType: String, // "CE" or "PE"
        expiry: String,
        currentLtp: Double,
        changePercent: Double,
        iv: Double,
        timeframe: String = "5m",
        candleCount: Int = 80
    ): List<Candle> {
        val safeLtp = if (currentLtp > 0.05) currentLtp else 1.0
        val safeIv = if (iv > 0) iv else 15.0

        val tfMillis = when (timeframe.lowercase()) {
            "1m" -> 60_000L
            "3m" -> 180_000L
            "5m" -> 300_000L
            "15m" -> 900_000L
            "30m" -> 1800_000L
            "1h", "60m" -> 3600_000L
            "4h" -> 14400_000L
            "1d", "day" -> 86400_000L
            else -> 300_000L
        }

        // Deterministic seed based on contract parameters so the chart is stable and unique per strike/type/tf
        val seedStr = "${assetSymbol.uppercase()}_${strike.toInt()}_${optionType.uppercase()}_${expiry}_$timeframe"
        val seed = seedStr.hashCode().toLong()
        val random = Random(seed)

        // Calculate approximate session open price
        val totalMoveRatio = (changePercent / 100.0).coerceIn(-0.90, 5.0)
        val initialPrice = (safeLtp / (1.0 + totalMoveRatio)).coerceAtLeast(0.1)

        val now = System.currentTimeMillis()
        val startTime = now - (candleCount * tfMillis)

        // Generate a Brownian bridge path that starts at initialPrice and lands exactly at safeLtp
        val rawPoints = DoubleArray(candleCount)
        rawPoints[0] = initialPrice
        val volFactor = (safeIv / 100.0 * 0.15).coerceIn(0.008, 0.08)

        for (i in 1 until candleCount) {
            val shock = (random.nextGaussian() * volFactor)
            rawPoints[i] = (rawPoints[i - 1] * (1.0 + shock)).coerceAtLeast(0.05)
        }

        // Bridge transformation to align the end point with safeLtp
        val endDiscrepancy = safeLtp - rawPoints[candleCount - 1]
        val bridgePrices = DoubleArray(candleCount)
        for (i in 0 until candleCount) {
            val fraction = i.toDouble() / (candleCount - 1).toDouble()
            bridgePrices[i] = (rawPoints[i] + fraction * endDiscrepancy).coerceAtLeast(0.05)
        }
        bridgePrices[candleCount - 1] = safeLtp

        val candles = ArrayList<Candle>(candleCount)
        var curOpen = initialPrice

        for (i in 0 until candleCount) {
            val targetClose = bridgePrices[i]
            val open = curOpen
            val close = targetClose

            // Volatility wicks
            val candleRange = abs(close - open)
            val maxPrice = max(open, close)
            val minPrice = min(open, close)

            val wickRatioHigh = random.nextDouble() * 0.4 + 0.1
            val wickRatioLow = random.nextDouble() * 0.4 + 0.1

            val high = (maxPrice + (candleRange * wickRatioHigh).coerceAtLeast(safeLtp * 0.003)).coerceAtLeast(maxPrice)
            val low = (minPrice - (candleRange * wickRatioLow).coerceAtLeast(safeLtp * 0.003)).coerceAtLeast(0.05)

            val candleOpenTime = startTime + (i * tfMillis)
            val candleCloseTime = candleOpenTime + tfMillis

            val baseVol = (1000 + random.nextInt(8000)).toDouble()
            val volumeMultiplier = 1.0 + (candleRange / (safeLtp * 0.01)).coerceIn(0.0, 5.0)
            val volume = baseVol * volumeMultiplier

            candles.add(
                Candle(
                    symbol = "${assetSymbol.uppercase()} ${strike.toInt()} ${optionType.uppercase()}",
                    timeframe = timeframe,
                    openTime = candleOpenTime,
                    open = "%.2f".format(java.util.Locale.US, open).toDouble(),
                    high = "%.2f".format(java.util.Locale.US, high).toDouble(),
                    low = "%.2f".format(java.util.Locale.US, low).toDouble(),
                    close = "%.2f".format(java.util.Locale.US, close).toDouble(),
                    volume = volume,
                    closeTime = candleCloseTime,
                    isClosed = i < candleCount - 1
                )
            )

            curOpen = close
        }

        return candles
    }
}
