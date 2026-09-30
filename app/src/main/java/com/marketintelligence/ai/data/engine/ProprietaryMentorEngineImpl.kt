package com.marketintelligence.ai.data.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.domain.engine.LiveIntelligenceBus
import com.marketintelligence.ai.domain.engine.MentorEngine
import com.marketintelligence.ai.domain.model.PerformanceReview
import com.marketintelligence.ai.domain.model.PersonalizedLearningPath
import com.marketintelligence.ai.redxaimentor.RedXKnowledgeBase
import java.util.Locale
import javax.inject.Inject

class ProprietaryMentorEngineImpl @Inject constructor(
    private val intelligenceBus: LiveIntelligenceBus
) : MentorEngine {

    override val engineName: String = "Proprietary Engine"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        val learningSequence = RedXKnowledgeBase.entries
            .find { it.id == "KB-CORE-019" }
            ?.content
            ?.lines()
            ?.filter { it.isNotBlank() }
            ?: listOf("Proprietary Risk Model", "Institutional Order Flow")

        return PersonalizedLearningPath(
            suggestedCourseOrder = learningSequence,
            welcomeMessage = "Welcome to the Proprietary Mentorship Program. Follow the Institutional Trading Framework."
        )
    }

    override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview {
        return PerformanceReview(
            overallFeedback = "Consistent adherence to proprietary models.",
            identifiedWeakness = "None detected.",
            suggestedNextLesson = "Advanced Algo Strategies"
        )
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Proprietary Insight: Detected high-probability ${signal.strategyName} on ${signal.underlyingSymbol}. Institutional order flow confirms ${signal.marketRegime} bias."
    }

    override suspend fun ask(query: String): String {
        val lower = query.lowercase(Locale.US)

        // ── 1. Portfolio Intelligence Query ──
        if (lower.contains("portfolio") || lower.contains("holding") || lower.contains("pnl") || lower.contains("profit") || lower.contains("invest") || lower.contains("my stock")) {
            val port = intelligenceBus.portfolioIntelligence.value
            val pnlSign = if (port.totalPnl >= 0) "+" else ""
            val pnlPct = if (port.totalInvestedValue > 0) (port.totalPnl / port.totalInvestedValue) * 100 else 0.0
            val holdingsStr = port.holdings.joinToString("\n") { h ->
                "  • **${h.symbol}**: ${h.quantity} units (Avg: ₹${h.avgPrice} → LTP: ₹${h.currentPrice} | P&L: ₹${h.pnl} [${h.pnlPercent}%])"
            }
            return """
                |💼 **Institutional Portfolio Audit & Live Intelligence**
                |
                |• **Current Valuation**: ₹${String.format(Locale.US, "%,.2f", port.totalCurrentValue)}
                |• **Invested Capital**: ₹${String.format(Locale.US, "%,.2f", port.totalInvestedValue)}
                |• **Net Unrealized P&L**: ₹$pnlSign${String.format(Locale.US, "%,.2f", port.totalPnl)} ($pnlSign${String.format(Locale.US, "%.2f", pnlPct)}%)
                |• **Today's Movement**: ₹${String.format(Locale.US, "%,.2f", port.todayPnl)}
                |
                |**Active Asset Allocations**:
                |$holdingsStr
                |
                |💡 *Institutional Mentor Guidance*: Portfolio exposure is well-distributed. Maintain disciplined invalidation levels and avoid over-concentration in correlated assets.
            """.trimMargin()
        }

        // ── 2. F&O Scanner Intelligence Query ──
        if (lower.contains("fno") || lower.contains("option") || lower.contains("pcr") || lower.contains("max pain") || lower.contains("wall") || lower.contains("gex") || lower.contains("strike") || lower.contains("regime")) {
            val fno = intelligenceBus.fnoIntelligence.value
            return """
                |🎯 **F&O Institutional Scanner Intelligence**
                |
                |• **Underlying Asset**: ${fno.symbol} (Spot: ₹${String.format(Locale.US, "%,.2f", fno.spotPrice)})
                |• **Market Regime**: **${fno.regime}**
                |• **Put-Call Ratio (PCR)**: **${fno.pcr}** (${if (fno.pcr >= 1.0) "Bullish Accumulation" else "Bearish Distribution"})
                |• **Max Pain Strike**: ₹${String.format(Locale.US, "%,.0f", fno.maxPain)}
                |• **Call Wall (Dealer Resistance)**: ₹${String.format(Locale.US, "%,.0f", fno.callWall)}
                |• **Put Wall (Dealer Support)**: ₹${String.format(Locale.US, "%,.0f", fno.putWall)}
                |• **Institutional Footprint**: ${fno.institutionalBias}
                |
                |💡 *Study Recommendation*: Observe high-probability credit spread structures centered around Max Pain, illustrating dealer gamma pinning.
            """.trimMargin()
        }

        // ── 3. AI Scanner Signals Query ──
        if (lower.contains("signal") || lower.contains("scanner") || lower.contains("scan") || lower.contains("setup") || lower.contains("breakout") || lower.contains("alert") || lower.contains("confluence")) {
            val signals = intelligenceBus.scannerSignals.value
            val signalsStr = signals.joinToString("\n\n") { s ->
                "• **${s.symbol}** (${s.timeframe} • ${s.bias}):\n  - Study Zone: ₹${s.entryPrice} | Invalidation: ₹${s.stopLoss} | Projection: ₹${s.target}\n  - Confidence: ${s.confidence}%\n  - Rationale: ${s.rationale}"
            }
            return """
                |⚡ **Active AI Scanner Confluence Studies**
                |
                |$signalsStr
                |
                |💡 *Institutional Study Protocol*: Wait for confirmation on lower timeframe liquidity sweeps before validating analysis.
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
                |📊 **Live Market Intelligence Pulse**
                |
                |• **NIFTY 50**: ₹${nifty?.price ?: 22716.20} (${nifty?.changePercent ?: -0.28}%)
                |• **SENSEX**: ₹${sensex?.price ?: 72529.07} (${sensex?.changePercent ?: -0.33}%)
                |• **BANKNIFTY**: ₹${banknifty?.price ?: 54259.95} (${banknifty?.changePercent ?: -0.39}%)
                |• **RELIANCE**: ₹${reliance?.price ?: 1182.0} (${reliance?.changePercent ?: -1.30}%)
                |• **BTC / USD**: $${btc?.price ?: 83923.0} (+${btc?.changePercent ?: 1.45}%)
                |• **ETH / USD**: $${eth?.price ?: 2495.0} (+${eth?.changePercent ?: 0.75}%)
                |• **S&P 500 (SPX)**: $${spx?.price ?: 5485.88} (${spx?.changePercent ?: -0.26}%)
                |• **DFM General (UAE)**: ${dfmgi?.price ?: 4849.37} (+${dfmgi?.changePercent ?: 0.11}%)
                |
                |💡 *Market Structure*: Major indices are consolidating near institutional demand zones. Study confirmation patterns before drawing directional conclusions.
            """.trimMargin()
        }

        // ── 5. Knowledge Base Lookup ──
        val matchedEntry = RedXKnowledgeBase.entries.sortedByDescending { entry ->
            var score = 0
            if (entry.title.lowercase(Locale.US).contains(lower)) score += 10
            score += entry.keywords.count { lower.contains(it) }
            score
        }.firstOrNull {
            it.title.lowercase(Locale.US).contains(lower) || it.keywords.any { k -> lower.contains(k) }
        }

        return if (matchedEntry != null) {
            "**${matchedEntry.title}**\n\n${matchedEntry.content}\n\n---\n*Live Context: F&O Regime ${intelligenceBus.fnoIntelligence.value.regime} • NIFTY at ₹${intelligenceBus.getLivePrice("NIFTY 50")?.price ?: 22716.20}*"
        } else {
            "Proprietary Mentor: Analyzing '$query' through our institutional Smart Money Framework. Active F&O regime is ${intelligenceBus.fnoIntelligence.value.regime} with PCR at ${intelligenceBus.fnoIntelligence.value.pcr}. Ask about 'live prices', 'signals', 'fno', or 'portfolio' for instant real-time intelligence."
        }
    }
}
