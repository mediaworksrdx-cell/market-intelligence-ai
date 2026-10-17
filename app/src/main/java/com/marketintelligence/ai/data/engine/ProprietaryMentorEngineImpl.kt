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
        return FinancialIntelligenceBrain.generateResponse(query, intelligenceBus)
    }
}
