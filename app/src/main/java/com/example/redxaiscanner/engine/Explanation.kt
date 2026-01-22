package com.example.redxaiscanner.engine

/**
 * A container for a structured, human-readable explanation of why a signal was generated.
 * Each signal component provides its own "reasoning" object.
 */
data class SignalExplanation(
    val title: String,
    val components: List<ExplanationComponent>
)

/**
 * A generic component of an explanation, providing a title and a set of key-value details.
 * This structure is versatile enough to describe any engine's logic.
 */
data class ExplanationComponent(
    val componentName: String, // e.g., "Market Structure (SMC)"
    val reasoning: String,     // A high-level summary, e.g., "Bullish trend confirmed by Break of Structure."
    val details: Map<String, String>
)