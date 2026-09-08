package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.engine.Engine
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath

interface MentorEngine : Engine {
    suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath
    suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview
    suspend fun explainSignal(signal: AiTradeSignal): String
    suspend fun ask(query: String): String
}
