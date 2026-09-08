package com.marketintelligence.cryptotracker.scanner

import com.marketintelligence.cryptotracker.engine.*

/**
 * The consolidated result of a single asset/timeframe scan.
 */
data class ScanResult(
    val symbol: String,
    val timeframe: String,
    val htfBias: StructureState,
    val lastSweep: LiquiditySweep?,
    val lastBOS: StructureEvent?,
    val activeFVG: EnrichedFVG?,
    val rsiValue: Double,
    val score: Int,
    val grade: SetupGrade,
    val signal: SetupDirection?,
    val setup: CryptoSetup?
)

/**
 * An aggregate overview across multiple scanned assets and timeframes.
 */
data class MarketOverview(
    val scanResults: List<ScanResult>,
    val topLongSetups: List<ScanResult>,
    val topShortSetups: List<ScanResult>,
    val activeFVGCount: Int,
    val recentSweepCount: Int,
    val structureEventCount: Int,
    val timestamp: Long
) {
    companion object {
        /**
         * Factory method that processes raw scan results into a comprehensive MarketOverview.
         */
        fun from(results: List<ScanResult>): MarketOverview {
            val topLongSetups = results
                .filter { it.signal == SetupDirection.LONG }
                .sortedByDescending { it.score }
            
            val topShortSetups = results
                .filter { it.signal == SetupDirection.SHORT }
                .sortedByDescending { it.score }

            val activeFVGCount = results.count { it.activeFVG != null }
            val recentSweepCount = results.count { it.lastSweep != null }
            val structureEventCount = results.count { it.lastBOS != null }

            return MarketOverview(
                scanResults = results,
                topLongSetups = topLongSetups,
                topShortSetups = topShortSetups,
                activeFVGCount = activeFVGCount,
                recentSweepCount = recentSweepCount,
                structureEventCount = structureEventCount,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
