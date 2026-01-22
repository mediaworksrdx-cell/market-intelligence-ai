package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.model.PersonalizedLearningPath
import com.example.marketintelligence.data.model.PerformanceReview
import com.example.marketintelligence.domain.engine.MentorEngine
import com.example.marketintelligence.data.source.remote.GeminiService
import javax.inject.Inject

class GeminiMentorEngineImpl @Inject constructor(
    private val geminiService: GeminiService
) : MentorEngine {
    override val engineName: String = "Standard (Gemini)"

    override suspend fun generateLearningPath(experience: String, risk: String, goals: List<String>): PersonalizedLearningPath { 
        return geminiService.generateLearningPath(experience, risk, goals) 
    }
    
    override suspend fun generatePerformanceReview(tradeHistory: String): PerformanceReview { 
        return geminiService.generatePerformanceReview(tradeHistory) 
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "This ${signal.strategyName} is recommended based on the current market regime: ${signal.marketRegime}."
    }
}
