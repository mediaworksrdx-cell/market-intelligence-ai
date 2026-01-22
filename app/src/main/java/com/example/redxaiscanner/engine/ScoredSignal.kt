package com.example.redxaiscanner.engine

/**
 * Represents the final, scored output of the entire analysis pipeline.
 * This signal has been validated for confluence and quantitatively scored for confidence.
 *
 * @param confidenceScore The final normalized score (0-100) for the trading setup.
 * @param scoreBreakdown A map detailing how the final score was calculated.
 * @param underlyingSignal The original ConfluenceSignal that was scored.
 */
data class ScoredSignal(
    val confidenceScore: Int,
    val scoreBreakdown: Map<String, Int>,
    val underlyingSignal: ConfluenceSignal
)