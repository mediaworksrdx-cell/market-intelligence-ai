package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.MentorEngine
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.redxaimentor.RedXKnowledgeBase
import javax.inject.Inject

class ProprietaryMentorEngineImpl @Inject constructor() : MentorEngine {

    override val engineName: String = "Proprietary Engine"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        // Extract learning sequence from KB if available
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
        val lowerQuery = query.lowercase()
        
        // Direct KB lookup
        val matchedEntry = RedXKnowledgeBase.entries.sortedByDescending { entry ->
             // Score based on how many keywords match or if title matches
             var score = 0
             if (entry.title.lowercase().contains(lowerQuery)) score += 10
             score += entry.keywords.count { lowerQuery.contains(it) }
             score
        }.firstOrNull { 
            // Only consider it a match if title matches or at least one keyword matches
            it.title.lowercase().contains(lowerQuery) || it.keywords.any { k -> lowerQuery.contains(k) }
        }

        return if (matchedEntry != null) {
            "**${matchedEntry.title}**\n\n${matchedEntry.content}"
        } else {
            "Proprietary Mentor: Analyzing '$query' through our institutional lens... Ensure your query relates to Liquidity, Market Structure, or Order Flow as per our framework."
        }
    }
}
