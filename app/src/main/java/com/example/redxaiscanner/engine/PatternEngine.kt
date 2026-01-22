package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle

data class PatternResult(
    val patternName: String,
    val patternType: PatternType,
    val strengthScore: Double,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val explanation: ExplanationComponent
)

enum class PatternType { REVERSAL, CONTINUATION }

class PatternEngine {
    fun analyze(candles: List<Candle>): List<PatternResult> {
        val results = mutableListOf<PatternResult>()
        // Simple sliding window for pattern detection
        for (i in 0 until candles.size - 3) {
            detectThreeWhiteSoldiers(candles.subList(i, i + 3))?.let { results.add(it) }
        }
        return results
    }

    private fun detectThreeWhiteSoldiers(window: List<Candle>): PatternResult? {
        val first = window[0]
        val second = window[1]
        val third = window[2]
        
        if (!(third.close > second.close && second.close > first.close)) return null
        if (!(first.close > first.open && second.close > second.open && third.close > third.open)) return null

        val strength = 1.0
        
        val explanation = ExplanationComponent(
            componentName = "Chart Pattern",
            reasoning = "A 'Three White Soldiers' bullish reversal pattern was identified.",
            details = mapOf(
                "Pattern Name" to "Three White Soldiers",
                "Strength Score" to "%.2f".format(strength)
            )
        )

        return PatternResult(
            patternName = "Three White Soldiers",
            patternType = PatternType.REVERSAL,
            strengthScore = strength,
            startTimestamp = first.timestamp,
            endTimestamp = third.timestamp,
            explanation = explanation
        )
    }
}
