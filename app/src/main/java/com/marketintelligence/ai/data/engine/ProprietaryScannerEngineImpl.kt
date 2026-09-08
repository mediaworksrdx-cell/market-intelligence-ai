package com.marketintelligence.ai.data.engine

import com.marketintelligence.ai.domain.engine.ScannerEngine
import com.marketintelligence.ai.domain.model.AIAnalysisResult
import com.marketintelligence.ai.domain.model.RiskLevel
import com.marketintelligence.ai.domain.model.SignalType
import com.marketintelligence.redxaiscanner.config.AppConfig
import com.marketintelligence.redxaiscanner.domain.model.Candle
import com.marketintelligence.redxaiscanner.engine.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.random.Random

class ProprietaryScannerEngineImpl @Inject constructor() : ScannerEngine {

    override val engineName: String = "Proprietary Engine (Institutional)"

    // Engines
    private val regimeEngine = RegimeFilterEngine()
    private val smcEngine = SMCEngine()
    private val confidenceEngine = ConfidenceScoringEngine()
    private val appConfig = AppConfig("prod", "", true, 70, true, emptyList(), 2.0)
    private val tradeSetupEngine = TradeSetupEngine(appConfig)

    override suspend fun scanSymbol(symbol: String, timeframe: String): AIAnalysisResult {
        delay(500)

        val candles = generateInstitutionalCandles()

        val regime = regimeEngine.getRegime(candles)
        if (regime == MarketRegime.UNDEFINED || regime == MarketRegime.HIGH_VOLATILITY) {
            return createEmptyResult(symbol, timeframe, "Market regime undefined or too volatile.")
        }

        val smcResult = smcEngine.analyze(candles)
        
        val validOB = smcResult.orderBlocks.lastOrNull { 
            (it.direction == FVGDirection.BULLISH && regime == MarketRegime.BULL_TREND) ||
            (it.direction == FVGDirection.BEARISH && regime == MarketRegime.BEAR_TREND) ||
            (regime == MarketRegime.RANGING)
        }

        if (validOB == null) {
            return createEmptyResult(symbol, timeframe, "No institutional structure aligned with regime.")
        }

        val confluenceSignal = ConfluenceSignal(
            symbol = symbol,
            timeframe = timeframe,
            higherTimeframeBias = smcResult.bias,
            confidenceScore = 0.0,
            smcSignal = validOB,
            patternSignal = null,
            fvgSignal = null,
            versions = mapOf("smc" to "2.0"),
            integrityHash = "hash",
            explanation = SignalExplanation("Confluence", emptyList()),
            liquidityZones = smcResult.liquidityZones,
            regime = regime
        )

        val scoredSignal = confidenceEngine.score(confluenceSignal, candles, regime, smcResult.liquidityZones)
        if (scoredSignal.confidenceScore < 60) {
            return createEmptyResult(symbol, timeframe, "Low confidence score (${scoredSignal.confidenceScore}).")
        }

        val setup = tradeSetupEngine.create(scoredSignal, candles, smcResult.liquidityZones) 
            ?: return createEmptyResult(symbol, timeframe, "Risk/Reward criteria not met.")

        return AIAnalysisResult(
            symbol = symbol,
            signal = if (validOB.direction == FVGDirection.BULLISH) SignalType.BULLISH else SignalType.BEARISH,
            confidence = scoredSignal.confidenceScore,
            alphaScore = (scoredSignal.confidenceScore / 10),
            rrRatio = setup.riskToRewardRatio,
            pattern = "Institutional Order Block",
            patternComplexity = "Institutional",
            timeframe = timeframe,
            entryZone = setup.entryPrice.toPlainString(),
            liquidityZone = setup.takeProfit1.toPlainString(),
            stopLoss = setup.stopLossPrice.toDouble(),
            target = listOf(setup.takeProfit1.toPlainString(), setup.takeProfit2.toPlainString()),
            risk = if (scoredSignal.confidenceScore > 80) RiskLevel.LOW else RiskLevel.MEDIUM,
            rationale = setup.explanation.components.joinToString("\n") { "${it.componentName}: ${it.reasoning}" },
            marketStructure = smcResult.bias.name,
            timestamp = java.text.SimpleDateFormat("HH:mm").format(java.util.Date())
        )
    }

    override fun monitorSymbols(symbols: List<String>): Flow<AIAnalysisResult> = flow {
        while(true) {
            currentCoroutineContext().ensureActive()
            for (symbol in symbols) {
                emit(scanSymbol(symbol, "1H"))
                delay(2000)
            }
            delay(5000)
        }
    }

    private fun createEmptyResult(symbol: String, timeframe: String, reason: String): AIAnalysisResult {
        return AIAnalysisResult(
            symbol = symbol,
            signal = SignalType.NEUTRAL,
            confidence = 0,
            alphaScore = 0,
            rrRatio = "-",
            pattern = null,
            patternComplexity = "-",
            timeframe = timeframe,
            entryZone = null,
            liquidityZone = null,
            stopLoss = null,
            target = null,
            risk = RiskLevel.HIGH,
            rationale = reason,
            marketStructure = "Neutral",
            timestamp = java.text.SimpleDateFormat("HH:mm").format(java.util.Date())
        )
    }
    
    private fun generateInstitutionalCandles(): List<Candle> {
        val candles = mutableListOf<Candle>()
        var price = 100.0
        val now = System.currentTimeMillis()
        
        for (i in 0 until 50) {
            price += Random.nextDouble(0.1, 0.5)
            candles.add(createCandle(now - (100-i)*60000, price, true))
        }
        for (i in 0 until 5) {
            price -= Random.nextDouble(0.2, 0.6)
            candles.add(createCandle(now - (50-i)*60000, price, false))
        }
        val obLow = price - 0.5
        price = obLow
        candles.add(createCandle(now - 45*60000, price, false, isOB = true))
        
        price += 2.0
        candles.add(createCandle(now - 44*60000, price, true, isImpulse = true))
        
        price -= 1.0
        candles.add(createCandle(now - 43*60000, price, false))
        
        return candles
    }
    
    private fun createCandle(time: Long, close: Double, isBullish: Boolean, isOB: Boolean = false, isImpulse: Boolean = false): Candle {
        val open = if (isBullish) close - (if(isImpulse) 1.5 else 0.4) else close + (if(isOB) 0.5 else 0.4)
        val high = maxOf(open, close) + 0.1
        val low = minOf(open, close) - 0.1
        return Candle(
            timestamp = time,
            open = BigDecimal.valueOf(open),
            high = BigDecimal.valueOf(high),
            low = BigDecimal.valueOf(low),
            close = BigDecimal.valueOf(close),
            volume = BigDecimal.valueOf(if(isImpulse) 5000 else 1000)
        )
    }
}
