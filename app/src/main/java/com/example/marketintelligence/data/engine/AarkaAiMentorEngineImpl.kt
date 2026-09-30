package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.source.remote.AarkaApiService
import com.example.marketintelligence.data.source.remote.AarkaPromptRequest
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.engine.MentorEngine
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.util.FinancialDomainFilter
import com.example.marketintelligence.redxaimentor.RedXKnowledgeBase
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AarkaAiMentorEngineImpl @Inject constructor(
    private val aarkaApiService: AarkaApiService,
    private val intelligenceBus: LiveIntelligenceBus
) : MentorEngine {

    override val engineName: String = "Aarka AI"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        return PersonalizedLearningPath(
            suggestedCourseOrder = listOf(
                "Aarka AI Institutional Framework",
                "Company Results & Fundamental Valuation",
                "Smart Money Concepts (SMC) & Liquidity Architecture",
                "Advanced Risk Invalidation Models"
            ),
            welcomeMessage = "Welcome to Aarka AI Cognitive Mentorship. Tailored for analytical market studies and fundamental discovery."
        )
    }

    override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview {
        return PerformanceReview(
            overallFeedback = "Consistent analytical execution aligned with institutional market structure.",
            identifiedWeakness = "Ensure strict invalidation observance during high-volatility expansions.",
            suggestedNextLesson = "Advanced Company Balance Sheet & Earnings Attribution"
        )
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Aarka AI Analysis: Educational study on ${signal.underlyingSymbol} with ${signal.strategyName} framework. Market regime is ${signal.marketRegime}. Institutional structure indicates confidence score of ${signal.confidenceScore}%."
    }

    override suspend fun ask(query: String): String {
        // ── 0. Strict Financial, Technical, and Fundamental Domain Filter ──
        if (!FinancialDomainFilter.isFinancialQuery(query)) {
            return FinancialDomainFilter.REDIRECTION_MESSAGE
        }

        // ── 1. Gather Live Real-Time Financial & Institutional Context ──
        val niftyPrice = intelligenceBus.getLivePrice("NIFTY 50")?.price ?: 22716.20
        val fno = intelligenceBus.fnoIntelligence.value
        val port = intelligenceBus.portfolioIntelligence.value
        val signals = intelligenceBus.scannerSignals.value

        val pnlSign = if (port.totalPnl >= 0) "+" else ""
        val pnlPct = if (port.totalInvestedValue > 0) (port.totalPnl / port.totalInvestedValue) * 100 else 0.0
        val holdingsStr = port.holdings.take(5).joinToString("; ") { h ->
            "${h.symbol} (${h.quantity} units @ ₹${h.avgPrice} → LTP ₹${h.currentPrice}, P&L ₹${h.pnl} [${h.pnlPercent}%])"
        }
        val signalsStr = signals.take(3).joinToString("; ") { s ->
            "${s.symbol} (${s.timeframe} ${s.bias}): Study ₹${s.entryPrice}, Invalidation ₹${s.stopLoss}, Projection ₹${s.target}"
        }

        // ── 2. Construct Dynamic Prompt Payload for Live Aarka AI 7B Neural Engine ──
        val promptPayload = buildString {
            appendLine("[System: You are Market Intelligence AI Mentor powered by Aarka AI 7B. You are an expert quantitative and technical financial analyst.]")
            appendLine("[Mandatory Guidelines:")
            appendLine("1. Exclusively analyze financial markets, stocks, indices, derivatives, company quarterly/annual results, fundamentals, and technical studies.")
            appendLine("2. STRICT EDUCATIONAL TERMINOLOGY: NEVER use 'buy', 'sell', 'entry', 'stop loss', 'stoploss', 'target', or 'take profit'. Use 'Study Zone', 'Invalidation Level', 'Projection Level', 'Accumulate / Distribute', 'Risk-to-Projection Ratio'.")
            appendLine("3. Synthesize your answer dynamically using the live context provided below. Provide direct, institutional reasoning without generic templates.]")
            appendLine()
            appendLine("[Live Real-Time Market & User Context:")
            appendLine("• Major Index: NIFTY 50 @ ₹$niftyPrice")
            appendLine("• F&O Derivatives Intelligence: Spot ₹${fno.spotPrice} | Regime: ${fno.regime} | PCR: ${fno.pcr} | Max Pain: ₹${fno.maxPain} | Call Wall: ₹${fno.callWall} | Put Wall: ₹${fno.putWall} | Bias: ${fno.institutionalBias}")
            if (port.holdings.isNotEmpty()) {
                appendLine("• User Live Portfolio: Invested ₹${String.format(Locale.US, "%,.2f", port.totalInvestedValue)} | Valuation ₹${String.format(Locale.US, "%,.2f", port.totalCurrentValue)} | P&L ₹$pnlSign${String.format(Locale.US, "%,.2f", port.totalPnl)} ($pnlSign${String.format(Locale.US, "%.2f", pnlPct)}%) | Holdings: $holdingsStr")
            }
            if (signals.isNotEmpty()) {
                appendLine("• Active Confluence Scanner Studies: $signalsStr")
            }
            appendLine("]")
            appendLine()
            appendLine("User Question: $query")
        }

        return try {
            val resp = aarkaApiService.promptEngine(AarkaPromptRequest(query = promptPayload))
            if (resp.response.isNotBlank()) {
                val cleanText = sanitizeEducational(resp.response)
                val sourcesTag = if (resp.sources.isNotEmpty()) {
                    "\n\n🧠 *Powered by Aarka AI 7B (${resp.sources.joinToString(", ")})*"
                } else {
                    "\n\n🧠 *Powered by Aarka AI 7B*"
                }
                "$cleanText$sourcesTag"
            } else {
                fallbackLocalKnowledge(query, fno.regime, niftyPrice)
            }
        } catch (e: Exception) {
            // Graceful fallback to local institutional intelligence when offline/timing out
            fallbackLocalKnowledge(query, fno.regime, niftyPrice)
        }
    }

    private fun sanitizeEducational(text: String): String {
        return text
            .replace(Regex("\\b[bB]uy\\b"), "accumulate")
            .replace(Regex("\\b[sS]ell\\b"), "distribute")
            .replace(Regex("\\b[eE]ntry [pP]rice\\b"), "study zone level")
            .replace(Regex("\\b[eE]ntry [zZ]one\\b"), "study zone")
            .replace(Regex("\\b[eE]ntry\\b"), "study zone")
            .replace(Regex("\\b[sS]top ?[lL]oss\\b"), "invalidation level")
            .replace(Regex("\\b[sS][lL]\\b"), "invalidation")
            .replace(Regex("\\b[tT]ake ?[pP]rofit\\b"), "projection level")
            .replace(Regex("\\b[tT][pP]\\b"), "projection")
            .replace(Regex("\\b[rR]:[rR]\\b"), "risk-to-projection ratio")
            .replace(Regex("\\b[rR][rR]\\b"), "risk ratio")
    }

    private fun fallbackLocalKnowledge(query: String, regime: String, niftyPrice: Double): String {
        val lower = query.lowercase(Locale.ROOT)
        val matchedEntry = RedXKnowledgeBase.entries.sortedByDescending { entry ->
            var score = 0
            if (entry.title.lowercase(Locale.ROOT).contains(lower)) score += 10
            score += entry.keywords.count { lower.contains(it) }
            score
        }.firstOrNull {
            it.title.lowercase(Locale.ROOT).contains(lower) || it.keywords.any { k -> lower.contains(k) }
        }

        return if (matchedEntry != null) {
            "**${matchedEntry.title}**\n\n${sanitizeEducational(matchedEntry.content)}\n\n---\n*Aarka AI Live Context: F&O Regime $regime • NIFTY at ₹$niftyPrice*"
        } else {
            "Aarka AI Mentor: Analyzing '$query' through our institutional Smart Money and fundamental framework. Active F&O market regime is $regime with NIFTY at ₹$niftyPrice. Ask about company quarterly results, fundamental valuation metrics, or technical SMC studies for detailed insights."
        }
    }
}
