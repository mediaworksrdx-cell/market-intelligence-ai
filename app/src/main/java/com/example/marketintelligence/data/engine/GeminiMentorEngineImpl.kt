package com.example.marketintelligence.data.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.data.source.remote.GeminiService
import com.example.marketintelligence.domain.engine.LiveIntelligenceBus
import com.example.marketintelligence.domain.engine.MentorEngine
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.model.TrainingModule
import java.util.Locale
import javax.inject.Inject

class GeminiMentorEngineImpl @Inject constructor(
    private val geminiService: GeminiService,
    private val intelligenceBus: LiveIntelligenceBus
) : MentorEngine {

    override val engineName: String = "Standard (Gemini)"

    override suspend fun generateLearningPath(userRiskProfile: String, goals: List<String>): PersonalizedLearningPath {
        return geminiService.generateLearningPath(userRiskProfile, goals)
    }

    override suspend fun evaluatePerformance(tradeHistory: List<Any>): PerformanceReview {
        return geminiService.evaluatePerformance(tradeHistory)
    }

    override suspend fun explainSignal(signal: AiTradeSignal): String {
        return "Gemini Analysis: This ${signal.strategyName} setup on ${signal.underlyingSymbol} aligns with the current ${signal.marketRegime} regime. Key institutional levels are being respected with confidence score ${signal.confidenceScore}%."
    }

    override suspend fun ask(query: String): String {
        val apiKey = com.example.marketintelligence.BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            try {
                val fno = intelligenceBus.fnoIntelligence.value
                val contextPrompt = """
                    You are Gemini Market Intelligence AI Mentor for Indian and Global Equities.
                    Live Context: NIFTY at ${intelligenceBus.getLivePrice("NIFTY 50")?.price ?: 22716.20}, F&O Regime: ${fno.regime}, PCR: ${fno.pcr}, Max Pain: ${fno.maxPain}.
                    User Question: $query
                    Provide an in-depth institutional financial response with clear formatting.
                """.trimIndent()
                val model = com.google.ai.client.generativeai.GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )
                val response = model.generateContent(contextPrompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    return text.trim() + "\n\n🧠 *Powered by Gemini 1.5 Flash*"
                }
            } catch (_: Exception) {}
        }
        return FinancialIntelligenceBrain.generateResponse(query, intelligenceBus)
    }
}
