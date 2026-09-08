package com.marketintelligence.cryptotracker.scanner

import com.marketintelligence.cryptotracker.engine.*
import com.marketintelligence.tradeengine.models.Candle

class CryptoMarketScanner(
    private val confluenceEngine: CryptoConfluenceEngine
) {
    companion object {
        val DEFAULT_SYMBOLS = listOf(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "LINKUSDT", "SUIUSDT"
        )
    }

    /**
     * Scan a single symbol across timeframes.
     */
    fun scanSymbol(
        symbol: String,
        htfCandles: List<Candle>,
        mtfCandles: List<Candle>,
        ltfCandles: List<Candle>? = null
    ): ScanResult {
        val setup = confluenceEngine.analyze(symbol, htfCandles, mtfCandles, ltfCandles)
        val timeframe = mtfCandles.firstOrNull()?.timeframe ?: "15m"
        
        if (setup != null) {
            val rsiVal = setup.rsiAnalysis.currentRSI
            return ScanResult(
                symbol = symbol,
                timeframe = timeframe,
                htfBias = setup.htfBias,
                lastSweep = setup.smcAnalysis.sweeps.lastOrNull(),
                lastBOS = setup.smcAnalysis.structureEvents.lastOrNull { it.type == StructureEventType.BOS },
                activeFVG = setup.fvg,
                rsiValue = rsiVal,
                score = setup.score.total,
                grade = setup.grade,
                signal = setup.direction,
                setup = setup
            )
        } else {
            // Run basic SMC & RSI analysis for fallback
            val basicAnalysis = confluenceEngine.smcEngine.analyze(mtfCandles)
            val htfAnalysis = confluenceEngine.smcEngine.analyze(htfCandles)
            val rsiAnalysis = confluenceEngine.rsiEngine.analyze(mtfCandles, basicAnalysis.structureState, basicAnalysis.swingPoints)
            
            return ScanResult(
                symbol = symbol,
                timeframe = timeframe,
                htfBias = htfAnalysis.structureState,
                lastSweep = basicAnalysis.sweeps.lastOrNull(),
                lastBOS = basicAnalysis.structureEvents.lastOrNull { it.type == StructureEventType.BOS },
                activeFVG = null,
                rsiValue = rsiAnalysis.currentRSI,
                score = 0,
                grade = SetupGrade.IGNORE,
                signal = null,
                setup = null
            )
        }
    }

    /**
     * Scan multiple symbols and return a ranked MarketOverview.
     */
    fun scanAll(
        candleData: Map<String, Map<String, List<Candle>>>
    ): MarketOverview {
        val results = mutableListOf<ScanResult>()

        for ((symbol, timeframes) in candleData) {
            val htfCandles = timeframes["4h"] ?: timeframes["4H"] ?: timeframes["1h"] ?: timeframes["1H"] ?: emptyList()
            val mtfCandles = timeframes["15m"] ?: timeframes["15M"] ?: emptyList()
            val ltfCandles = timeframes["5m"] ?: timeframes["5M"] ?: timeframes["1m"]
            
            if (htfCandles.isNotEmpty() && mtfCandles.isNotEmpty()) {
                val result = scanSymbol(symbol, htfCandles, mtfCandles, ltfCandles)
                results.add(result)
            }
        }

        return MarketOverview.from(results)
    }
}
