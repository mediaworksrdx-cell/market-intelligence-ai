package com.marketintelligence.cryptotracker.engine

import com.marketintelligence.tradeengine.models.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CryptoFVGEngine {
    fun analyze(
        candles: List<Candle>,
        smcAnalysis: CryptoSMCAnalysis,
        htfBias: StructureState,
        rsiValue: Double
    ): List<EnrichedFVG> {
        val fvgs = mutableListOf<EnrichedFVG>()
        if (candles.isEmpty()) return fvgs

        val symbol = candles[0].symbol
        val timeframe = candles[0].timeframe
        
        for (i in 2 until candles.size) {
            val candle1 = candles[i - 2]
            val candle2 = candles[i - 1]
            val candle3 = candles[i]
            
            val atr = smcAnalysis.atrMap[candle3.openTime] ?: continue
            val minGapSize = 0.3 * atr
            
            var direction: FVGDirection? = null
            var top = 0.0
            var bottom = 0.0
            
            if (candle1.high < candle3.low) {
                top = candle3.low
                bottom = candle1.high
                if (top - bottom >= minGapSize) {
                    direction = FVGDirection.BULLISH
                }
            } else if (candle1.low > candle3.high) {
                top = candle1.low
                bottom = candle3.high
                if (top - bottom >= minGapSize) {
                    direction = FVGDirection.BEARISH
                }
            }
            
            if (direction != null) {
                val (status, filledPercent) = determineStatus(candles, i + 1, direction, top, bottom)
                val midpoint = (top + bottom) / 2
                val sizePercent = ((top - bottom) / midpoint) * 100
                val age = candles.size - 1 - i
                
                val bodySize2 = abs(candle2.close - candle2.open)
                val isDisplacement = bodySize2 > 1.5 * (smcAnalysis.atrMap[candle2.openTime] ?: atr)
                
                val associatedBOS = smcAnalysis.structureEvents.any { 
                    it.type == StructureEventType.BOS && abs(candles.indexOfFirst { c -> c.openTime == it.timestamp } - i) <= 5 
                }
                
                val associatedSweep = smcAnalysis.sweeps.any {
                    val sweepIdx = candles.indexOfFirst { c -> c.openTime == it.sweepTimestamp }
                    sweepIdx in (i - 5)..i
                }
                
                val htfAlignment = (direction == FVGDirection.BULLISH && htfBias == StructureState.BULLISH) ||
                                   (direction == FVGDirection.BEARISH && htfBias == StructureState.BEARISH)
                
                var volSum = 0.0
                var count = 0
                for (j in max(0, i - 1 - 20) until i - 1) {
                    volSum += candles[j].volume
                    count++
                }
                val avgVol = if (count > 0) volSum / count else candle2.volume
                val volumeRatio = if (avgVol > 0) candle2.volume / avgVol else 1.0
                
                val score = calculateScore(
                    htfAlignmentVal = htfAlignment,
                    associatedSweep = associatedSweep,
                    associatedBOS = associatedBOS,
                    displacement = isDisplacement,
                    age = age,
                    sizePercent = sizePercent,
                    volumeRatio = volumeRatio,
                    direction = direction,
                    rsiValue = rsiValue
                )
                
                val idPrefix = if (direction == FVGDirection.BULLISH) "BULL_FVG_" else "BEAR_FVG_"
                val id = "$idPrefix${candle2.openTime}"

                fvgs.add(EnrichedFVG(
                    id = id,
                    symbol = symbol,
                    timeframe = timeframe,
                    direction = direction,
                    createdTime = candle2.openTime,
                    top = top,
                    bottom = bottom,
                    sizePercent = sizePercent,
                    age = age,
                    filledPercent = filledPercent,
                    displacement = isDisplacement,
                    associatedBOS = associatedBOS,
                    associatedSweep = associatedSweep,
                    htfAlignment = htfAlignment,
                    volumeRatio = volumeRatio,
                    status = status,
                    qualityScore = score
                ))
            }
        }
        
        return fvgs
    }
    
    private fun determineStatus(
        candles: List<Candle>, 
        startIndex: Int, 
        direction: FVGDirection, 
        top: Double, 
        bottom: Double
    ): Pair<FVGStatus, Double> {
        var status = FVGStatus.NEW
        if (startIndex >= candles.size) return Pair(status, 0.0)
        
        status = FVGStatus.ACTIVE
        var maxTraversed = 0.0
        val gapSize = top - bottom
        
        for (i in startIndex until candles.size) {
            val candle = candles[i]
            
            if (direction == FVGDirection.BULLISH) {
                if (candle.close < bottom) {
                    return Pair(FVGStatus.INVALIDATED, 100.0)
                } else if (candle.close <= top) {
                    status = FVGStatus.MITIGATED
                    maxTraversed = max(maxTraversed, top - candle.close)
                } else if (candle.low < top) {
                    val traversed = top - candle.low
                    maxTraversed = max(maxTraversed, traversed)
                    if (status != FVGStatus.MITIGATED) {
                        status = if (maxTraversed / gapSize >= 0.5) FVGStatus.PARTIALLY_FILLED else FVGStatus.TOUCHED
                    }
                }
            } else {
                if (candle.close > top) {
                    return Pair(FVGStatus.INVALIDATED, 100.0)
                } else if (candle.close >= bottom) {
                    status = FVGStatus.MITIGATED
                    maxTraversed = max(maxTraversed, candle.close - bottom)
                } else if (candle.high > bottom) {
                    val traversed = candle.high - bottom
                    maxTraversed = max(maxTraversed, traversed)
                    if (status != FVGStatus.MITIGATED) {
                        status = if (maxTraversed / gapSize >= 0.5) FVGStatus.PARTIALLY_FILLED else FVGStatus.TOUCHED
                    }
                }
            }
        }
        
        val filledPercent = (maxTraversed / gapSize) * 100
        return Pair(status, min(filledPercent, 100.0))
    }
    
    private fun calculateScore(
        htfAlignmentVal: Boolean,
        associatedSweep: Boolean,
        associatedBOS: Boolean,
        displacement: Boolean,
        age: Int,
        sizePercent: Double,
        volumeRatio: Double,
        direction: FVGDirection,
        rsiValue: Double
    ): FVGQualityScore {
        val sHtfAlignment = if (htfAlignmentVal) 20 else 0
        val sLiquiditySweep = if (associatedSweep) 20 else 0
        val sBosMss = if (associatedBOS) 20 else 0
        val sDisplacement = if (displacement) 15 else 0
        
        val sFreshness = when {
            age <= 3 -> 10
            age <= 6 -> 7
            age <= 12 -> 4
            age <= 20 -> 1
            else -> 0
        }
        
        val sSize = min(5.0, sizePercent).toInt()
        val sVolume = min(5.0, volumeRatio).toInt()
        
        val rsiConfirming = (direction == FVGDirection.BULLISH && rsiValue > 50) || 
                            (direction == FVGDirection.BEARISH && rsiValue < 50)
        val sRsiConfirmation = if (rsiConfirming) 5 else 0
        
        val total = sHtfAlignment + sLiquiditySweep + sBosMss + sDisplacement + sFreshness + sSize + sVolume + sRsiConfirmation
        
        return FVGQualityScore(
            htfAlignment = sHtfAlignment,
            liquiditySweep = sLiquiditySweep,
            bosMss = sBosMss,
            displacement = sDisplacement,
            freshness = sFreshness,
            size = sSize,
            volume = sVolume,
            rsiConfirmation = sRsiConfirmation,
            total = min(total, 100)
        )
    }
}
