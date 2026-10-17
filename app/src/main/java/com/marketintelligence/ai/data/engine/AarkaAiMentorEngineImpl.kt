package com.marketintelligence.ai.data.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.data.source.remote.AarkaApiService
import com.marketintelligence.ai.data.source.remote.AarkaPromptRequest
import com.marketintelligence.ai.domain.engine.LiveIntelligenceBus
import com.marketintelligence.ai.domain.engine.MentorEngine
import com.marketintelligence.ai.domain.model.PerformanceReview
import com.marketintelligence.ai.domain.model.PersonalizedLearningPath
import com.marketintelligence.ai.domain.util.FinancialDomainFilter
import com.marketintelligence.ai.redxaimentor.RedXKnowledgeBase
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
        val query = "Provide an institutional market study on ${signal.underlyingSymbol} with ${signal.strategyName} in ${signal.marketRegime} market regime. Strike levels: ${signal.optionLegs.joinToString { "${it.strike} ${it.type}" }}. Confidence: ${signal.confidenceScore}%."
        return try {
            val resp = aarkaApiService.promptEngine(AarkaPromptRequest(query = query))
            if (resp.response.isNotBlank() && resp.response.length > 30) {
                sanitizeEducational(resp.response) + "\n\n🧠 *Powered by Aarka AI 7B*"
            } else {
                "Aarka AI Analysis: Educational study on ${signal.underlyingSymbol} with ${signal.strategyName} framework. Market regime is ${signal.marketRegime}. Institutional structure indicates confidence score of ${signal.confidenceScore}%."
            }
        } catch (_: Exception) {
            "Aarka AI Analysis: Educational study on ${signal.underlyingSymbol} with ${signal.strategyName} framework. Market regime is ${signal.marketRegime}. Institutional structure indicates confidence score of ${signal.confidenceScore}%."
        }
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
            if (resp.response.isNotBlank() && !resp.response.contains("Analyzing") && resp.response.length > 50) {
                val cleanText = sanitizeEducational(resp.response)
                val sourcesTag = if (resp.sources.isNotEmpty()) {
                    "\n\n🧠 *Powered by Aarka AI 7B (${resp.sources.joinToString(", ")})*"
                } else {
                    "\n\n🧠 *Powered by Aarka AI 7B*"
                }
                "$cleanText$sourcesTag"
            } else {
                // Direct retry to Aarka AI without verbose wrapper
                val directResp = aarkaApiService.promptEngine(AarkaPromptRequest(query = query))
                if (directResp.response.isNotBlank()) {
                    sanitizeEducational(directResp.response) + "\n\n🧠 *Powered by Aarka AI 7B*"
                } else {
                    "Aarka AI 7B is synthesizing institutional market data for '$query'. Please resubmit with specific timeframe or ticker context."
                }
            }
        } catch (e: Exception) {
            try {
                val directResp = aarkaApiService.promptEngine(AarkaPromptRequest(query = query))
                if (directResp.response.isNotBlank()) {
                    sanitizeEducational(directResp.response) + "\n\n🧠 *Powered by Aarka AI 7B*"
                } else {
                    "Aarka AI was unable to complete the analysis at this moment. Please verify server connectivity and retry."
                }
            } catch (retryEx: Exception) {
                "Aarka AI connection issue: ${retryEx.localizedMessage ?: "Request timed out"}. Please check server connectivity at ${com.marketintelligence.ai.BuildConfig.BASE_URL}."
            }
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
}

