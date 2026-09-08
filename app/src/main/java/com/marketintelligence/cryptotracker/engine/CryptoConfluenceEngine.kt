package com.marketintelligence.cryptotracker.engine

import com.marketintelligence.tradeengine.models.Candle

class CryptoConfluenceEngine(
    val smcEngine: CryptoSMCEngine,
    val fvgEngine: CryptoFVGEngine,
    val rsiEngine: CryptoRSIEngine,
    val riskEngine: CryptoRiskEngine
) {
    /**
     * Analyze a single symbol across multiple timeframes.
     * @param symbol Trading pair (e.g., "BTCUSDT")
     * @param htfCandles Higher timeframe candles (4H or 1H) for bias
     * @param mtfCandles Medium timeframe candles (15m) for setup identification
     * @param ltfCandles Lower timeframe candles (5m or 1m) for entry confirmation (optional)
     * @return CryptoSetup if a valid setup is found, null otherwise
     */
    fun analyze(
        symbol: String,
        htfCandles: List<Candle>,
        mtfCandles: List<Candle>,
        ltfCandles: List<Candle>? = null
    ): CryptoSetup? {
        if (htfCandles.isEmpty() || mtfCandles.isEmpty()) return null

        val timeframe = mtfCandles[0].timeframe

        // 1. HTF Analysis (4H/1H)
        val htfAnalysis = smcEngine.analyze(htfCandles)
        val htfBias = htfAnalysis.structureState

        // 2. MTF Analysis (15m)
        val mtfAnalysis = smcEngine.analyze(mtfCandles)
        val rsiAnalysis = rsiEngine.analyze(mtfCandles, mtfAnalysis.structureState, mtfAnalysis.swingPoints)
        val fvgAnalysis = fvgEngine.analyze(mtfCandles, mtfAnalysis, htfBias, rsiAnalysis.currentRSI)
        
        val currentPrice = mtfCandles.last().close

        val longSetup = evaluateLongSetup(symbol, timeframe, htfBias, mtfAnalysis, fvgAnalysis, rsiAnalysis, currentPrice, mtfCandles.last().openTime)
        val shortSetup = evaluateShortSetup(symbol, timeframe, htfBias, mtfAnalysis, fvgAnalysis, rsiAnalysis, currentPrice, mtfCandles.last().openTime)

        val bestSetup = listOfNotNull(longSetup, shortSetup).maxByOrNull { it.score.total }
        
        return if ((bestSetup?.score?.total ?: 0) >= 30) bestSetup else null
    }

    private fun evaluateLongSetup(
        symbol: String,
        timeframe: String,
        htfBias: StructureState,
        mtfAnalysis: CryptoSMCAnalysis,
        enrichedFvgs: List<EnrichedFVG>,
        rsiAnalysis: CryptoRSIAnalysis,
        currentPrice: Double,
        currentTime: Long
    ): CryptoSetup? {
        var smcScore = 0
        var liquidityScore = 0
        var fvgScore = 0
        var rsiScore = 0
        var htfScore = 0
        var riskScore = 0

        // SMC Structure (Max 25)
        val latestBullishEvent = mtfAnalysis.structureEvents.lastOrNull { it.direction == StructureDirection.BULLISH }
        if (latestBullishEvent != null) {
            smcScore = if (latestBullishEvent.type == StructureEventType.BOS) 25 else 15
        } else if (mtfAnalysis.structureState == StructureState.BULLISH) {
            smcScore = 5
        }

        // Liquidity (Max 20)
        val hasSSL = mtfAnalysis.liquidityLevels.any { it.side == LiquiditySide.SELL }
        val sslSwept = mtfAnalysis.sweeps.any { it.level.side == LiquiditySide.SELL }
        val bullishDisplacement = mtfAnalysis.displacements.any { it.direction == StructureDirection.BULLISH }

        if (sslSwept && bullishDisplacement) {
            liquidityScore = 20
        } else if (sslSwept) {
            liquidityScore = 15
        } else if (hasSSL) {
            liquidityScore = 5
        }

        // FVG (Max 20)
        val activeBullishFVGs = enrichedFvgs.filter { it.direction == FVGDirection.BULLISH && (it.status == FVGStatus.ACTIVE || it.status == FVGStatus.NEW) }
        val bestFVG = activeBullishFVGs.maxByOrNull { it.qualityScore.total }
        if (bestFVG != null) {
            fvgScore = (bestFVG.qualityScore.total / 5.0).toInt().coerceIn(0, 20)
        }

        // RSI (Max 15)
        if (rsiAnalysis.currentRSI > 50 && rsiAnalysis.momentumDirection == MomentumDirection.BULLISH) {
            val hasBullishDiv = rsiAnalysis.divergences.any { it.type == DivergenceType.BULLISH }
            rsiScore = if (hasBullishDiv) 15 else 10
        } else if (rsiAnalysis.currentRSI in 40.0..60.0) {
            rsiScore = 5
        }

        // HTF Alignment (Max 10)
        if (htfBias == StructureState.BULLISH) {
            htfScore = 10
        } else if (htfBias == StructureState.RANGING) {
            htfScore = 5
        }

        // Calculate Risk
        val riskCalc = riskEngine.calculate(SetupDirection.LONG, bestFVG, mtfAnalysis, currentPrice)
        if (riskCalc.riskRewardRatio >= 3.0) {
            riskScore = 10
        } else if (riskCalc.riskRewardRatio >= 2.0) {
            riskScore = 7
        } else if (riskCalc.riskRewardRatio >= 1.5) {
            riskScore = 4
        }

        val breakdown = ScoreBreakdown.fromFactors(
            smcStructure = smcScore,
            liquidity = liquidityScore,
            fvg = fvgScore,
            rsi = rsiScore,
            htfAlignment = htfScore,
            riskReward = riskScore
        )
        
        val grade = breakdown.toGrade()

        return CryptoSetup(
            symbol = symbol,
            timeframe = timeframe,
            direction = SetupDirection.LONG,
            htfBias = htfBias,
            smcAnalysis = mtfAnalysis,
            fvg = bestFVG,
            rsiAnalysis = rsiAnalysis,
            score = breakdown,
            grade = grade,
            entry = riskCalc.entry,
            stopLoss = riskCalc.stopLoss,
            tp1 = riskCalc.tp1,
            tp2 = riskCalc.tp2,
            tp3 = riskCalc.tp3,
            riskRewardRatio = riskCalc.riskRewardRatio,
            riskPercent = riskCalc.riskPercent,
            status = SetupStatus.ACTIVE,
            createdAt = currentTime,
            invalidatedAt = null
        )
    }

    private fun evaluateShortSetup(
        symbol: String,
        timeframe: String,
        htfBias: StructureState,
        mtfAnalysis: CryptoSMCAnalysis,
        enrichedFvgs: List<EnrichedFVG>,
        rsiAnalysis: CryptoRSIAnalysis,
        currentPrice: Double,
        currentTime: Long
    ): CryptoSetup? {
        var smcScore = 0
        var liquidityScore = 0
        var fvgScore = 0
        var rsiScore = 0
        var htfScore = 0
        var riskScore = 0

        // SMC Structure (Max 25)
        val latestBearishEvent = mtfAnalysis.structureEvents.lastOrNull { it.direction == StructureDirection.BEARISH }
        if (latestBearishEvent != null) {
            smcScore = if (latestBearishEvent.type == StructureEventType.BOS) 25 else 15
        } else if (mtfAnalysis.structureState == StructureState.BEARISH) {
            smcScore = 5
        }

        // Liquidity (Max 20)
        val hasBSL = mtfAnalysis.liquidityLevels.any { it.side == LiquiditySide.BUY }
        val bslSwept = mtfAnalysis.sweeps.any { it.level.side == LiquiditySide.BUY }
        val bearishDisplacement = mtfAnalysis.displacements.any { it.direction == StructureDirection.BEARISH }

        if (bslSwept && bearishDisplacement) {
            liquidityScore = 20
        } else if (bslSwept) {
            liquidityScore = 15
        } else if (hasBSL) {
            liquidityScore = 5
        }

        // FVG (Max 20)
        val activeBearishFVGs = enrichedFvgs.filter { it.direction == FVGDirection.BEARISH && (it.status == FVGStatus.ACTIVE || it.status == FVGStatus.NEW) }
        val bestFVG = activeBearishFVGs.maxByOrNull { it.qualityScore.total }
        if (bestFVG != null) {
            fvgScore = (bestFVG.qualityScore.total / 5.0).toInt().coerceIn(0, 20)
        }

        // RSI (Max 15)
        if (rsiAnalysis.currentRSI < 50 && rsiAnalysis.momentumDirection == MomentumDirection.BEARISH) {
            val hasBearishDiv = rsiAnalysis.divergences.any { it.type == DivergenceType.BEARISH }
            rsiScore = if (hasBearishDiv) 15 else 10
        } else if (rsiAnalysis.currentRSI in 40.0..60.0) {
            rsiScore = 5
        }

        // HTF Alignment (Max 10)
        if (htfBias == StructureState.BEARISH) {
            htfScore = 10
        } else if (htfBias == StructureState.RANGING) {
            htfScore = 5
        }

        // Calculate Risk
        val riskCalc = riskEngine.calculate(SetupDirection.SHORT, bestFVG, mtfAnalysis, currentPrice)
        if (riskCalc.riskRewardRatio >= 3.0) {
            riskScore = 10
        } else if (riskCalc.riskRewardRatio >= 2.0) {
            riskScore = 7
        } else if (riskCalc.riskRewardRatio >= 1.5) {
            riskScore = 4
        }

        val breakdown = ScoreBreakdown.fromFactors(
            smcStructure = smcScore,
            liquidity = liquidityScore,
            fvg = fvgScore,
            rsi = rsiScore,
            htfAlignment = htfScore,
            riskReward = riskScore
        )
        
        val grade = breakdown.toGrade()

        return CryptoSetup(
            symbol = symbol,
            timeframe = timeframe,
            direction = SetupDirection.SHORT,
            htfBias = htfBias,
            smcAnalysis = mtfAnalysis,
            fvg = bestFVG,
            rsiAnalysis = rsiAnalysis,
            score = breakdown,
            grade = grade,
            entry = riskCalc.entry,
            stopLoss = riskCalc.stopLoss,
            tp1 = riskCalc.tp1,
            tp2 = riskCalc.tp2,
            tp3 = riskCalc.tp3,
            riskRewardRatio = riskCalc.riskRewardRatio,
            riskPercent = riskCalc.riskPercent,
            status = SetupStatus.ACTIVE,
            createdAt = currentTime,
            invalidatedAt = null
        )
    }
}
