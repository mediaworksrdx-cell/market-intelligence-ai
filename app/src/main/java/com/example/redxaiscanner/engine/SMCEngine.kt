package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

class SMCEngine {

    private val swingPointLookback = 5
    private val liquidityThreshold = BigDecimal("0.0015") // 0.15% threshold for equal highs/lows

    fun analyze(candles: List<Candle>): SMCAnalysisResult {
        val emptyExplanation = ExplanationComponent("SMC Study", "Insufficient data", emptyMap())
        
        if (candles.size < swingPointLookback * 2 + 1) {
            return SMCAnalysisResult(MarketBias.RANGING, emptyList(), emptyList(), emptyList(), emptyList(), null, emptyExplanation)
        }

        val swingPoints = findSwingPoints(candles)
        if (swingPoints.size < 2) {
             return SMCAnalysisResult(MarketBias.RANGING, swingPoints, emptyList(), emptyList(), emptyList(), null, emptyExplanation)
        }
        
        val (bias, structureEvents) = determineStructureAndBias(swingPoints)
        // Rule Set 6: OB detection depends on Structure Events (BOS)
        val orderBlocks = findOrderBlocks(candles, structureEvents)
        // Rule Set 4: Liquidity Mapping
        val liquidityZones = findLiquidityZones(swingPoints)
        val pdZone = calculatePDZone(swingPoints)
        
        val explanation = ExplanationComponent(
            componentName = "Market Structure (SMC)",
            reasoning = "Institutional bias identified as ${bias.name} following ${structureEvents.lastOrNull()?.type?.name ?: "consolidation"}.",
            details = mapOf(
                "Bias" to bias.name,
                "Last Event" to (structureEvents.lastOrNull()?.let { "${it.type} @ ${it.price}" } ?: "None"),
                "Liquidity Pools" to "${liquidityZones.size} Zones",
                "Range Position" to if (pdZone != null && candles.last().close.toDouble() > pdZone.equilibrium.toDouble()) "PREMIUM" else "DISCOUNT"
            )
        )

        return SMCAnalysisResult(
            bias = bias,
            swingPoints = swingPoints,
            structureEvents = structureEvents,
            orderBlocks = orderBlocks,
            liquidityZones = liquidityZones,
            premiumDiscountZone = pdZone,
            explanation = explanation
        )
    }

    private fun findSwingPoints(candles: List<Candle>): List<SwingPoint> {
        val points = mutableListOf<SwingPoint>()
        for (i in swingPointLookback until candles.size - swingPointLookback) {
            val center = candles[i]
            val leftWindow = candles.subList(i - swingPointLookback, i)
            val rightWindow = candles.subList(i + 1, i + 1 + swingPointLookback)

            val isSwingHigh = leftWindow.all { it.high <= center.high } && rightWindow.all { it.high < center.high }
            val isSwingLow = leftWindow.all { it.low >= center.low } && rightWindow.all { it.low > center.low }

            if (isSwingHigh) {
                points.add(SwingPoint(SwingType.HIGH, BigDecimal.valueOf(center.high.toDouble()), center.timestamp))
            } else if (isSwingLow) {
                 points.add(SwingPoint(SwingType.LOW, BigDecimal.valueOf(center.low.toDouble()), center.timestamp))
            }
        }
        return points
    }
    
    // Rule Set 3: Structure Classification
    private fun determineStructureAndBias(swingPoints: List<SwingPoint>): Pair<MarketBias, List<MarketStructureEvent>> {
        val events = mutableListOf<MarketStructureEvent>()
        if (swingPoints.size < 3) return MarketBias.RANGING to events

        var lastHigh = swingPoints.firstOrNull { it.type == SwingType.HIGH } ?: return MarketBias.RANGING to events
        var lastLow = swingPoints.firstOrNull { it.type == SwingType.LOW } ?: return MarketBias.RANGING to events
        
        var isUptrend = lastHigh.price > lastLow.price

        for (point in swingPoints) {
            if (point.type == SwingType.HIGH) {
                if (point.price > lastHigh.price) {
                    if(isUptrend) {
                         events.add(MarketStructureEvent(EventType.BOS, point.timestamp, point.price, lastHigh.timestamp))
                    } else {
                         events.add(MarketStructureEvent(EventType.CHoCH, point.timestamp, point.price, lastHigh.timestamp))
                         isUptrend = true
                    }
                    lastHigh = point
                }
            } else {
                 if (point.price < lastLow.price) {
                    if(!isUptrend) {
                        events.add(MarketStructureEvent(EventType.BOS, point.timestamp, point.price, lastLow.timestamp))
                    } else {
                        events.add(MarketStructureEvent(EventType.CHoCH, point.timestamp, point.price, lastLow.timestamp))
                        isUptrend = false
                    }
                    lastLow = point
                }
            }
        }
        
        val bias = if (events.isEmpty()) MarketBias.RANGING else if (isUptrend) MarketBias.BULLISH else MarketBias.BEARISH
        return bias to events
    }

    // Rule Set 6: Order Block Detection
    private fun findOrderBlocks(candles: List<Candle>, events: List<MarketStructureEvent>): List<OrderBlock> {
        val obs = mutableListOf<OrderBlock>()
        val candleMap by lazy { candles.associateBy { it.timestamp } }

        for (event in events.filter { it.type == EventType.BOS || it.type == EventType.CHoCH }) {
            val eventCandle = candleMap[event.timestamp] ?: continue
            val candleIndex = candles.indexOf(eventCandle)
            if (candleIndex < 2) continue

            if (eventCandle.close > eventCandle.open) { // Bullish Break
                val obCandle = findLastOpposingCandle(candles, candleIndex, isBullishSearch = false)
                if (obCandle != null) {
                    if (isImpulsiveMove(candles, candles.indexOf(obCandle))) {
                        obs.add(OrderBlock(FVGDireciton.BULLISH, BigDecimal.valueOf(obCandle.high.toDouble()), BigDecimal.valueOf(obCandle.low.toDouble()), obCandle.timestamp))
                    }
                }
            } 
            else { // Bearish Break
                val obCandle = findLastOpposingCandle(candles, candleIndex, isBullishSearch = true)
                if (obCandle != null) {
                    if (isImpulsiveMove(candles, candles.indexOf(obCandle))) {
                        obs.add(OrderBlock(FVGDireciton.BEARISH, BigDecimal.valueOf(obCandle.high.toDouble()), BigDecimal.valueOf(obCandle.low.toDouble()), obCandle.timestamp))
                    }
                }
            }
        }
        return obs
    }

    private fun isImpulsiveMove(candles: List<Candle>, obIndex: Int): Boolean {
        if (obIndex >= candles.size - 1) return false
        val nextCandle = candles[obIndex + 1]
        val bodySize = (nextCandle.close - nextCandle.open).abs()
        val avgBodySize = candles.subList((obIndex - 5).coerceAtLeast(0), obIndex).map { (it.close - it.open).abs().toDouble() }.average()
        return bodySize.toDouble() > avgBodySize * 1.5
    }

    private fun findLiquidityZones(swingPoints: List<SwingPoint>): List<LiquidityZone> {
        val zones = mutableListOf<LiquidityZone>()
        val highs = swingPoints.filter { it.type == SwingType.HIGH }
        val lows = swingPoints.filter { it.type == SwingType.LOW }

        // Rule Set 4: Equal Highs/Lows
        for (i in highs.indices) {
            for (j in i + 1 until highs.size) {
                val diff = (highs[i].price - highs[j].price).abs()
                val avg = (highs[i].price + highs[j].price) / BigDecimal("2")
                if (diff / avg < liquidityThreshold) {
                    zones.add(LiquidityZone(LiquidityType.EQUAL_HIGHS, highs[i].price, highs[i].timestamp, highs[j].timestamp))
                }
            }
        }

        for (i in lows.indices) {
            for (j in i + 1 until lows.size) {
                val diff = (lows[i].price - lows[j].price).abs()
                val avg = (lows[i].price + lows[j].price) / BigDecimal("2")
                if (diff / avg < liquidityThreshold) {
                    zones.add(LiquidityZone(LiquidityType.EQUAL_LOWS, lows[i].price, lows[i].timestamp, lows[j].timestamp))
                }
            }
        }
        return zones
    }

    private fun calculatePDZone(swingPoints: List<SwingPoint>): PremiumDiscountZone? {
        if (swingPoints.size < 2) return null
        val recentHigh = swingPoints.filter { it.type == SwingType.HIGH }.lastOrNull()?.price ?: return null
        val recentLow = swingPoints.filter { it.type == SwingType.LOW }.lastOrNull()?.price ?: return null
        val eq = (recentHigh + recentLow) / BigDecimal("2")
        return PremiumDiscountZone(recentHigh, recentLow, eq)
    }
    
    private fun findLastOpposingCandle(candles: List<Candle>, startIndex: Int, isBullishSearch: Boolean): Candle? {
        for(i in startIndex downTo 1) {
            val c = candles[i]
            val isBullish = c.close > c.open
            if(isBullish == isBullishSearch) return c
        }
        return null
    }
}
