package com.marketintelligence.cryptotracker.engine

import com.marketintelligence.tradeengine.models.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CryptoSMCEngine(private val swingLookback: Int = 5) {

    fun analyze(candles: List<Candle>): CryptoSMCAnalysis {
        if (candles.size < swingLookback * 2 + 1) {
            return CryptoSMCAnalysis(
                structureState = StructureState.RANGING,
                swingPoints = emptyList(),
                structureEvents = emptyList(),
                liquidityLevels = emptyList(),
                sweeps = emptyList(),
                displacements = emptyList(),
                premiumDiscountZone = null,
                atrMap = emptyMap()
            )
        }

        val atrMap = calculateATR(candles)
        val swingPoints = detectSwingPoints(candles)

        if (swingPoints.size < 2) {
            return CryptoSMCAnalysis(
                structureState = StructureState.RANGING,
                swingPoints = swingPoints,
                structureEvents = emptyList(),
                liquidityLevels = emptyList(),
                sweeps = emptyList(),
                displacements = emptyList(),
                premiumDiscountZone = null,
                atrMap = atrMap
            )
        }

        val (structureState, events) = analyzeStructure(candles, swingPoints)
        val liquidityLevels = detectLiquidityLevels(swingPoints)
        val sweeps = detectSweeps(candles, liquidityLevels, atrMap)
        
        // Collect all displacements from sweeps for now
        val displacements = sweeps.mapNotNull { it.displacement }

        val lastHigh = swingPoints.findLast { it.type == SwingType.HIGH }
        val lastLow = swingPoints.findLast { it.type == SwingType.LOW }
        val premiumDiscountZone = if (lastHigh != null && lastLow != null) {
            PremiumDiscountZone(
                high = lastHigh.price,
                low = lastLow.price,
                equilibrium = (lastHigh.price + lastLow.price) / 2
            )
        } else null

        return CryptoSMCAnalysis(
            structureState = structureState,
            swingPoints = swingPoints,
            structureEvents = events,
            liquidityLevels = liquidityLevels,
            sweeps = sweeps,
            displacements = displacements,
            premiumDiscountZone = premiumDiscountZone,
            atrMap = atrMap
        )
    }

    private fun calculateATR(candles: List<Candle>): Map<Long, Double> {
        val period = 14
        if (candles.size <= period) return emptyMap()

        val atrMap = mutableMapOf<Long, Double>()
        var trSum = 0.0

        for (i in 1..period) {
            val tr = max(
                candles[i].high - candles[i].low,
                max(
                    abs(candles[i].high - candles[i - 1].close),
                    abs(candles[i].low - candles[i - 1].close)
                )
            )
            trSum += tr
        }

        var currentATR = trSum / period
        atrMap[candles[period].openTime] = currentATR

        for (i in (period + 1) until candles.size) {
            val tr = max(
                candles[i].high - candles[i].low,
                max(
                    abs(candles[i].high - candles[i - 1].close),
                    abs(candles[i].low - candles[i - 1].close)
                )
            )
            currentATR = (currentATR * (period - 1) + tr) / period
            atrMap[candles[i].openTime] = currentATR
        }
        return atrMap
    }

    private fun detectSwingPoints(candles: List<Candle>): List<CryptoSwingPoint> {
        val swings = mutableListOf<CryptoSwingPoint>()
        var lastHigh: CryptoSwingPoint? = null
        var lastLow: CryptoSwingPoint? = null

        for (i in swingLookback until candles.size - swingLookback) {
            val center = candles[i]

            var isSwingHigh = true
            for (j in i - swingLookback..i + swingLookback) {
                if (i != j && candles[j].high >= center.high) {
                    isSwingHigh = false
                    break
                }
            }

            if (isSwingHigh) {
                val classification = if (lastHigh != null) {
                    if (center.high > lastHigh.price) SwingClassification.HH else SwingClassification.LH
                } else SwingClassification.HH
                
                val sp = CryptoSwingPoint(
                    type = SwingType.HIGH,
                    price = center.high,
                    timestamp = center.openTime,
                    classification = classification
                )
                swings.add(sp)
                lastHigh = sp
            }

            var isSwingLow = true
            for (j in i - swingLookback..i + swingLookback) {
                if (i != j && candles[j].low <= center.low) {
                    isSwingLow = false
                    break
                }
            }

            if (isSwingLow) {
                val classification = if (lastLow != null) {
                    if (center.low < lastLow.price) SwingClassification.LL else SwingClassification.HL
                } else SwingClassification.LL
                
                val sp = CryptoSwingPoint(
                    type = SwingType.LOW,
                    price = center.low,
                    timestamp = center.openTime,
                    classification = classification
                )
                swings.add(sp)
                lastLow = sp
            }
        }
        return swings.sortedBy { it.timestamp }
    }

    private fun analyzeStructure(
        candles: List<Candle>,
        swingPoints: List<CryptoSwingPoint>
    ): Pair<StructureState, List<StructureEvent>> {
        var isUptrend: Boolean? = null
        val events = mutableListOf<StructureEvent>()
        
        var lastHigh: CryptoSwingPoint? = null
        var lastLow: CryptoSwingPoint? = null

        for (sp in swingPoints) {
            val spIndex = candles.indexOfFirst { it.openTime == sp.timestamp }
            if (spIndex == -1) continue

            if (sp.type == SwingType.HIGH) {
                if (lastHigh != null && sp.classification == SwingClassification.HH) {
                    val breakCandle = candles.drop(spIndex).firstOrNull { it.close > lastHigh!!.price }
                    if (breakCandle != null) {
                        val eventType = if (isUptrend == true) StructureEventType.BOS else StructureEventType.CHoCH
                        events.add(StructureEvent(
                            type = eventType,
                            direction = StructureDirection.BULLISH,
                            price = lastHigh!!.price,
                            timestamp = breakCandle.openTime,
                            confirmationType = ConfirmationType.CLOSE,
                            brokenLevelTimestamp = lastHigh!!.timestamp
                        ))
                        isUptrend = true
                    }
                }
                lastHigh = sp
            } else if (sp.type == SwingType.LOW) {
                if (lastLow != null && sp.classification == SwingClassification.LL) {
                    val breakCandle = candles.drop(spIndex).firstOrNull { it.close < lastLow!!.price }
                    if (breakCandle != null) {
                        val eventType = if (isUptrend == false) StructureEventType.BOS else StructureEventType.CHoCH
                        events.add(StructureEvent(
                            type = eventType,
                            direction = StructureDirection.BEARISH,
                            price = lastLow!!.price,
                            timestamp = breakCandle.openTime,
                            confirmationType = ConfirmationType.CLOSE,
                            brokenLevelTimestamp = lastLow!!.timestamp
                        ))
                        isUptrend = false
                    }
                }
                lastLow = sp
            }
        }

        val state = when (isUptrend) {
            true -> StructureState.BULLISH
            false -> StructureState.BEARISH
            null -> StructureState.RANGING
        }
        return Pair(state, events)
    }

    private fun detectLiquidityLevels(swingPoints: List<CryptoSwingPoint>): List<LiquidityLevel> {
        val levels = mutableListOf<LiquidityLevel>()
        val tolerance = 0.0015

        val highs = swingPoints.filter { it.type == SwingType.HIGH }
        for (i in highs.indices) {
            levels.add(LiquidityLevel(
                side = LiquiditySide.BUY,
                source = LiquiditySource.SWING_HIGH,
                price = highs[i].price,
                timestamp = highs[i].timestamp
            ))
            for (j in i + 1 until highs.size) {
                if (abs(highs[i].price - highs[j].price) / highs[i].price <= tolerance) {
                    levels.add(LiquidityLevel(
                        side = LiquiditySide.BUY,
                        source = LiquiditySource.EQUAL_HIGHS,
                        price = max(highs[i].price, highs[j].price),
                        timestamp = highs[j].timestamp
                    ))
                }
            }
        }

        val lows = swingPoints.filter { it.type == SwingType.LOW }
        for (i in lows.indices) {
            levels.add(LiquidityLevel(
                side = LiquiditySide.SELL,
                source = LiquiditySource.SWING_LOW,
                price = lows[i].price,
                timestamp = lows[i].timestamp
            ))
            for (j in i + 1 until lows.size) {
                if (abs(lows[i].price - lows[j].price) / lows[i].price <= tolerance) {
                    levels.add(LiquidityLevel(
                        side = LiquiditySide.SELL,
                        source = LiquiditySource.EQUAL_LOWS,
                        price = min(lows[i].price, lows[j].price),
                        timestamp = lows[j].timestamp
                    ))
                }
            }
        }
        return levels
    }

    private fun detectSweeps(
        candles: List<Candle>,
        levels: List<LiquidityLevel>,
        atrMap: Map<Long, Double>
    ): List<LiquiditySweep> {
        val sweeps = mutableListOf<LiquiditySweep>()

        for (level in levels) {
            val levelIdx = candles.indexOfFirst { it.openTime == level.timestamp }
            if (levelIdx == -1) continue

            for (i in levelIdx + 1 until candles.size) {
                val candle = candles[i]
                var swept = false
                var reclaimed = false

                if (level.side == LiquiditySide.BUY) {
                    if (candle.high > level.price && candle.close < level.price) {
                        swept = true
                        reclaimed = true
                    }
                } else {
                    if (candle.low < level.price && candle.close > level.price) {
                        swept = true
                        reclaimed = true
                    }
                }

                if (swept && reclaimed && i + 1 < candles.size) {
                    val dispCandle = candles[i + 1]
                    val atr = atrMap[dispCandle.openTime] ?: continue
                    val bodySize = abs(dispCandle.close - dispCandle.open)
                    
                    val displacement = if (bodySize > 1.5 * atr) {
                        DisplacementCandle(
                            direction = if (dispCandle.close > dispCandle.open) StructureDirection.BULLISH else StructureDirection.BEARISH,
                            bodySize = bodySize,
                            atrMultiple = bodySize / atr,
                            volume = dispCandle.volume,
                            timestamp = dispCandle.openTime
                        )
                    } else null

                    sweeps.add(LiquiditySweep(
                        level = level,
                        sweepPrice = if (level.side == LiquiditySide.BUY) candle.high else candle.low,
                        sweepTimestamp = candle.openTime,
                        reclaimed = true,
                        displacement = displacement
                    ))
                    break
                }
            }
        }
        return sweeps
    }
}
