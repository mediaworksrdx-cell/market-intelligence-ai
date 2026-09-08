package com.marketintelligence.ai.data.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.data.source.remote.GeminiService
import com.marketintelligence.ai.domain.engine.MentorEngine
import com.marketintelligence.ai.domain.model.PerformanceReview
import com.marketintelligence.ai.domain.model.PersonalizedLearningPath
import com.marketintelligence.ai.redxaimentor.RedXKnowledgeBase
import javax.inject.Inject

class GeminiMentorEngineImpl @Inject constructor(
    private val geminiService: GeminiService
) : MentorEngine {

    override val engineName: String = "Standard (Gemini)"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        return geminiService.generateLearningPath(userRiskProfile, goals)
    }

    override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview {
        return geminiService.evaluatePerformance(tradeHistory)
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Gemini Analysis: This ${signal.strategyName} setup on ${signal.underlyingSymbol} aligns with the current ${signal.marketRegime} regime. Key institutional levels are being respected."
    }

    override suspend fun ask(query: String): String {
        val lowerQuery = query.lowercase()
        val matchedEntry = RedXKnowledgeBase.entries.sortedByDescending { entry ->
            var score = 0
            if (entry.title.lowercase().contains(lowerQuery)) score += 10
            score += entry.keywords.count { lowerQuery.contains(it) }
            score
        }.firstOrNull { 
            it.title.lowercase().contains(lowerQuery) || it.keywords.any { k -> lowerQuery.contains(k) }
        }

        return if (matchedEntry != null) {
            "**${matchedEntry.title}**\n\n${matchedEntry.content}"
        } else {
            "AI Mentor: Analyzing '$query' through Smart Money Concepts (SMC). Key institutional focus areas: Liquidity Sweeps, Order Blocks, Fair Value Gaps, and Market Structure Breaks."
        }
    }
}
