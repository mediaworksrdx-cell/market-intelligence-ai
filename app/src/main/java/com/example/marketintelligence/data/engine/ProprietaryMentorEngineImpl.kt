package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.model.PerformanceReview
import com.example.marketintelligence.data.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.engine.MentorEngine
import javax.inject.Inject

class ProprietaryMentorEngineImpl @Inject constructor() : MentorEngine {
    override val engineName: String = "Proprietary Engine"

    override suspend fun generateLearningPath(experience: String, risk: String, goals: List<String>): PersonalizedLearningPath {
        throw Exception("Not implemented")
    }

    override suspend fun generatePerformanceReview(tradeHistory: String): PerformanceReview {
        throw Exception("Not implemented")
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Explanation from proprietary engine for ${signal.underlyingSymbol}"
    }
}
