package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.domain.engine.Engine
import com.marketintelligence.ai.domain.model.PerformanceReview
import com.marketintelligence.ai.domain.model.PersonalizedLearningPath

interface MentorEngine : Engine {
    suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath
    suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview
    suspend fun explainSignal(signal: AiTradeSignal): String
    suspend fun ask(query: String): String
}
