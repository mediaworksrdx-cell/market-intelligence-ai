package com.example.marketintelligence.data.source.remote

import com.example.marketintelligence.data.util.MarketPriceCatalog
import com.example.marketintelligence.domain.model.AIAnalysisResult
import com.example.marketintelligence.domain.model.PerformanceReview
import com.example.marketintelligence.domain.model.PersonalizedLearningPath
import com.example.marketintelligence.domain.model.RiskLevel
import com.example.marketintelligence.domain.model.SignalType
import com.example.marketintelligence.domain.repository.MarketRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GeminiService @Inject constructor(
    private val marketRepository: MarketRepository
) {

    suspend fun analyzeStock(symbol: String, timeframe: String): AIAnalysisResult {
        // Simulated API Call
        delay(400)
        val basePrice = MarketPriceCatalog.resolveBasePrice(symbol, marketRepository)
        val entryLow = basePrice * 0.998
        val entryHigh = basePrice * 1.002
        val sl = basePrice * 0.992
        val tp1 = basePrice * 1.016
        val tp2 = basePrice * 1.032

        val dec = if (basePrice < 1.0) "%.6f" else "%.2f"
        val entryZoneStr = "${String.format(Locale.US, dec, entryLow)} - ${String.format(Locale.US, dec, entryHigh)}"
        val liquidityZoneStr = String.format(Locale.US, dec, tp1)
        val stopLossVal = String.format(Locale.US, dec, sl).toDoubleOrNull() ?: sl
        val targetList = listOf(
            String.format(Locale.US, dec, tp1),
            String.format(Locale.US, dec, tp2)
        )

        return AIAnalysisResult(
            symbol = symbol,
            signal = SignalType.BULLISH,
            confidence = 88,
            alphaScore = 9,
            rrRatio = "1:2.8",
            pattern = "Bull Flag Breakout",
            patternComplexity = "Intermediate",
            timeframe = timeframe,
            entryZone = entryZoneStr,
            liquidityZone = liquidityZoneStr,
            stopLoss = stopLossVal,
            target = targetList,
            risk = RiskLevel.MEDIUM,
            rationale = "Gemini AI detects strong momentum and institutional volume inflow on $timeframe.",
            marketStructure = "Uptrend (Bullish Flow)",
            timestamp = java.text.SimpleDateFormat("HH:mm", Locale.US).format(Date())
        )
    }

    suspend fun generateLearningPath(profile: String, goals: List<String>): PersonalizedLearningPath {
        delay(500)
        return PersonalizedLearningPath(
            suggestedCourseOrder = listOf("Intro to AI Trading", "Risk Management 101"),
            welcomeMessage = "Here is your Gemini-curated learning path based on your $profile profile."
        )
    }

    suspend fun evaluatePerformance(trades: List<Any>): PerformanceReview {
        delay(500)
        return PerformanceReview(
            overallFeedback = "Good discipline shown in recent trades.",
            identifiedWeakness = "Tendency to exit early.",
            suggestedNextLesson = "Psychology of Holding Winners"
        )
    }
}

