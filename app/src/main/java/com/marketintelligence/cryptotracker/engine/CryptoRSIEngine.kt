package com.marketintelligence.cryptotracker.engine

import com.marketintelligence.tradeengine.models.Candle
import kotlin.math.abs

class CryptoRSIEngine(private val period: Int = 14) {
    fun analyze(
        candles: List<Candle>,
        structureState: StructureState,
        swingPoints: List<CryptoSwingPoint>
    ): CryptoRSIAnalysis {
        val rsiMap = calculateRSI(candles)
        
        if (candles.isEmpty() || rsiMap.isEmpty()) {
            return CryptoRSIAnalysis(
                currentRSI = 50.0,
                zone = RSIZone.WEAK_BULLISH,
                momentumDirection = MomentumDirection.NEUTRAL,
                snapshots = emptyMap(),
                divergences = emptyList(),
                confirmation = RSIConfirmation(
                    isConfirming = false,
                    zone = RSIZone.WEAK_BULLISH,
                    momentumDirection = MomentumDirection.NEUTRAL,
                    value = 50.0
                )
            )
        }
        
        val lastCandle = candles.last()
        val currentValue = rsiMap[lastCandle.openTime] ?: 50.0
        
        val zone = when {
            currentValue < 30 -> RSIZone.OVERSOLD
            currentValue <= 50 -> RSIZone.WEAK_BEARISH
            currentValue <= 70 -> RSIZone.WEAK_BULLISH
            else -> RSIZone.OVERBOUGHT
        }
        
        val momentum = when {
            structureState == StructureState.BULLISH && currentValue > 50 -> MomentumDirection.BULLISH
            structureState == StructureState.BEARISH && currentValue < 50 -> MomentumDirection.BEARISH
            else -> MomentumDirection.NEUTRAL
        }
        
        val isConfirming = (structureState == StructureState.BULLISH && currentValue > 50) ||
                           (structureState == StructureState.BEARISH && currentValue < 50)
                           
        val divergences = detectDivergences(swingPoints, rsiMap)
        
        val snapshot = RSISnapshot(
            value = currentValue,
            zone = zone,
            timestamp = lastCandle.openTime,
            timeframe = lastCandle.timeframe
        )
        
        val snapshots = mapOf(lastCandle.timeframe to snapshot)
        
        return CryptoRSIAnalysis(
            currentRSI = currentValue,
            zone = zone,
            momentumDirection = momentum,
            snapshots = snapshots,
            divergences = divergences,
            confirmation = RSIConfirmation(
                isConfirming = isConfirming,
                zone = zone,
                momentumDirection = momentum,
                value = currentValue
            )
        )
    }
    
    private fun calculateRSI(candles: List<Candle>): Map<Long, Double> {
        if (candles.size <= period) return emptyMap()
        
        val rsiMap = mutableMapOf<Long, Double>()
        var sumGain = 0.0
        var sumLoss = 0.0
        
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) sumGain += change
            else sumLoss += abs(change)
        }
        
        var avgGain = sumGain / period
        var avgLoss = sumLoss / period
        
        val initialRS = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        rsiMap[candles[period].openTime] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + initialRS))
        
        for (i in period + 1 until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0
            
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            rsiMap[candles[i].openTime] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
        }
        
        return rsiMap
    }
    
    private fun detectDivergences(
        swingPoints: List<CryptoSwingPoint>,
        rsiMap: Map<Long, Double>
    ): List<RSIDivergence> {
        val divergences = mutableListOf<RSIDivergence>()
        
        val lows = swingPoints.filter { it.type == SwingType.LOW }.takeLast(2)
        if (lows.size == 2) {
            val prevLow = lows[0]
            val currLow = lows[1]
            val prevRsi = rsiMap[prevLow.timestamp]
            val currRsi = rsiMap[currLow.timestamp]
            
            if (prevRsi != null && currRsi != null) {
                if (currLow.price < prevLow.price && currRsi > prevRsi) {
                    divergences.add(RSIDivergence(
                        type = DivergenceType.BULLISH,
                        priceSwingPrice = currLow.price,
                        priceSwingTimestamp = currLow.timestamp,
                        rsiSwingValue = currRsi,
                        rsiSwingTimestamp = currLow.timestamp
                    ))
                }
            }
        }
        
        val highs = swingPoints.filter { it.type == SwingType.HIGH }.takeLast(2)
        if (highs.size == 2) {
            val prevHigh = highs[0]
            val currHigh = highs[1]
            val prevRsi = rsiMap[prevHigh.timestamp]
            val currRsi = rsiMap[currHigh.timestamp]
            
            if (prevRsi != null && currRsi != null) {
                if (currHigh.price > prevHigh.price && currRsi < prevRsi) {
                    divergences.add(RSIDivergence(
                        type = DivergenceType.BEARISH,
                        priceSwingPrice = currHigh.price,
                        priceSwingTimestamp = currHigh.timestamp,
                        rsiSwingValue = currRsi,
                        rsiSwingTimestamp = currHigh.timestamp
                    ))
                }
            }
        }
        
        return divergences
    }
}
