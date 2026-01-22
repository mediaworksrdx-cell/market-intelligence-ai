package com.example.redxaiscanner.engine

/**
 * Encapsulates the result of the AI validation layer.
 * It contains the AI's verdict on a given signal, including a refined confidence score.
 *
 * @param isValidated A boolean indicating if the setup passed the AI's evaluation.
 * @param aiConfidenceScore The AI's own calculated confidence score (0-100).
 * @param underlyingSignal The original ScoredSignal that was evaluated.
 */
data class AIValidationResult(
    val isValidated: Boolean,
    val aiConfidenceScore: Int,
    val underlyingSignal: ScoredSignal
)