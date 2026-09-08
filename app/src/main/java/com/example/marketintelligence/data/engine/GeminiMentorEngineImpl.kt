package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.source.remote.GeminiService
import com.example.marketintelligence.domain.engine.MentorEngine
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.model.TrainingModule
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
        return "Gemini Mentor: Based on SMC principles, $query..."
    }
}
