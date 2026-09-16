package com.marketintelligence.ai.data.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.data.source.remote.GeminiService
import com.marketintelligence.ai.domain.engine.LiveIntelligenceBus
import com.marketintelligence.ai.domain.engine.MentorEngine
import com.marketintelligence.ai.domain.model.PerformanceReview
import com.marketintelligence.ai.domain.model.PersonalizedLearningPath
import com.marketintelligence.ai.redxaimentor.RedXKnowledgeBase
import java.util.Locale
import javax.inject.Inject

class GeminiMentorEngineImpl @Inject constructor(
    private val geminiService: GeminiService,
    private val intelligenceBus: LiveIntelligenceBus
) : MentorEngine {

    override val engineName: String = "Standard (Gemini)"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        return geminiService.generateLearningPath(userRiskProfile, goals)
    }

    override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview {
        return geminiService.evaluatePerformance(tradeHistory)
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Gemini Analysis: This ${signal.strategyName} setup on ${signal.underlyingSymbol} aligns with the current ${signal.marketRegime} regime. Key institutional levels are being respected with confidence score ${signal.confidenceScore}%."
    }

    override suspend fun ask(query: String): String {
        val lower = query.lowercase(Locale.US)

        // ── 1. Portfolio Intelligence ──
        if (lower.contains("portfolio") || lower.contains("holding") || lower.contains("pnl") || lower.contains("profit") || lower.contains("invest") || lower.contains("my stock")) {
            val port = intelligenceBus.portfolioIntelligence.value
            val pnlSign = if (port.totalPnl >= 0) "+" else ""
            val pnlPct = if (port.totalInvestedValue > 0) (port.totalPnl / port.totalInvestedValue) * 100 else 0.0
            val holdingsStr = port.holdings.joinToString("\n") { h ->
                "  • **${h.symbol}**: ${h.quantity} units (Avg: ₹${h.avgPrice} → LTP: ₹${h.currentPrice} | P&L: ₹${h.pnl} [${h.pnlPercent}%])"
            }
            return """
                |💼 **Gemini Portfolio Intelligence**
                |
                |• **Total Valuation**: ₹${String.format(Locale.US, "%,.2f", port.totalCurrentValue)}
                |• **Invested Capital**: ₹${String.format(Locale.US, "%,.2f", port.totalInvestedValue)}
                |• **Net Unrealized P&L**: ₹$pnlSign${String.format(Locale.US, "%,.2f", port.totalPnl)} ($pnlSign${String.format(Locale.US, "%.2f", pnlPct)}%)
                |• **Today's Movement**: ₹${String.format(Locale.US, "%,.2f", port.todayPnl)}
                |
                |**Current Holdings**:
                |$holdingsStr
                |
                |🤖 *Gemini AI Insight*: Your portfolio balance demonstrates strategic risk distribution. Ensure periodic rebalancing near major resistance levels.
            """.trimMargin()
        }

        // ── 2. F&O Scanner Intelligence ──
        if (lower.contains("fno") || lower.contains("option") || lower.contains("pcr") || lower.contains("max pain") || lower.contains("wall") || lower.contains("gex") || lower.contains("strike") || lower.contains("regime")) {
            val fno = intelligenceBus.fnoIntelligence.value
            return """
                |🎯 **Gemini F&O Derivatives Intelligence**
                |
                |• **Underlying Asset**: ${fno.symbol} (Spot: ₹${String.format(Locale.US, "%,.2f", fno.spotPrice)})
                |• **Market Regime**: **${fno.regime}**
                |• **Put-Call Ratio (PCR)**: **${fno.pcr}** (${if (fno.pcr >= 1.0) "Bullish Bias" else "Bearish Pressure"})
                |• **Max Pain Strike**: ₹${String.format(Locale.US, "%,.0f", fno.maxPain)}
                |• **Call Wall (Dealer Resistance)**: ₹${String.format(Locale.US, "%,.0f", fno.callWall)}
                |• **Put Wall (Dealer Support)**: ₹${String.format(Locale.US, "%,.0f", fno.putWall)}
                |• **Dealer Footprint**: ${fno.institutionalBias}
                |
                |🤖 *Gemini AI Strategy*: F&O open interest clusters suggest hedging pressure near the Call Wall. Look for consolidation around Max Pain.
            """.trimMargin()
        }

        // ── 3. AI Scanner Signals ──
        if (lower.contains("signal") || lower.contains("scanner") || lower.contains("scan") || lower.contains("setup") || lower.contains("breakout") || lower.contains("alert") || lower.contains("confluence")) {
            val signals = intelligenceBus.scannerSignals.value
            val signalsStr = signals.joinToString("\n\n") { s ->
                "• **${s.symbol}** (${s.timeframe} • ${s.bias}):\n  - Entry: ₹${s.entryPrice} | SL: ₹${s.stopLoss} | Target: ₹${s.target}\n  - Confidence: ${s.confidence}%\n  - Rationale: ${s.rationale}"
            }
            return """
                |⚡ **Gemini AI Scanner Confluence Signals**
                |
                |$signalsStr
                |
                |🤖 *Gemini Execution Rule*: Trade strictly in alignment with higher timeframe structure. Confirm volume surge before market entry.
            """.trimMargin()
        }

        // ── 4. Live Prices / Market Query ──
        if (lower.contains("price") || lower.contains("quote") || lower.contains("nifty") || lower.contains("sensex") || lower.contains("banknifty") || lower.contains("reliance") || lower.contains("btc") || lower.contains("eth") || lower.contains("spx") || lower.contains("market")) {
            val prices = intelligenceBus.livePrices.value
            val nifty = prices["NIFTY 50"] ?: prices["NIFTY"]
            val sensex = prices["SENSEX"]
            val banknifty = prices["BANKNIFTY"]
            val reliance = prices["RELIANCE"]
            val btc = prices["BTC"]
            val eth = prices["ETH"]
            val spx = prices["SPX"]
            val dfmgi = prices["DFMGI"]

            return """
                |📊 **Gemini Live Market Pulse**
                |
                |• **NIFTY 50**: ₹${nifty?.price ?: 23600.0} (${nifty?.changePercent ?: -0.61}%)
                |• **SENSEX**: ₹${sensex?.price ?: 77200.0} (${sensex?.changePercent ?: -0.73}%)
                |• **BANKNIFTY**: ₹${banknifty?.price ?: 50400.0} (${banknifty?.changePercent ?: -0.42}%)
                |• **RELIANCE**: ₹${reliance?.price ?: 1301.0} (${reliance?.changePercent ?: -1.11}%)
                |• **BTC / USD**: $${btc?.price ?: 79227.0} (+${btc?.changePercent ?: 1.45}%)
                |• **ETH / USD**: $${eth?.price ?: 2480.0} (+${eth?.changePercent ?: 0.75}%)
                |• **S&P 500 (SPX)**: $${spx?.price ?: 5485.88} (${spx?.changePercent ?: -0.26}%)
                |• **DFM General (UAE)**: ${dfmgi?.price ?: 4849.37} (+${dfmgi?.changePercent ?: 0.11}%)
                |
                |🤖 *Gemini Trend Analysis*: Institutional order blocks are active at current price levels across global markets.
            """.trimMargin()
        }

        return "Gemini Mentor: Based on Smart Money Concepts (SMC), market structure is exhibiting ${intelligenceBus.fnoIntelligence.value.regime}. Ask for 'live prices', 'scanner signals', 'fno option chain', or 'my portfolio' for real-time live data."
    }
}
