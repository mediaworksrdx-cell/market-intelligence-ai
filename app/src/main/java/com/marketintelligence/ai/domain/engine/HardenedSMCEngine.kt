package com.marketintelligence.ai.domain.engine

import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SMCBias { BULLISH, BEARISH, NEUTRAL }
enum class SMCDirection { BULLISH, BEARISH }
enum class MitigationState { ACTIVE, TESTED, MITIGATED, INVALIDATED }

data class ValidatedFVG(
    val id: String,
    val direction: SMCDirection,
    val top: Double,
    val bottom: Double,
    val startTimestamp: Long,
    var endTimestamp: Long,
    var state: MitigationState = MitigationState.ACTIVE,
    val atrMultiplier: Double
)

data class ValidatedOrderBlock(
    val id: String,
    val direction: SMCDirection,
    val top: Double,
    val bottom: Double,
    val timestamp: Long,
    var endTimestamp: Long,
    var state: MitigationState = MitigationState.ACTIVE,
    val volumeRatio: Double
)

data class ValidatedStructureBreak(
    val type: String,
    val price: Double,
    val timestamp: Long,
    val isBullish: Boolean
)

data class ValidatedLiquidityPool(
    val type: String,
    val priceLevel: Double,
    val startTimestamp: Long,
    val endTimestamp: Long
)

data class InstitutionalSMCAnalysis(
    val bias: SMCBias,
    val fairValueGaps: List<ValidatedFVG>,
    val orderBlocks: List<ValidatedOrderBlock>,
    val structureBreaks: List<ValidatedStructureBreak>,
    val liquidityPools: List<ValidatedLiquidityPool>,
    val equilibriumPrice: Double,
    val premiumZoneHigh: Double,
    val discountZoneLow: Double
)

@Singleton
class HardenedSMCEngine @Inject constructor() {

    fun analyze(candles: List<Candle>): InstitutionalSMCAnalysis {
        if (candles.size < 15) {
            val lastClose = candles.lastOrNull()?.close ?: 0.0
            return InstitutionalSMCAnalysis(
                bias = SMCBias.NEUTRAL,
                fairValueGaps = emptyList(),
                orderBlocks = emptyList(),
                structureBreaks = emptyList(),
                liquidityPools = emptyList(),
                equilibriumPrice = lastClose,
                premiumZoneHigh = lastClose,
                discountZoneLow = lastClose
            )
        }

        val atrs = calculateATR(candles, 14)
        val volumeMas = calculateVolumeMA(candles, 20)

        val fvgs = detectFVGs(candles, atrs)
        val orderBlocks = detectOrderBlocks(candles, volumeMas)
        val structureBreaks = detectStructureBreaks(candles)
        val liquidityPools = detectLiquidityPools(candles)

        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        val equilibrium = (minPrice + maxPrice) / 2.0

        val bias = when {
            structureBreaks.lastOrNull()?.isBullish == true -> SMCBias.BULLISH
            structureBreaks.lastOrNull()?.isBullish == false -> SMCBias.BEARISH
            candles.last().close >= equilibrium -> SMCBias.BULLISH
            else -> SMCBias.BEARISH
        }

        return InstitutionalSMCAnalysis(
            bias = bias,
            fairValueGaps = fvgs,
            orderBlocks = orderBlocks,
            structureBreaks = structureBreaks,
            liquidityPools = liquidityPools,
            equilibriumPrice = equilibrium,
            premiumZoneHigh = maxPrice,
            discountZoneLow = minPrice
        )
    }

    private fun detectFVGs(candles: List<Candle>, atrs: Map<Long, Double>): List<ValidatedFVG> {
        val fvgs = mutableListOf<ValidatedFVG>()

        for (i in 2 until candles.size) {
            val c1 = candles[i - 2]
            val c2 = candles[i - 1]
            val c3 = candles[i]
            val atr = atrs[c2.openTime] ?: (c2.high - c2.low)

            if (c3.low > c1.high) {
                val gapHeight = c3.low - c1.high
                if (gapHeight >= 0.4 * atr) {
                    val fvg = ValidatedFVG(
                        id = "BULL_FVG_" + c2.openTime,
                        direction = SMCDirection.BULLISH,
                        top = c3.low,
                        bottom = c1.high,
                        startTimestamp = c2.openTime,
                        endTimestamp = candles.last().openTime,
                        atrMultiplier = gapHeight / (atr.takeIf { it > 0 } ?: 1.0)
                    )
                    for (k in (i + 1) until candles.size) {
                        val sub = candles[k]
                        if (sub.close < fvg.bottom) {
                            fvg.state = MitigationState.INVALIDATED
                            fvg.endTimestamp = sub.openTime
                            break
                        } else if (sub.close <= fvg.top) {
                            fvg.state = MitigationState.MITIGATED
                            fvg.endTimestamp = sub.openTime
                        } else if (sub.low <= fvg.top && fvg.state == MitigationState.ACTIVE) {
                            fvg.state = MitigationState.TESTED
                        }
                    }
                    fvgs.add(fvg)
                }
            }

            if (c1.low > c3.high) {
                val gapHeight = c1.low - c3.high
                if (gapHeight >= 0.4 * atr) {
                    val fvg = ValidatedFVG(
                        id = "BEAR_FVG_" + c2.openTime,
                        direction = SMCDirection.BEARISH,
                        top = c1.low,
                        bottom = c3.high,
                        startTimestamp = c2.openTime,
                        endTimestamp = candles.last().openTime,
                        atrMultiplier = gapHeight / (atr.takeIf { it > 0 } ?: 1.0)
                    )
                    for (k in (i + 1) until candles.size) {
                        val sub = candles[k]
                        if (sub.close > fvg.top) {
                            fvg.state = MitigationState.INVALIDATED
                            fvg.endTimestamp = sub.openTime
                            break
                        } else if (sub.close >= fvg.bottom) {
                            fvg.state = MitigationState.MITIGATED
                            fvg.endTimestamp = sub.openTime
                        } else if (sub.high >= fvg.bottom && fvg.state == MitigationState.ACTIVE) {
                            fvg.state = MitigationState.TESTED
                        }
                    }
                    fvgs.add(fvg)
                }
            }
        }
        return fvgs
    }

    private fun detectOrderBlocks(candles: List<Candle>, volumeMas: Map<Long, Double>): List<ValidatedOrderBlock> {
        val obs = mutableListOf<ValidatedOrderBlock>()

        for (i in 2 until (candles.size - 1)) {
            val candle = candles[i]
            val nextCandle = candles[i + 1]
            val volMa = volumeMas[nextCandle.openTime] ?: nextCandle.volume

            val isHighVolumeBreak = nextCandle.volume >= 1.2 * volMa

            if (candle.close < candle.open && nextCandle.close > candle.high && isHighVolumeBreak) {
                val ob = ValidatedOrderBlock(
                    id = "BULL_OB_" + candle.openTime,
                    direction = SMCDirection.BULLISH,
                    top = candle.high,
                    bottom = candle.low,
                    timestamp = candle.openTime,
                    endTimestamp = candles.last().openTime,
                    volumeRatio = nextCandle.volume / (volMa.takeIf { it > 0 } ?: 1.0)
                )
                for (k in (i + 2) until candles.size) {
                    val sub = candles[k]
                    if (sub.close < ob.bottom) {
                        ob.state = MitigationState.INVALIDATED
                        ob.endTimestamp = sub.openTime
                        break
                    } else if (sub.low <= ob.top) {
                        ob.state = MitigationState.MITIGATED
                        ob.endTimestamp = sub.openTime
                        break
                    }
                }
                obs.add(ob)
            }

            if (candle.close > candle.open && nextCandle.close < candle.low && isHighVolumeBreak) {
                val ob = ValidatedOrderBlock(
                    id = "BEAR_OB_" + candle.openTime,
                    direction = SMCDirection.BEARISH,
                    top = candle.high,
                    bottom = candle.low,
                    timestamp = candle.openTime,
                    endTimestamp = candles.last().openTime,
                    volumeRatio = nextCandle.volume / (volMa.takeIf { it > 0 } ?: 1.0)
                )
                for (k in (i + 2) until candles.size) {
                    val sub = candles[k]
                    if (sub.close > ob.top) {
                        ob.state = MitigationState.INVALIDATED
                        ob.endTimestamp = sub.openTime
                        break
                    } else if (sub.high >= ob.bottom) {
                        ob.state = MitigationState.MITIGATED
                        ob.endTimestamp = sub.openTime
                        break
                    }
                }
                obs.add(ob)
            }
        }
        return obs
    }

    private fun detectStructureBreaks(candles: List<Candle>): List<ValidatedStructureBreak> {
        val breaks = mutableListOf<ValidatedStructureBreak>()
        var lastSwingHigh = Double.MIN_VALUE
        var lastSwingLow = Double.MAX_VALUE

        for (i in 2 until candles.size) {
            val prev = candles[i - 1]
            val curr = candles[i]

            if (i >= 4 && candles[i - 2].high > candles[i - 4].high && candles[i - 2].high > candles[i - 3].high &&
                candles[i - 2].high > candles[i - 1].high && candles[i - 2].high > candles[i].high) {
                lastSwingHigh = candles[i - 2].high
            }

            if (i >= 4 && candles[i - 2].low < candles[i - 4].low && candles[i - 2].low < candles[i - 3].low &&
                candles[i - 2].low < candles[i - 1].low && candles[i - 2].low < candles[i].low) {
                lastSwingLow = candles[i - 2].low
            }

            if (lastSwingHigh > Double.MIN_VALUE && curr.close > lastSwingHigh && prev.close <= lastSwingHigh) {
                breaks.add(
                    ValidatedStructureBreak(
                        type = "BOS ↑",
                        price = lastSwingHigh,
                        timestamp = curr.openTime,
                        isBullish = true
                    )
                )
                lastSwingHigh = Double.MIN_VALUE
            }

            if (lastSwingLow < Double.MAX_VALUE && curr.close < lastSwingLow && prev.close >= lastSwingLow) {
                breaks.add(
                    ValidatedStructureBreak(
                        type = "BOS ↓",
                        price = lastSwingLow,
                        timestamp = curr.openTime,
                        isBullish = false
                    )
                )
                lastSwingLow = Double.MAX_VALUE
            }
        }
        return breaks
    }

    private fun detectLiquidityPools(candles: List<Candle>): List<ValidatedLiquidityPool> {
        val pools = mutableListOf<ValidatedLiquidityPool>()
        val tolerance = 0.0015

        for (i in 3 until candles.size) {
            val c1 = candles[i - 3]
            val c2 = candles[i]

            if (abs(c1.high - c2.high) / c1.high <= tolerance) {
                pools.add(
                    ValidatedLiquidityPool(
                        type = "EQH (Buy-Side Liquidity)",
                        priceLevel = max(c1.high, c2.high),
                        startTimestamp = c1.openTime,
                        endTimestamp = candles.last().openTime
                    )
                )
            }

            if (abs(c1.low - c2.low) / c1.low <= tolerance) {
                pools.add(
                    ValidatedLiquidityPool(
                        type = "EQL (Sell-Side Liquidity)",
                        priceLevel = min(c1.low, c2.low),
                        startTimestamp = c1.openTime,
                        endTimestamp = candles.last().openTime
                    )
                )
            }
        }
        return pools
    }

    private fun calculateATR(candles: List<Candle>, period: Int): Map<Long, Double> {
        val atrs = HashMap<Long, Double>(candles.size)
        if (candles.size <= period) return atrs

        var atr = 0.0
        for (i in 1..period) {
            val tr = max(candles[i].high - candles[i].low, max(abs(candles[i].high - candles[i - 1].close), abs(candles[i].low - candles[i - 1].close)))
            atr += tr
        }
        atr /= period
        atrs[candles[period].openTime] = atr

        for (i in (period + 1) until candles.size) {
            val tr = max(candles[i].high - candles[i].low, max(abs(candles[i].high - candles[i - 1].close), abs(candles[i].low - candles[i - 1].close)))
            atr = (atr * (period - 1) + tr) / period
            atrs[candles[i].openTime] = atr
        }
        return atrs
    }

    private fun calculateVolumeMA(candles: List<Candle>, period: Int): Map<Long, Double> {
        val mas = HashMap<Long, Double>(candles.size)
        for (i in (period - 1) until candles.size) {
            var sum = 0.0
            for (j in (i - period + 1)..i) sum += candles[j].volume
            mas[candles[i].openTime] = sum / period
        }
        return mas
    }
}
